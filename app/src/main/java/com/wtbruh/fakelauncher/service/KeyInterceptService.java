package com.wtbruh.fakelauncher.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import com.wtbruh.fakelauncher.ShutdownActivity;
import com.wtbruh.fakelauncher.SplashActivity;
import com.wtbruh.fakelauncher.constants.SettingsConstants;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;

import java.io.DataInputStream;
import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * 按键拦截服务（0.6.1）：
 * 以特权身份（按设置里的授权方式：Root 或 Shizuku）启动 native keyintercept
 * （EVIOCGRAB 独占 /dev/input），物理按键事件经 TCP 转发到本服务：
 * - 电源键短按 -> 发 ROM 广播（android.powerkey.short-press）让 MainActivity 锁屏息屏
 * - 电源键长按 -> 弹出功能机风格关机/重启界面（系统菜单不会出现）
 * - 其他按键 -> 重新注入回系统（input keyevent），伪装界面正常处理
 */
public class KeyInterceptService extends Service {
    private static final String TAG = "KeyInterceptService";
    private static final int PORT = 45678;
    private static final int KEYCODE_POWER = 116;
    private static final long LONG_PRESS_MS = 800;

    private static final String ACTION_POWERKEY_SHORT = "android.powerkey.short-press";
    /** keyintercept 打包进 lib/<abi>/ 的名字（本体是可执行文件，只是套了 .so 后缀） */
    private static final String BIN_LIB_NAME = "libkeyintercept.so";
    private static final String BIN_PATH = "/data/local/tmp/keyintercept";

    private volatile boolean running = false;
    private Thread serverThread;
    private ServerSocket serverSocket;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService exec = Executors.newSingleThreadExecutor();

    private volatile long powerDownAt = 0;
    private volatile boolean powerLongFired = false;
    private Runnable powerLongRunnable;

    // 快捷键（音量键等）状态
    private long lastHotkeyDown = 0;
    private int hotkeyPressCount = 0;
    private Runnable hotkeyLongRunnable;
    private long hotkeyFiredAt = 0; // 快捷键触发启动的时间戳（用于吞掉启动残留的音量键 up）
    private boolean hotkeyStarted = false; // 本次按压是否触发了启动（触发的那次 up 一律忽略，参考历史可行方案）
    private long lastVolumeDownAt = 0; // 最近一次音量键 down 时刻（用于按按压时长判断启动残留）

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!running) {
            running = true;
            // 前台服务：避免息屏/休眠时进程被冻结，按键事件必须持续可收
            startForegroundCompat();
            ensureNative();
            serverThread = new Thread(this::startServer);
            serverThread.start();
            startWatchdog();
        }
        return START_STICKY;
    }

    private void startForegroundCompat() {
        try {
            String channelId = "key_intercept";
            NotificationChannel channel = new NotificationChannel(channelId,
                    "按键拦截", NotificationManager.IMPORTANCE_MIN);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);
            Notification n = new Notification.Builder(this, channelId)
                    .setContentTitle("老人机伪装")
                    .setContentText("按键服务运行中")
                    .setSmallIcon(android.R.drawable.ic_lock_lock)
                    .setOngoing(true)
                    .build();
            startForeground(1, n);
        } catch (Exception e) {
            Log.e(TAG, "startForeground err: " + e);
        }
    }

    /** 以特权身份（按设置：Root / Shizuku）安装并启动 native 拦截进程 */
    private void ensureNative() {
        exec.execute(() -> {
            try {
                byte[] buf = readNativeBinary();
                if (buf == null || buf.length == 0) {
                    Log.e(TAG, "native binary not found: " + BIN_LIB_NAME);
                    return;
                }
                String b64 = Base64.encodeToString(buf, Base64.NO_WRAP);
                // 先杀旧实例（单实例），再写入并后台启动
                // （不再做“文件存在就跳过”：随 APK 升级要保证换成新版本二进制）
                PrivilegeProvider.execPrivileged(this, "pkill -f " + BIN_PATH);
                PrivilegeProvider.execPrivileged(this, "echo " + b64 + " | base64 -d > " + BIN_PATH
                        + " && chmod 755 " + BIN_PATH);
                PrivilegeProvider.execPrivileged(this, "nohup " + BIN_PATH + " " + PORT + " >/dev/null 2>&1 &");
                Log.d(TAG, "native ensure issued");
            } catch (Exception e) {
                Log.e(TAG, "ensureNative err", e);
            }
        });
    }

    /**
     * 读回打包成 lib*.so 的可执行文件。
     * 用 nativeLibraryDir 而不是 assets：Android 只会把 native 产物放进 lib/&lt;abi&gt;/，
     * 也因此能拿到 16 KB 对齐、ABI 过滤等构建期保障。
     */
    private byte[] readNativeBinary() {
        File f = new File(getApplicationInfo().nativeLibraryDir, BIN_LIB_NAME);
        if (!f.exists()) return null;
        try (java.io.InputStream is = new java.io.FileInputStream(f)) {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream((int) f.length());
            byte[] chunk = new byte[8192];
            int n;
            while ((n = is.read(chunk)) > 0) bos.write(chunk, 0, n);
            return bos.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, "read native binary failed", e);
            return null;
        }
    }

    /** 杀掉 native 进程，释放按键 */
    public static void killNative(Context ctx) {
        PrivilegeProvider.execPrivileged(ctx, "pkill -f " + BIN_PATH);
        Log.d("KeyInterceptService", "native killed");
    }

    private void startWatchdog() {
        handler.postDelayed(() -> {
            exec.execute(() -> {
                try {
                    // native 不在则重新拉起（幂等；若在则不重复安装/启动）
                    PrivilegeProvider.execPrivileged(this, "pgrep -f " + BIN_PATH + " >/dev/null || (nohup " + BIN_PATH + " " + PORT + " >/dev/null 2>&1 &)");
                } catch (Exception e) {
                    Log.e(TAG, "watchdog err: " + e);
                }
                handler.post(() -> {
                    if (running) startWatchdog();
                });
            });
        }, 30000);
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            Log.d(TAG, "key server listening " + PORT);
            while (running) {
                Socket client = serverSocket.accept();
                handleClient(client);
            }
        } catch (Exception e) {
            Log.e(TAG, "server err", e);
        } finally {
            cleanup();
        }
    }

    private void handleClient(Socket client) {
        try {
            DataInputStream dis = new DataInputStream(client.getInputStream());
            while (running) {
                if (dis.available() < 5) {
                    try { Thread.sleep(10); } catch (InterruptedException ignored) { }
                    continue;
                }
                int code = dis.readInt();
                byte action = dis.readByte();
                onKey(code, action == 1);
            }
        } catch (Exception e) {
            Log.d(TAG, "client disconnected: " + e);
        } finally {
            try { client.close(); } catch (Exception ignored) { }
        }
    }

    /** Linux input EV_KEY code -> Android KeyEvent keycode 映射（对齐 ROM Generic.kl） */
    private static int linuxToAndroid(int code) {
        switch (code) {
            case 2:  return 8;   // KEY_1 -> 1
            case 3:  return 9;   // KEY_2
            case 4:  return 10;  // KEY_3
            case 5:  return 11;  // KEY_4
            case 6:  return 12;  // KEY_5
            case 7:  return 13;  // KEY_6
            case 8:  return 14;  // KEY_7
            case 9:  return 15;  // KEY_8
            case 10: return 16;  // KEY_9
            case 11: return 7;   // KEY_0
            case 14: return 67;  // KEY_BACKSPACE -> DEL
            case 28: return 66;  // KEY_ENTER -> ENTER
            case 56: return 17;  // KEY_KPASTERISK -> STAR (*)
            case 57: return 18;  // KEY_SHARP / KEY_SPACE -> POUND (#)
            case 102: return 3;  // KEY_HOME -> HOME
            case 103: return 19; // KEY_UP -> DPAD_UP
            case 105: return 21; // KEY_LEFT -> DPAD_LEFT
            case 106: return 22; // KEY_RIGHT -> DPAD_RIGHT
            case 108: return 20; // KEY_DOWN -> DPAD_DOWN
            case 113: return 91; // KEY_MUTE -> MUTE
            case 114: return 25; // KEY_VOLUMEDOWN
            case 115: return 24; // KEY_VOLUMEUP
            case 116: return 26; // KEY_POWER
            case 127: return 82; // KEY_MENU(alt) -> MENU
            case 139: return 82; // KEY_MENU -> MENU
            case 152: return 26; // KEY_POWER(alt)
            case 158: return 4;  // KEY_BACK -> BACK
            case 169: return 5;  // KEY_CALL -> CALL（拨号键）
            case 217: return 84; // KEY_SEARCH -> SEARCH
            case 353: return 23; // KEY_DPAD_CENTER -> DPAD_CENTER（确定键）
            case 522: return 17; // KEY_NUMERIC_STAR? -> STAR（*键）
            case 523: return 18; // KEY_NUMERIC_POUND? -> POUND（#键）
            default:  return code; // 未知键：原样注入
        }
    }

    private void onKey(int code, boolean down) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean fakeActive = sp.getBoolean("fakeui_active", false);
        if (down && (code == 114 || code == 115)) {
            lastVolumeDownAt = SystemClock.uptimeMillis();
        }

        if (code == KEYCODE_POWER) {
            if (down) {
                powerDownAt = SystemClock.uptimeMillis();
                powerLongFired = false;
                handler.removeCallbacks(powerLongRunnable);
                powerLongRunnable = () -> {
                    if (powerDownAt > 0 && !powerLongFired) {
                        powerLongFired = true;
                        if (fakeActive) {
                            Log.d(TAG, "power long press -> shutdown activity");
                            try {
                                Intent i = new Intent(this, ShutdownActivity.class);
                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(i);
                            } catch (Exception e) {
                                Log.e(TAG, "start shutdown err", e);
                            }
                        }
                    }
                };
                handler.postDelayed(powerLongRunnable, LONG_PRESS_MS);
            } else {
                boolean longFired = powerLongFired;
                powerDownAt = 0;
                handler.removeCallbacks(powerLongRunnable);
                if (!longFired) {
                if (fakeActive) {
                    // 伪装激活：发广播交 MainActivity 处理——不亮屏时用 IPowerManager.wakeUp 反射真亮屏
                    // （深度休眠后 input 注入不可靠，wakeUp 直接唤醒 PowerManager 更稳），亮屏时 goToSleep 真息屏
                    Log.d(TAG, "power short press -> broadcast");
                    try {
                        Intent bi = new Intent(ACTION_POWERKEY_SHORT);
                        bi.setPackage("com.wtbruh.fakelauncher");
                        sendBroadcast(bi);
                    } catch (Exception e) {
                        Log.e(TAG, "send power broadcast err", e);
                    }
                } else {
                    // 未激活：注入 keyevent 26 交系统原生处理亮屏/息屏
                    Log.d(TAG, "power short press -> inject keyevent 26");
                    exec.execute(() -> PrivilegeProvider.execPrivileged(this, "input keyevent 26"));
                }
            }
            }
            return;
        }

        // 非伪装时：检查启动快捷键（音量键等），命中则消费不注入
        if (!fakeActive && isHotkeyMatch(code, sp)) {
            handleHotkey(code, down, sp);
            return;
        }

        // 伪装激活：音量键不注入系统，静默调媒体音量 + 弹伪装音量界面
        if (fakeActive && (code == 114 || code == 115)) {
            if (down) {
                // 新一次按压开始，清除启动残留标志
                hotkeyStarted = false;
                return;
            }
            // up：本次按压触发过启动（长按/连按）则一律忽略，无论按了多久（参考历史可行方案）
            if (hotkeyStarted) {
                hotkeyStarted = false;
                Log.d(TAG, "ignore residual volume key after hotkey");
                return;
            }
            // 兜底2：本次按压时长 >= 700ms（长按触发过启动）的 up 也忽略（防 repeat down 清掉标志）
            if (SystemClock.uptimeMillis() - lastVolumeDownAt >= 700) {
                Log.d(TAG, "ignore long-press residual up");
                return;
            }
            // 兜底：启动后 1 秒内的音量键 up 也忽略
            if (SystemClock.uptimeMillis() - hotkeyFiredAt < 1000) {
                hotkeyFiredAt = 0;
                Log.d(TAG, "ignore residual volume key after hotkey");
                return;
            }
                boolean volUp = (code == 115);
                Log.d(TAG, "fake volume key -> fake volume ui");
                try {
                    AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                            volUp ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER, 0);
                } catch (Exception e) {
                    Log.e(TAG, "adjust volume err", e);
                }
                try {
                    Intent vi = new Intent(this, com.wtbruh.fakelauncher.ui.VolumeActivity.class);
                    vi.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(vi);
                } catch (Exception e) {
                    Log.e(TAG, "start VolumeActivity err", e);
                }
            return;
        }

        // 非电源键：映射成 Android keycode 后注入回系统（input keyevent 自动含 down+up），伪装界面正常处理
        if (down && fakeActive) {
            final int kc = linuxToAndroid(code);
            Log.d(TAG, "inject linux " + code + " -> android " + kc);
            exec.execute(() -> PrivilegeProvider.execPrivileged(this, "input keyevent " + kc));
        }
    }

    /** 是否匹配用户设置的启动快捷键（Linux code） */
    private boolean isHotkeyMatch(int code, SharedPreferences sp) {
        String key = sp.getString(SettingsConstants.PREF_HOTKEY_KEY, "none");
        if (key == null || "none".equals(key) || key.isEmpty()) return false;
        if ("volume_up".equals(key)) return code == 115;
        if ("volume_down".equals(key)) return code == 114;
        if ("star".equals(key)) return code == 522;
        if ("hash".equals(key)) return code == 523;
        if ("menu".equals(key)) return code == 139 || code == 127;
        if ("back".equals(key)) return code == 158;
        if ("custom".equals(key)) {
            int custom = sp.getInt(SettingsConstants.PREF_HOTKEY_CUSTOM_KEY, -1);
            return custom >= 0 && code == androidToLinux(custom);
        }
        return false;
    }

    /** Android keycode -> Linux code（自定义快捷键录入的是 Android keycode） */
    private static int androidToLinux(int kc) {
        switch (kc) {
            case 24: return 115;  // VOLUME_UP
            case 25: return 114;  // VOLUME_DOWN
            case 17: return 522;  // STAR
            case 18: return 523;  // POUND
            case 82: return 139;  // MENU
            case 4:  return 158;  // BACK
            default: return -1;
        }
    }

    /** 快捷键判定：长按 700ms / 双击 / 三击 -> 启动伪装 */
    private void handleHotkey(int code, boolean down, SharedPreferences sp) {
        String method = sp.getString(SettingsConstants.PREF_HOTKEY_METHOD, "long_press");
        long now = SystemClock.uptimeMillis();
        if (down) {
            if ("long_press".equals(method)) {
                if (hotkeyLongRunnable != null) handler.removeCallbacks(hotkeyLongRunnable);
                hotkeyLongRunnable = () -> {
                    hotkeyLongRunnable = null;
                    startFakeUi();
                };
                handler.postDelayed(hotkeyLongRunnable, 700);
            } else if ("double_press".equals(method)) {
                if (now - lastHotkeyDown < 600) {
                    lastHotkeyDown = 0;
                    startFakeUi();
                } else {
                    lastHotkeyDown = now;
                }
            } else if ("triple_press".equals(method)) {
                if (now - lastHotkeyDown < 600) hotkeyPressCount++;
                else hotkeyPressCount = 1;
                lastHotkeyDown = now;
                if (hotkeyPressCount >= 3) {
                    hotkeyPressCount = 0;
                    startFakeUi();
                }
            }
        } else {
            if ("long_press".equals(method) && hotkeyLongRunnable != null) {
                // 短按（未触发长按）：普通音量行为交给系统
                handler.removeCallbacks(hotkeyLongRunnable);
                hotkeyLongRunnable = null;
                final int kc = linuxToAndroid(code);
                exec.execute(() -> PrivilegeProvider.execPrivileged(this, "input keyevent " + kc));
            }
        }
    }

    private void startFakeUi() {
        hotkeyFiredAt = SystemClock.uptimeMillis();
        hotkeyStarted = true;
        try {
            Log.d(TAG, "hotkey -> start fake ui");
            Intent i = new Intent(this, SplashActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        } catch (Exception e) {
            Log.e(TAG, "startFakeUi err", e);
        }
    }

    private void cleanup() {
        running = false;
        if (serverSocket != null) {
            try { serverSocket.close(); } catch (Exception ignored) { }
        }
    }

    @Override
    public void onDestroy() {
        cleanup();
        // 服务销毁时杀掉 native，释放 /dev/input grab（否则物理按键全被吞，系统无法响应）
        try {
            killNative(this);
        } catch (Throwable ignored) { }
        try {
            stopForeground(true);
        } catch (Exception ignored) { }
        super.onDestroy();
    }
}
