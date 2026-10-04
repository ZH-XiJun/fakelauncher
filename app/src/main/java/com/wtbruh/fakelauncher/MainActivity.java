package com.wtbruh.fakelauncher;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import com.rosan.dhizuku.api.Dhizuku;
import com.wtbruh.fakelauncher.receiver.DeviceAdminReceiver;
import com.wtbruh.fakelauncher.receiver.PowerConnectionReceiver;
import com.wtbruh.fakelauncher.service.KeyInterceptService;
import com.wtbruh.fakelauncher.service.MusicService;
import com.wtbruh.fakelauncher.ui.StatusBarOverlay;
import com.wtbruh.fakelauncher.ui.VolumeActivity;
import com.wtbruh.fakelauncher.ui.fragment.MenuFragment;
import com.wtbruh.fakelauncher.ui.fragment.player.MusicPlayerFragment;
import com.wtbruh.fakelauncher.ui.fragment.phone.DialerFragment;
import com.wtbruh.fakelauncher.constants.SettingsConstants;
import com.wtbruh.fakelauncher.ui.widget.FitTextView;
import com.wtbruh.fakelauncher.utils.ContentProvider;
import com.wtbruh.fakelauncher.utils.LunarCalender;
import com.wtbruh.fakelauncher.ui.BaseAppCompatActivity;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;
import com.wtbruh.fakelauncher.utils.ScreenObserver;
import com.wtbruh.fakelauncher.utils.TelephonyHelper;
import com.wtbruh.fakelauncher.utils.UIHelper;

import rikka.shizuku.Shizuku;
import rikka.shizuku.SystemServiceHelper;
import rikka.shizuku.ShizukuBinderWrapper;
import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends BaseAppCompatActivity implements PowerConnectionReceiver.getStat, ScreenObserver.ScreenStateListener, View.OnTouchListener {

    // extra args 额外参数
    public final static String
            EXTRA_PREVIEW = "preview",
            EXTRA_EXIT = "exit";
    private String mStyle;

    // 挂机键锁屏息屏
    private boolean mScreenOff = false;          // 息屏状态标志
    private int mOrigScreenTimeout = -1;         // 原屏幕超时（用于真息屏）
    private int mOrigStayOn = -1;                 // 原保持唤醒设置（充电时）
    private boolean mWriteSettingsPrompted = false;
    // ROM 电源键短按广播（老人机定制：按挂机键 = 系统发此广播而非直接息屏）
    private static final String ACTION_POWERKEY_SHORT = "android.powerkey.short-press";
    private final BroadcastReceiver mPowerKeyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "powerkey short-press received");
            // 纯预览：忽略（预览模式不接管电源键，更不锁任务/息屏）
            if (mPreviewMode) return;
            // 以系统真实屏幕状态为准（mScreenOff 应用标志可能与实际不同步，例如屏幕超时息屏）
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            boolean interactive = pm != null && pm.isInteractive();
            if (!interactive) {
                // 系统已息屏：短按电源 = 真亮屏（Shizuku wakeUp）
                mScreenOff = true;
                wakeFromSleep();
                shizukuWake();
                // 深度休眠后锁任务可能失效：唤醒后拉回伪装桌面并恢复锁任务，防止停在系统锁屏/设置页
                try {
                    Intent mi = new Intent(context, MainActivity.class);
                    mi.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    context.startActivity(mi);
                } catch (Exception e) {
                    Log.e(TAG, "bring back main err", e);
                }
                int lockedTask = -1;
                if (context instanceof android.app.Activity) lockedTask = ((android.app.Activity) context).getTaskId();
                if (lockedTask < 0) lockedTask = UIHelper.getLockApp(context);
                final int ftask = lockedTask;
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        UIHelper.setLockApp(context, ftask);
                    } catch (Throwable ignored) { }
                }, 400);
                return;
            }
            // 系统亮屏：应用标志错位时仅恢复
            if (mScreenOff) {
                mScreenOff = false;
                return;
            }
            String top = ApplicationHelper.topActivity;
            if (top == null || top.contains(MainActivity.class.getSimpleName())) {
                lockAndSleep();
            }
        }
    };

    // UI scale
    private float mScale = 1;
    // Custom dialog
    private Dialog dialog;
    // date
    private final static int TIME = 0, DATE = 1, WEEK = 2;

    // battery
    private StatusBarOverlay mStatusBarOverlay;
    private boolean mCharging = false, mShowAccurateBattery = false;

    // 广播接收器 Broadcast receiver
    private final PowerConnectionReceiver mReceiver = new PowerConnectionReceiver();
    private boolean mReceiverRegistered = false;

    // is screen off 是否熄屏
    private boolean mLocked = false;

    // key long press check 按键长按检查
    private boolean mKeyLongPressed = false;

    // preview mode 预览模式
    private boolean mPreviewMode = false;

    // Regularly refresh data
    // 计时任务
    private String mPreviousDate, previousTime;
    private Timer mTimer;

    // key detection 按键检测相关
    private int mKeyCount = 0;
    private int[] mKeyAction = {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT
    };

    // Device owner
    private int mDeviceAdminType = PrivilegeProvider.DEACTIVATED;
    private DevicePolicyManager mDpm;

    private final static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        // 隐藏系统状态栏：桌面状态图标由 Overlay（双卡信号+电池）独占，避免与系统图标重叠
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .hide(WindowInsetsCompat.Type.statusBars());

        // Switch UI style
        mStyle = UIHelper.getCurrentUIType(this);
        if (mStyle.equals(UIHelper.STYLE_PLAYER)) {
            setContentView(R.layout.activity_main_player);
        } else {
            setContentView(R.layout.activity_main_phone);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.Main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Android 13 返回键走 OnBackPressedDispatcher，不走 onKeyDown。
        // 桌面态按右软键 = 上锁 + 息屏；锁屏/伪息屏态 = 亮屏/解锁提示。
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 纯预览：直接退出，不做上锁/息屏
                if (mPreviewMode) {
                    finish();
                    return;
                }
                if (mScreenOff) {
                    wakeFromSleep();
                    return;
                }
                if (!mLocked) {
                    lockAndSleep();
                    return;
                }
                // 锁屏态按返回：显示解锁提示（与原有按键提示一致）
                setDialog(UIHelper.showCustomDialog(MainActivity.this,
                        R.string.dialog_press_star_unlock, (dialogInterface, keyCode1, keyEvent) -> {
                            if (keyCode1 == KeyEvent.KEYCODE_STAR) onUnlocked();
                            return true;
                        }));
            }
        });

        // 注册挂机键（电源键）短按广播 → 真熄屏
        try {
            registerReceiver(mPowerKeyReceiver, new IntentFilter(ACTION_POWERKEY_SHORT));
        } catch (Exception e) {
            Log.e(TAG, "register powerkey receiver failed: " + e);
        }
        // If expected preview, then only initialize UI
        // 如果只需要预览，就只做UI初始化
        if (getIntent().getBooleanExtra(EXTRA_PREVIEW, false)) {
            mPreviewMode = true;
            initUI();
        } else init();
    }

    /**
     * Init of MainActivity | MainActivity初始化
     */
    private void init() {
        Log.d(TAG, "Now start init, UI style: " + mStyle);
        // Common init 通用初始化代码
        // UI init
        initUI();
        // Manually get battery 手动获取电池电量
        setBattery();
        // Register the receiver 注册接收器
        receiverRegister(true);
        // Manually get connection status 手动获取连接状态
        getConnectionStatus();
        if (mStyle.equals(UIHelper.STYLE_PLAYER)) {
            findViewById(R.id.Main).setOnTouchListener(this);
        } else { // Default/Fallback: feature phone UI
            ScreenObserver screenObserver = new ScreenObserver(this);
            screenObserver.startScreenObserver(this);
            // Device owner init
            initDeviceOwner();
            // Start pin mode 启用屏幕固定
            // 延迟到主线程 post：等 ApplicationHelper.onActivityResumed 更新 sCurrentActivity，
            // 否则 observer 拿不到 activity，startLockTask 不执行（首次进入常失效 -> 唤醒回原桌面）
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    UIHelper.setLockApp(MainActivity.this, getTaskId());
                } catch (Throwable ignored) { }
            }, 300);

            // key action init
            if (UIHelper.checkExitMethod(this, UIHelper.EXIT_METHOD_DPAD)) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
                String keyActionStr = sp.getString(SettingsConstants.PREF_EXIT_FAKEUI_CONFIG_KEY, "");
                if (!keyActionStr.isEmpty()) {
                    String[] array = keyActionStr.split(",");
                    mKeyAction = new int[array.length];
                    int index = 0;
                    for (String keyCode : array) {
                        mKeyAction[index] = Integer.parseInt(keyCode);
                        index++;
                    }
                }
            }

        }
        // Start timer 启动计时任务
        updateInfo();
        // 启动按键拦截（0.6.1）：Shizuku 拉起 native grab，物理按键由我们接管
        try {
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                startForegroundService(new Intent(this, KeyInterceptService.class));
            } else {
                startService(new Intent(this, KeyInterceptService.class));
            }
        } catch (Exception e) {
            Log.e(TAG, "start KeyInterceptService failed: " + e);
        }
    }

    /**
     * Init of MainActivity user interface | MainActivity 用户界面初始化
     */
    private void initUI() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        mScale = (float) sp.getInt(SettingsConstants.PREF_MAIN_UI_HEIGHT_SCALE, 10) / 10;
        batteryAccurate();
        if (mStyle.equals(UIHelper.STYLE_PLAYER)) {
            // todo: mp3 ui init
        } else { // Default/Fallback: feature phone UI
            int[] simpleResizableView = {
                    R.id.date,
                    R.id.lunarDate,
                    R.id.simCard,
                    R.id.main_ActionBar
            };
            if (mScale > 0 && mScale != 1.0) {
                for (int resId : simpleResizableView) {
                    View view = findViewById(resId);
                    UIHelper.resizeView(mScale, view);
                }
            }
            // 下面的自缩放屎山代码实则少一条都不行，能用就别动了

            TelephonyHelper mTelHelper = new TelephonyHelper(this);
            FitTextView
                    card1 = findViewById(R.id.card1_provider),
                    card2 = findViewById(R.id.card2_provider);
            View cardProvider = findViewById(R.id.cardProvider);
            card1.setText(mTelHelper.getProvidersName(0));
            card2.setText(mTelHelper.getProvidersName(1));

            cardProvider.post(() -> {
                cardProvider.getLayoutParams().width = WRAP_CONTENT;
                cardProvider.getViewTreeObserver().addOnGlobalLayoutListener(UIHelper.getFitWidthViewsListener(cardProvider, card1, card2));
                cardProvider.requestLayout();
            });

            UIHelper.resizeView(mScale,
                    findViewById(R.id.statusBar),
                    findViewById(R.id.connection),
                    findViewById(R.id.battery));

            // Overlay 状态栏：双卡真实信号 + 电池图标（无 WiFi/蓝牙）
            mStatusBarOverlay = new StatusBarOverlay(findViewById(R.id.Main));
            mStatusBarOverlay.start();
        }
        // Common init 通用初始化代码
        // 时间字体大小自适应适配
        FitTextView time = findViewById(R.id.time);
        time.post(() -> {
            boolean pref = sp.getBoolean(SettingsConstants.PREF_TIME_SHOW_SECOND, false);
            if (mPreviewMode) time.setText(pref? "11:45:14" : "19:19" );
            time.getLayoutParams().height = (int) (time.getHeight() * mScale);
            time.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    time.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    time.fitHeight();
                }
            });
            time.requestLayout();
        });
    }

    /**
     * 当想要退出App时，重复启动MainActivity可触发退出
     */
    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        boolean isExit = intent.getBooleanExtra(EXTRA_EXIT, false);
        if (isExit) exit();
    }

    /**
     * do necessary codes before calling onDestroy()<br>
     * 调用onDestroy()之前需要执行的代码
     */
    private void exit() {
        try {
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                    .putBoolean("fakeui_active", false).apply();
        } catch (Exception e) { }
        // 退出伪装：停止按键拦截，释放物理按键（0.6.1）
        try {
            KeyInterceptService.killNative(this);
        } catch (Exception e) {
            Log.e(TAG, "kill native failed: " + e);
        }
        try {
            stopService(new Intent(this, KeyInterceptService.class));
        } catch (Exception e) {
            Log.e(TAG, "stop KeyInterceptService failed: " + e);
        }
        // 退出伪装：停止音乐播放（0.6.2 音乐回归：退出时音乐随之关闭）
        try {
            stopService(new Intent(this, MusicService.class));
        } catch (Exception e) {
            Log.e(TAG, "stop MusicService failed: " + e);
        }
        // 退出伪装：清空音乐缓存（下次进入重新扫描曲目，发现新增歌曲）
        try {
            MusicPlayerFragment.clearCache();
        } catch (Exception e) {
            Log.e(TAG, "clear music cache failed: " + e);
        }
        if (mTimer != null) mTimer.cancel();
        // Disable pin mode
        // 关闭屏幕固定
        UIHelper.setLockApp(this, -1);
        // 主动调用原生 stopLockTask() 兜底解除屏幕固定（不依赖异步观察者回调）
        try {
            stopLockTask();
        } catch (Exception e) {
            Log.e(TAG, "stopLockTask failed", e);
        }
        // Wait for pin mode disabled, or finishAffinity() won't work
        // 等待屏幕固定被关闭，不然finishAffinity()没用
        try {
            Thread.sleep(700);
        } catch (InterruptedException e) {
            Log.e(TAG, "An error was occurred while waiting screen pinning to close: ", e);
        }
        try {
            finishAffinity();
        } catch (Exception e) {
            Log.e(TAG, "finishAffinity failed", e);
        }
        // 兜底：若仍处于前台（finishAffinity 被系统拦截），把任务移回桌面，离开伪装界面
        if (!isFinishing()) {
            moveTaskToBack(true);
        }
    }

    private void setDialog(Dialog dialog) {
        if (this.dialog != null && this.dialog.isShowing()) this.dialog.dismiss();
        this.dialog = dialog;
    }

    @Override
    protected void onDestroy() {
        // 纯预览：从未锁过任务，不要在这里去解锁
        if (!mPreviewMode && UIHelper.getLockApp(MainActivity.this) != -1)
            UIHelper.setLockApp(MainActivity.this, -1);
        // Unregister the receiver on destroy
        receiverRegister(false);
        // 停止计时任务 Stop timer
        // 退出伪装：停止音乐播放（0.6.2 音乐回归：退出时音乐随之关闭）
        try {
            stopService(new Intent(this, MusicService.class));
        } catch (Exception e) {
            Log.e(TAG, "stop MusicService failed: " + e);
        }
        // 退出伪装：清空音乐缓存（下次进入重新扫描曲目，发现新增歌曲）
        try {
            MusicPlayerFragment.clearCache();
        } catch (Exception e) {
            Log.e(TAG, "clear music cache failed: " + e);
        }
        if (mTimer != null) mTimer.cancel();
        // 注销电源键广播
        try { unregisterReceiver(mPowerKeyReceiver); } catch (Exception ignore) { }
        // 清理 overlay
        if (mStatusBarOverlay != null) mStatusBarOverlay.destroy();
        super.onDestroy();
        // Enable touch screen
        // 纯预览：预览不改动触控状态
        if (!mPreviewMode) UIHelper.setTouchscreenState(true, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPreviewMode) {
            // 纯预览：只刷新 UI，不改任何系统/应用状态
            // （不写 fakeui_active、不锁任务、不禁触控、不动系统熄屏超时）
            batteryAccurate();
            if (mStatusBarOverlay != null && !mShowAccurateBattery) mStatusBarOverlay.start();
            return;
        }
        try {
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                    .putBoolean("fakeui_active", true).apply();

        } catch (Exception e) { }
        // 从设置等触屏界面返回主菜单时，恢复伪装禁触（若增强触屏拦截开启）
        UIHelper.setTouchscreenState(false, this);
        restoreScreenTimeoutIfClamped();
        batteryAccurate();
        // 恢复 overlay 轮询
        if (mStatusBarOverlay != null && !mShowAccurateBattery) mStatusBarOverlay.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 暂停 overlay 轮询（省电）
        if (mStatusBarOverlay != null) mStatusBarOverlay.stop();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d(TAG,"onTouch");
        if (mStyle.equals(UIHelper.STYLE_PHONE)) return false;

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (!UIHelper.intentStarterDebounce(SubActivity.class)) {
                Intent intent = new Intent().setClass(MainActivity.this, SubActivity.class)
                        .putExtra(SubActivity.HIDE_ACTION_BAR, true);
                if (false) {// Detect if playing music
                    intent.putExtra(SubActivity.TARGET_FRAGMENT, MusicPlayerFragment.class.getName());

                } else {
                    intent.putExtra(SubActivity.TARGET_FRAGMENT, MenuFragment.class.getName());
                }
                startActivity(intent);
                // Disable transition anim
                // 去掉过渡动画
                overridePendingTransition(0, 0);
            }
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 纯预览：不响应任何伪装按键逻辑（挂机键上锁/息屏、音量键弹伪装音量页等）
        if (mPreviewMode) return super.onKeyDown(keyCode, event);
        // 挂机键：桌面态上锁并息屏；伪息屏态任意键亮屏
        if (keyCode == KeyEvent.KEYCODE_ENDCALL) {
            if (mScreenOff) {
                wakeFromSleep();
                return true;
            }
            if (!mLocked) {
                lockAndSleep();
                return true;
            }
            return true; // 锁屏态下挂机键忽略
        }
        // 右软键（返回键）：桌面态按下 = 无操作（空置，不锁屏不息屏，避免执行息屏命令带来的问题）
        if (!mLocked && keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        if (mScreenOff) {
            wakeFromSleep();
            return true;
        }
        // 音量键：显示伪装音量界面，阻止系统音量面板
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            UIHelper.startIntent(this, VolumeActivity.class);
            return true;
        }
        if (mStyle.equals(UIHelper.STYLE_PHONE)) {
            // Star key long press detection
            // 长按星键检测
            if (mLocked && keyCode == KeyEvent.KEYCODE_STAR && event.getRepeatCount() >= 2) {
                mKeyLongPressed = true;
                onUnlocked();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mPreviewMode) {
            return super.onKeyUp(keyCode, event);
        }
        if (mStyle.equals(UIHelper.STYLE_PHONE)) {
            if (mLocked) { // 需要解锁的情况 Need star key unlock
                if (keyCode == KeyEvent.KEYCODE_MENU) {
                    setDialog(UIHelper.showCustomDialog(this, R.string.dialog_press_star_unlock, (dialogInterface, keyCode1, keyEvent) -> {
                        if (keyCode1 == KeyEvent.KEYCODE_STAR) {
                            onUnlocked();
                        }
                        return true;
                    }));
                } else {
                    if (!mKeyLongPressed)
                        setDialog(UIHelper.showCustomDialog(this, R.string.dialog_long_press_star_unlock, (dialogInterface, keyCode1, keyEvent) -> {
                            if (keyCode1 != KeyEvent.KEYCODE_BACK) {
                                if (keyEvent.getRepeatCount() >= 2) {
                                    mKeyLongPressed = true;
                                    onUnlocked();
                                }
                            }
                            return true;
                        }));
                }
                // 展示提示弹窗后不会执行下面的代码
                // The codes below will not be executed. Only show dialog
                return super.onKeyUp(keyCode, event);
            }
            if (mKeyLongPressed) {
                mKeyLongPressed = false;
                return true;
            }
            // Unlocked 已解锁后
            counter(keyCode);
            if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                    if (!PreferenceManager.getDefaultSharedPreferences(this)
                            .getBoolean(SettingsConstants.PREF_DPAD_CENTER_OPEN_MENU, false)
                    ) return super.onKeyUp(keyCode, event);
                }
                // Open menu UI
                // 打开菜单界面
                UIHelper.startIntent(MainActivity.this, SubActivity.class);

            } else if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_POUND) {
                // Simulate the logic of the elders' phone: Pressing the number keys on the main UI will open the dialer
                // 模拟老人机逻辑：主界面按数字键打开拨号盘
                String key = switch (keyCode) {
                    case KeyEvent.KEYCODE_POUND -> "#";
                    case KeyEvent.KEYCODE_STAR -> "*";
                    default ->
                        // Key 0~9 0到9键
                            String.valueOf(keyCode - KeyEvent.KEYCODE_0);
                };
                Bundle extra = new Bundle();
                extra.putString(DialerFragment.ARG_INPUT, key);
                if (!UIHelper.intentStarterDebounce(SubActivity.class)) {
                    startActivity(
                            new Intent().setClass(MainActivity.this, SubActivity.class)
                                    .putExtra(SubActivity.TARGET_FRAGMENT, DialerFragment.class.getName())
                                    .putExtra(SubActivity.FRAGMENT_ARGS, extra)
                    );
                    // Disable transition anim
                    // 去掉过渡动画
                    overridePendingTransition(0, 0);
                }
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * 通过 Shizuku（shell 权限）发送 SLEEP 键（keyevent 223），系统直接休眠（真熄屏）。
     * 需要 Shizuku 服务运行 + 应用已授权；未授权则请求授权。
     */
    private boolean shizukuSleep() {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    android.os.IBinder binder = SystemServiceHelper.getSystemService("power");
                    if (binder == null) {
                        Log.d(TAG, "shizuku power binder null");
                        return false;
                    }
                    // 关键：包装为 ShizukuBinderWrapper，调用以 shell 身份执行（有 DEVICE_POWER）
                    binder = new ShizukuBinderWrapper(binder);
                    // 隐藏 API 需豁免，否则 Class.forName/getMethod 被拦截
                    HiddenApiBypass.addHiddenApiExemptions("Landroid/os/IPowerManager;");
                    Class<?> cls = Class.forName("android.os.IPowerManager$Stub");
                    Object pm = cls.getMethod("asInterface", android.os.IBinder.class).invoke(null, binder);
                    Class<?> iface = Class.forName("android.os.IPowerManager");
                    try {
                        HiddenApiBypass.getDeclaredMethod(iface, "goToSleep",
                                long.class, int.class, int.class)
                                .invoke(pm, SystemClock.uptimeMillis(), 0, 0);
                    } catch (NoSuchMethodException e) {
                        HiddenApiBypass.getDeclaredMethod(iface, "goToSleep", long.class)
                                .invoke(pm, SystemClock.uptimeMillis());
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        Log.e(TAG, "goToSleep ITE cause: " + e.getCause());
                        throw e;
                    }
                    Log.d(TAG, "shizuku goToSleep invoked");
                    return true;
                } else {
                    Log.d(TAG, "shizuku not granted, fallback to timeout");
                    // 未授权时不弹窗打扰，走超时兜底；可在 Shizuku app 里手动授权后自动启用 Shizuku
                }
            } else {
                Log.d(TAG, "shizuku binder not available");
            }
        } catch (Exception e) {
            Log.e(TAG, "shizuku sleep failed: " + e);
        }
        return false;
    }

    /**
     * 通过 Shizuku（shell 权限）调用 IPowerManager.wakeUp，真正亮屏。
     * 与 shizukuSleep 对称；用于伪装息屏后短按电源键唤醒。
     */
    private boolean shizukuWake() {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    android.os.IBinder binder = SystemServiceHelper.getSystemService("power");
                    if (binder == null) {
                        Log.d(TAG, "shizuku wake power binder null");
                        return false;
                    }
                    binder = new ShizukuBinderWrapper(binder);
                    HiddenApiBypass.addHiddenApiExemptions("Landroid/os/IPowerManager;");
                    Class<?> cls = Class.forName("android.os.IPowerManager$Stub");
                    Object pm = cls.getMethod("asInterface", android.os.IBinder.class).invoke(null, binder);
                    Class<?> iface = Class.forName("android.os.IPowerManager");
                    long now = SystemClock.uptimeMillis();
                    try {
                        // Android 12+：wakeUp(long, int, String, int, String, int)
                        HiddenApiBypass.getDeclaredMethod(iface, "wakeUp",
                                long.class, int.class, String.class, int.class, String.class, int.class)
                                .invoke(pm, now, 0, "fakelauncher-wake", 0, "com.wtbruh.fakelauncher", 0);
                    } catch (NoSuchMethodException e) {
                        try {
                            HiddenApiBypass.getDeclaredMethod(iface, "wakeUp",
                                    long.class, int.class, String.class)
                                    .invoke(pm, now, 0, "fakelauncher-wake");
                        } catch (NoSuchMethodException e2) {
                            HiddenApiBypass.getDeclaredMethod(iface, "wakeUp", long.class, int.class)
                                    .invoke(pm, now, 0);
                        }
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        Log.e(TAG, "wakeUp ITE cause: " + e.getCause());
                        throw e;
                    }
                    Log.d(TAG, "shizuku wakeUp invoked");
                    return true;
                } else {
                    Log.d(TAG, "shizuku not granted, wake fallback");
                }
            } else {
                Log.d(TAG, "shizuku binder not available for wake");
            }
        } catch (Exception e) {
            Log.e(TAG, "shizuku wake failed: " + e);
            // 兜底：反射失败时通过特权 shell（Root / Shizuku）注入 keyevent 26（adb shell 已验证可亮屏）
            try {
                PrivilegeProvider.execPrivileged(this, "input keyevent 26");
                Log.d(TAG, "wake fallback: injected keyevent 26");
                return true;
            } catch (Throwable t) {
                Log.e(TAG, "wake fallback input failed: " + t);
            }
        }
        return false;
    }

    /**
     * 桌面按挂机键：上锁 + 息屏。
     * 无系统 DEVICE_POWER 权限时降级为全黑覆盖 + 背光最暗（近似息屏）。
     */
    private void lockAndSleep() {
        onLocked();
        mScreenOff = true;
        // 真息屏：优先 Shizuku（shell 权限）goToSleep 强制休眠（可靠灭屏）
        // 注：注入 keyevent 26 只可唤醒不可关屏（系统策略），关屏必须 goToSleep
        if (shizukuSleep()) {
            Log.d(TAG, "shizuku real screen off");
            return;
        }
        // 兜底：stay_on_while_plugged_in=0 + screen_off_timeout=1000 仍可灭屏（需修改系统设置权限）
        // 真息屏：关闭“充电时保持唤醒” + 屏幕超时设为 1 秒（需“修改系统设置”权限）。
        // 验证：stay_on_while_plugged_in=0 + screen_off_timeout=1000 → 系统 1 秒内真正休眠（背光灭）。
        try {
            if (Settings.System.canWrite(this)) {
                mOrigScreenTimeout = Settings.System.getInt(getContentResolver(),
                        Settings.System.SCREEN_OFF_TIMEOUT, 30000);
                try {
                    mOrigStayOn = Settings.Global.getInt(getContentResolver(),
                            Settings.Global.STAY_ON_WHILE_PLUGGED_IN, 0);
                    Settings.Global.putInt(getContentResolver(),
                            Settings.Global.STAY_ON_WHILE_PLUGGED_IN, 0);
                } catch (Exception e) {
                    Log.d(TAG, "global stayOn write skipped: " + e);
                }
                Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_OFF_TIMEOUT, 1000);
                Log.d(TAG, "real screen off: stayOn=0 timeout=1000");
                // 触发用户活动，让系统立即检查超时（API33 userActivity 已移除公开签名，用反射兜底）
                try {
                    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    java.lang.reflect.Method m = pm.getClass().getMethod(
                            "userActivity", long.class, boolean.class);
                    m.invoke(pm, SystemClock.uptimeMillis(), false);
                } catch (Exception e) {
                    Log.d(TAG, "userActivity reflect failed: " + e);
                }
            } else {
                Log.d(TAG, "no WRITE_SETTINGS");
                if (!mWriteSettingsPrompted) {
                    mWriteSettingsPrompted = true;
                    setDialog(UIHelper.showCustomDialog(MainActivity.this,
                            R.string.write_settings_prompt, (dialogInterface, keyCode1, keyEvent) -> {
                                if (keyCode1 == KeyEvent.KEYCODE_ENTER
                                        || keyCode1 == KeyEvent.KEYCODE_MENU
                                        || keyCode1 == KeyEvent.KEYCODE_DPAD_CENTER) {
                                    try {
                                        startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                                Uri.parse("package:" + getPackageName())));
                                    } catch (Exception ignore) { }
                                    return true;
                                }
                                return true;
                            }));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "timeout set failed: " + e);
        }
        // 尝试真正息屏（普通应用无 DEVICE_POWER 会被拒，能成功最好）；
        // Android 13 公开 API 已移除 goToSleep，改用反射避免编译依赖
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            java.lang.reflect.Method m = pm.getClass().getMethod("goToSleep", long.class);
            m.invoke(pm, SystemClock.uptimeMillis());
            Log.d(TAG, "goToSleep reflect OK - real screen off");
        } catch (Exception e) {
            Log.e(TAG, "goToSleep reflect failed: " + e);
        }
    }

    /**
     * 兜底：检测屏幕超时是否被"真熄屏超时兜底"钳制为极短值（1000ms）。
     * 进程重启后内存中的原值丢失、恢复逻辑失效，会卡在 1 秒导致频繁息屏。
     * 发现残留（<=5000ms 非正常用户设置）就恢复为 30 秒默认。
     */
    private void restoreScreenTimeoutIfClamped() {
        try {
            if (!Settings.System.canWrite(this)) return;
            int cur = Settings.System.getInt(getContentResolver(),
                    Settings.System.SCREEN_OFF_TIMEOUT, -1);
            if (cur > 0 && cur <= 5000) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_OFF_TIMEOUT, 30000);
                Log.d(TAG, "restored clamped screen_off_timeout " + cur + " -> 30000");
            }
        } catch (Exception e) {
            Log.d(TAG, "restore check skipped: " + e);
        }
    }

    /** 伪装内熄屏时间（秒）；<=0 表示不干预系统设置 */
    private int getScreenTimeoutPrefSec() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getInt(SettingsConstants.PREF_FAKEUI_SCREEN_TIMEOUT, 60);
    }

    /** 将伪装内熄屏时间应用到系统屏幕超时；首次保存系统原值 */
    private void applyScreenTimeoutToSystem() {
        int timeout = getScreenTimeoutPrefSec();
        if (timeout <= 0) return;
        try {
            if (!Settings.System.canWrite(this)) return;
            if (mOrigScreenTimeout < 0) {
                mOrigScreenTimeout = Settings.System.getInt(getContentResolver(),
                        Settings.System.SCREEN_OFF_TIMEOUT, 30000);
            }
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SCREEN_OFF_TIMEOUT, timeout);
            Log.d(TAG, "fake screen timeout applied: " + timeout + "ms");
        } catch (Exception e) {
            Log.d(TAG, "applyScreenTimeoutToSystem err: " + e);
        }
    }

    /** 退出伪装：恢复系统原始屏幕超时与保持唤醒设置 */
    private void restoreOrigScreenTimeout() {
        if (mOrigScreenTimeout > 0) {
            try {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_OFF_TIMEOUT, mOrigScreenTimeout);
            } catch (Exception ignore) { }
            mOrigScreenTimeout = -1;
        }
        if (mOrigStayOn >= 0) {
            try {
                Settings.Global.putInt(getContentResolver(),
                        Settings.Global.STAY_ON_WHILE_PLUGGED_IN, mOrigStayOn);
            } catch (Exception ignore) { }
            mOrigStayOn = -1;
        }
    }

    /** 亮屏：移除黑屏覆盖并恢复背光。 */
    private void wakeFromSleep() {
        mScreenOff = false;
        restoreScreenTimeoutIfClamped();
        // 恢复系统屏幕超时 + 保持唤醒
        if (mOrigScreenTimeout > 0) {
            try {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_OFF_TIMEOUT, mOrigScreenTimeout);
            } catch (Exception ignore) { }
            mOrigScreenTimeout = -1;
        }
        if (mOrigStayOn >= 0) {
            try {
                Settings.Global.putInt(getContentResolver(),
                        Settings.Global.STAY_ON_WHILE_PLUGGED_IN, mOrigStayOn);
            } catch (Exception ignore) { }
            mOrigStayOn = -1;
        }
    }

    @Override
    public void onScreenOn() {
        mScreenOff = false; // 系统真实亮屏，同步应用标志
        restoreScreenTimeoutIfClamped();
        if (mOrigScreenTimeout > 0) {
            try {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_OFF_TIMEOUT, mOrigScreenTimeout);
            } catch (Exception ignore) { }
            mOrigScreenTimeout = -1;
        }
        if (mOrigStayOn >= 0) {
            try {
                Settings.Global.putInt(getContentResolver(),
                        Settings.Global.STAY_ON_WHILE_PLUGGED_IN, mOrigStayOn);
            } catch (Exception ignore) { }
            mOrigStayOn = -1;
        }
        onLocked();
    }
    @Override
    public void onScreenOff() {
        mScreenOff = true; // 系统真实息屏，同步应用标志
        // Clear dialog on screen off
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = null;
        mKeyLongPressed = false;
    }
    @Override
    public void onUserPresent() {}

    /**
     * init of DeviceOwner | DeviceOwner 初始化
     */
    private void initDeviceOwner() {
        // Check privilege level
        // 检查权限等级
        mDeviceAdminType = PrivilegeProvider.checkDeviceAdmin(this);
        mDpm = switch (mDeviceAdminType) {
            // Dhizuku已激活
            case PrivilegeProvider.DHIZUKU -> PrivilegeProvider.binderWrapperDevicePolicyManager(this);
            // Device Owner已激活
            case PrivilegeProvider.DEVICE_OWNER -> ContextCompat.getSystemService(this, DevicePolicyManager.class);
            default -> null;
        };
    }

    /**
     * Footer customization<br>
     * 界面底部自定义
     * @param resId 文字的资源id
     */
    private void setFooterBar(int resId) {
        TextView leftButtonTv = findViewById(R.id.main_leftButton);
        leftButtonTv.setText(resId);
    }

    private void onLocked() {
        String topActivity = ApplicationHelper.topActivity;
        if (topActivity != null) if (!topActivity.contains(MainActivity.class.getSimpleName())) return;
        mLocked = true;
        setFooterBar(R.string.unlock_leftButton);
    }
    private void onUnlocked() {
        /*
        // New dialog should be opened before closing the old one.
        Dialog dialog2;
        dialog2 = UIHelper.showCustomDialog(MainActivity.this, R.string.dialog_unlocked, null);
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = dialog2;
        setFooterBar(R.string.main_leftButton);
        mLocked = false;
         */
        setDialog(UIHelper.showCustomDialog(MainActivity.this, R.string.dialog_unlocked, null));
        setFooterBar(R.string.main_leftButton);
        mLocked = false;
    }

    /**
     * Get time info 获取时间信息
     * @param target true为获取时间，false为获取日期
     * @return 返回时间/日期信息
     */
    private String getTime(int target){
        long rawTime = System.currentTimeMillis();
        Date d = new Date(rawTime);
        SimpleDateFormat format = null;
        SharedPreferences defaultPref = PreferenceManager.getDefaultSharedPreferences(this);
        switch (target) {
            case TIME: {
                // Check if showing seconds
                boolean showSecond = defaultPref.getBoolean(SettingsConstants.PREF_TIME_SHOW_SECOND, false);
                String pattern = showSecond ? "HH:mm:ss" : "HH:mm";
                format = new SimpleDateFormat(pattern, Locale.getDefault());
                break;
            }
            case DATE: {
                String pattern = UIHelper.STYLE_PLAYER.equals(mStyle) ? "yyyy-MM-dd" : getResources().getString(R.string.date_format);
                format = new SimpleDateFormat(pattern, Locale.getDefault());
                break;
            }
            case WEEK: {
                format = new SimpleDateFormat("E", Locale.getDefault());
                break;
            }
        }
        return format == null ? "" : format.format(d) ;
    }

    /**
     * Automatically update data 定时更新数据
     */
    private void updateInfo() {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                new Handler(Looper.getMainLooper()).post(() -> {
                    TextView timeView = findViewById(R.id.time), 
                            dateView = findViewById(R.id.date),
                            lunarView = findViewById(R.id.lunarDate),
                            weekView = UIHelper.STYLE_PLAYER.equals(mStyle) ? findViewById(R.id.week) : null;
                    String time = getTime(TIME),
                            date = getTime(DATE);
                    if (!time.equals(previousTime)) {
                        timeView.setText(time);
                        previousTime = time;
                    }
                    if (!date.equals(mPreviousDate)) {
                        if (weekView != null) weekView.setText(getTime(WEEK));
                        if (lunarView != null) lunarView.setText(LunarCalender.getLunarString(LunarCalender.getDateArray()));
                        dateView.setText(date);
                        mPreviousDate = date;
                    }
                });
            }
        }, 0, 1000);
    }
    @SuppressLint("SetTextI18n")
    private void setBattery(int battery) {
        if (mStyle.equals(UIHelper.STYLE_PLAYER)) {
            TextView battery_view = findViewById(R.id.battery);
            battery_view.setText(battery+"%");
        }
        else if (mStyle.equals(UIHelper.STYLE_PHONE)) {
            if (mShowAccurateBattery) {
                TextView battery_view = findViewById(R.id.battery);
                battery_view.setText(battery+"%");
            } else {
                if (mStatusBarOverlay != null) mStatusBarOverlay.setBattery(battery);
            }
        }
    }

    private void setBattery() {
        setBattery(getBattery());
    }

    /**
     * Get battery percent 获取电量百分比
     * @return 返回电量百分比
     */
    private int getBattery() {

        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, filter);

        int defaultValue = -1, level = defaultValue, scale = defaultValue;

        if (batteryStatus != null) {
            level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, defaultValue);
            scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, defaultValue);
        }

        float batteryPct = level * 100 / (float) scale;
        return (int) batteryPct;
    }

    /**
     * Get connection status manually<br>
     * 手动获取连接状态
     */
    private void getConnectionStatus() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, filter);
        int defaultValue = -1, status = defaultValue;

        if (batteryStatus != null) {
            status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, defaultValue);
        }
        mCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
        setConnectionStatus();
    }

    /**
     * Get connection status from PowerConnectionReceiver<br>
     * 从PowerConnectionReceiver里获取连接状态
     * @param status PowerConnectionReceiver返回的充电状态
     */
    @Override
    public void getConnectionStatus(String status) {
        if (status.equals(Intent.ACTION_POWER_CONNECTED)) {
            mCharging = true;
        } else if (status.equals(Intent.ACTION_POWER_DISCONNECTED)) {
            mCharging = false;
        }
        setConnectionStatus();
    }

    private void setConnectionStatus() {
        if (mStyle.equals(UIHelper.STYLE_PLAYER)) {
            View connection_view = findViewById(R.id.connection);
            if (mCharging) {
                connection_view.setVisibility(View.VISIBLE);
            } else {
                connection_view.setVisibility(View.GONE);
            }

        } else {
            TextView connection_view = findViewById(R.id.connection);
            if (mCharging) {
                if (mShowAccurateBattery) connection_view.setText(R.string.charging);
                else if (mStatusBarOverlay != null) mStatusBarOverlay.setCharging(true);

            } else {
                if (mShowAccurateBattery) connection_view.setText(R.string.not_charging);
                else if (mStatusBarOverlay != null) mStatusBarOverlay.setCharging(false);
            }
        }
    }

    /**
     * Check if user needs to show accurate battery.<br>
     * If so, lunar calendar should be hidden<br>
     * 检查用户是否需要显示精确电量，<br>
     * 如果是，则需隐藏农历显示
     */
    private void batteryAccurate() {
        View statusBarView = findViewById(R.id.statusBar),
                dateView = switch (mStyle) {
                    case UIHelper.STYLE_PLAYER -> findViewById(R.id.week);
                    default -> findViewById(R.id.lunarDate); // case UIHelper.STYLE_PHONE
                };
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mShowAccurateBattery = sharedPrefs.getBoolean(SettingsConstants.PREF_SHOW_ACCURATE_BATTERY, false);
        statusBarView.setVisibility(mShowAccurateBattery? View.VISIBLE : View.INVISIBLE);
        if (mStyle.equals(UIHelper.STYLE_PHONE)) dateView.setVisibility(mShowAccurateBattery? View.INVISIBLE : View.VISIBLE);

    }

    /**
     * <h3>Receiver Register | 接收器动态注册</h3>
     * Implement of dynamically register the PowerConnectionReceiver<br>
     * 为PowerConnectionReceiver实现动态注册
     * @param operation 操作类型，true为注册，false为注销
     */
    private void receiverRegister(boolean operation) {
        IntentFilter ifilter = new IntentFilter();
        ifilter.addAction(Intent.ACTION_POWER_CONNECTED);
        ifilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        if (operation) {
            if (!mReceiverRegistered) {
                registerReceiver(mReceiver, ifilter);
                mReceiver.setStat(this);
                mReceiverRegistered = true;
                Log.d(TAG, "Receiver registered!");
            } else Log.w(TAG, "Receiver has already registered!");
        } else {
            if (mReceiverRegistered) {
                unregisterReceiver(mReceiver);
                mReceiverRegistered = false;
                Log.d(TAG, "Receiver unregistered!");
            } else Log.w(TAG, "Receiver was not registered yet!");
        }
    }

    /**
     * <h3>Key counter | 按键计数器</h3>
     * Expected key operation: up, up, down, down, left, right, left, right<br>
     * 预计的按键操作：上上下下左右左右
     *
     * @param keycode 键值
     */
    private void counter(int keycode) {
        if (! UIHelper.checkExitMethod(this, UIHelper.EXIT_METHOD_DPAD)) return;
        if (mKeyCount < 0 || mKeyCount > mKeyAction.length - 1) mKeyCount = 0;

        if (keycode != mKeyAction[mKeyCount]) mKeyCount = 0;
        else if (mKeyCount < mKeyAction.length - 1) mKeyCount++;
        else if (mKeyCount == mKeyAction.length - 1) {
            mKeyCount = 0;
            exit();
        }
        Log.d(TAG,"counter(): validKeyCount="+ mKeyCount);
    }

}
