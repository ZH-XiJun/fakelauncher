package com.wtbruh.fakelauncher.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.media.AudioManager;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.List;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;
import android.view.accessibility.AccessibilityEvent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import androidx.preference.PreferenceManager;

import com.wtbruh.fakelauncher.MainActivity;
import com.wtbruh.fakelauncher.SplashActivity;
import com.wtbruh.fakelauncher.ShutdownActivity;
import com.wtbruh.fakelauncher.constants.SettingsConstants;

/**
 * 启动快捷键全局监听（无障碍服务）：
 * 在系统任何界面按下设定的按键并按设定方式触发时，拉起伪装界面。
 * 需要用户在系统设置-无障碍中开启本服务。
 */
public class HotkeyAccessibilityService extends AccessibilityService {

    private long lastDown = 0;
    private int pressCount = 0;
    private long lastTrigger = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable longPressRunnable;
    private volatile String mForegroundPkg = "";
    private long lastFakePkgTime = 0;
    private WindowManager mOverlayWm;
    private View mTouchBlock;
    private boolean mTouchBlockActive = false;

    private static int hotkeyCode(String key) {
        switch (key) {
            case "volume_up": return KeyEvent.KEYCODE_VOLUME_UP;
            case "volume_down": return KeyEvent.KEYCODE_VOLUME_DOWN;
            case "star": return KeyEvent.KEYCODE_STAR;
            case "hash": return KeyEvent.KEYCODE_POUND;
            case "menu": return KeyEvent.KEYCODE_MENU;
            case "back": return KeyEvent.KEYCODE_BACK;
            default: return -1;
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        try {
            AccessibilityServiceInfo info = getServiceInfo();
            if (info != null) {
                // 强制请求按键过滤，确保 onKeyEvent 能收到按键（部分 ROM 需 setServiceInfo 才生效）
                info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
                info.eventTypes |= AccessibilityEvent.TYPE_VIEW_CLICKED;
                setServiceInfo(info);
                Log.d("HotkeyService", "connected, flags=" + info.flags
                        + " caps=" + info.getCapabilities());
            }
        } catch (Exception e) {
            Log.d("HotkeyService", "getServiceInfo err", e);
        }
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        android.content.SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String key = sp.getString(SettingsConstants.PREF_HOTKEY_KEY, "none");
        String method = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(SettingsConstants.PREF_HOTKEY_METHOD, "long_press");
        Log.d("HotkeyService", "onKeyEvent code=" + event.getKeyCode() + " action=" + event.getAction()
                + " repeat=" + event.getRepeatCount() + " method=" + method
                + " lastDown=" + lastDown + " fakeFg=" + isFakeUiForeground());
        // 自定义按键录入模式：任意按键按下即记录并退出录入
        if (sp.getBoolean(SettingsConstants.PREF_HOTKEY_CAPTURE, false)) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                int kc = event.getKeyCode();
                sp.edit().putInt(SettingsConstants.PREF_HOTKEY_CUSTOM_KEY, kc)
                        .putBoolean(SettingsConstants.PREF_HOTKEY_CAPTURE, false).apply();
                Log.d("HotkeyService", "captured key=" + kc);
                try {
                    sendBroadcast(new android.content.Intent("com.wtbruh.fakelauncher.HOTKEY_CAPTURED")
                            .putExtra("keycode", kc));
                    Toast.makeText(this, getString(com.wtbruh.fakelauncher.R.string.hotkey_captured, kc),
                            Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Log.d("HotkeyService", "capture notify err", e);
                }
            }
            return true; // 消费按键，避免系统特殊动作（如 SOS）
        }
        if (key == null || key.isEmpty() || "none".equals(key)) return super.onKeyEvent(event);
        int code;
        if ("custom".equals(key)) {
            code = sp.getInt(SettingsConstants.PREF_HOTKEY_CUSTOM_KEY, -1);
        } else {
            code = hotkeyCode(key);
        }
        if (code < 0 || event.getKeyCode() != code) return super.onKeyEvent(event);

        long now = SystemClock.uptimeMillis();

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            // 长按 repeat 事件：消费但不处理，避免反复重置长按定时器导致永不触发
            if (event.getRepeatCount() > 0) return true;
            // 防抖：触发后 1.5 秒内不再触发
            if (now - lastTrigger < 1500) return super.onKeyEvent(event);
            // 已在伪装内：不重复启动伪装，事件交给 app（音量键由伪装界面处理）
            if (isFakeUiForeground()) {
                return super.onKeyEvent(event);
            }
            if ("long_press".equals(method)) {
                // 长按：手动计时 700ms，期间没有抬起则触发（不依赖 repeat 事件）
                if (longPressRunnable != null) handler.removeCallbacks(longPressRunnable);
                final int kc = event.getKeyCode();
                longPressRunnable = () -> {
                    longPressRunnable = null;
                    lastTrigger = SystemClock.uptimeMillis();
                    startFakeUi(kc);
                };
                handler.postDelayed(longPressRunnable, 700);
                // 消费事件：阻止系统直接处理音量键，避免启动伪装时联动弹出音量界面
                return true;
            }
            if ("double_press".equals(method)) {
                // 连按两下：两次按下间隔 <600ms 触发（忽略 repeat，UP 不影响计数）
                if (lastDown != 0 && now - lastDown < 600) {
                    lastDown = 0;
                    lastTrigger = now;
                    startFakeUi(event.getKeyCode());
                } else {
                    lastDown = now;
                }
                // 快捷键模式下音量键全部消费，避免系统弹音量条
                return true;
            } else if ("triple_press".equals(method)) {
                if (now - lastDown < 600) pressCount++;
                else pressCount = 1;
                lastDown = now;
                if (pressCount >= 3) {
                    pressCount = 0;
                    lastTrigger = now;
                    startFakeUi(event.getKeyCode());
                    return true;
                }
                return true;
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            // 抬起：若长按计时尚未触发（短按），手动调节媒体音量并弹音量界面
            if ("long_press".equals(method) && longPressRunnable != null) {
                handler.removeCallbacks(longPressRunnable);
                longPressRunnable = null;
                try {
                    AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
                    boolean up = event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP;
                    if (isFakeUiForeground()) {
                        // 伪装前台：静默调音量 + 弹伪装音量界面
                        am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                                up ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER, 0);
                        Intent vi = new Intent(this, com.wtbruh.fakelauncher.ui.VolumeActivity.class);
                        vi.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(vi);
                        Log.d("HotkeyService", "fake volume ui shown");
                    } else {
                        // 桌面态：系统音量条
                        am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                                up ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER,
                                AudioManager.FLAG_SHOW_UI);
                        Log.d("HotkeyService", "system volume adjust " + (up ? "up" : "down"));
                    }
                } catch (Exception e) {
                    Log.d("HotkeyService", "volume adjust err", e);
                }
            }
        }
        return super.onKeyEvent(event);
    }

    private boolean isFakeUiForeground() {
        // 仅当最近 3 秒内有 fakelauncher 窗口事件才视为伪装前台（避免事件残留误判）
        if ("com.wtbruh.fakelauncher".equals(mForegroundPkg)
                && SystemClock.elapsedRealtime() - lastFakePkgTime < 3000) return true;
        try {
            // 活动窗口判断最准确（系统桌面/音量条等在前台时返回 false）
            List<AccessibilityWindowInfo> windows = getWindows();
            if (windows != null) {
                for (AccessibilityWindowInfo w : windows) {
                    if (w.isActive() && w.getRoot() != null && w.getRoot().getPackageName() != null) {
                        return "com.wtbruh.fakelauncher".contentEquals(w.getRoot().getPackageName());
                    }
                }
            }
            // 不用 getRunningTasks：残留任务栈（如 SettingsActivity）会导致误判
        } catch (Exception e) {
            Log.d("HotkeyService", "isFakeUiForeground err", e);
        }
        return false;
    }

    /** 全屏透明拦截层：系统关机菜单弹出后铺上，完全透明、无视觉内容，仅拦截触摸（防误触） */
    private void showTouchBlockOverlay() {
        try {
            if (mTouchBlock != null) return;
            mOverlayWm = (WindowManager) getSystemService(WINDOW_SERVICE);
            View v = new View(this);
            v.setBackgroundColor(Color.TRANSPARENT);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            lp.gravity = Gravity.TOP | Gravity.START;
            mOverlayWm.addView(v, lp);
            mTouchBlock = v;
            mTouchBlockActive = true;
            Log.d("HotkeyService", "touch block overlay shown");
        } catch (Exception e) {
            Log.d("HotkeyService", "showTouchBlockOverlay err", e);
        }
    }

    /** 移除透明拦截层（系统关机菜单关闭后调用） */
    private void hideTouchBlockOverlay() {
        try {
            if (mTouchBlock != null && mOverlayWm != null) {
                mOverlayWm.removeView(mTouchBlock);
                mTouchBlock = null;
                mTouchBlockActive = false;
                Log.d("HotkeyService", "touch block overlay hidden");
            }
        } catch (Exception e) {
            Log.d("HotkeyService", "hideTouchBlockOverlay err", e);
        }
    }

    private void startFakeUi(int keyCode) {
        try {
            Intent i = new Intent(this, SplashActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            Log.d("HotkeyService", "startFakeUi called, key=" + keyCode);
        } catch (Exception e) {
            Log.d("HotkeyService", "startFakeUi err", e);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event != null && event.getPackageName() != null) {
            mForegroundPkg = event.getPackageName().toString();
            if ("com.wtbruh.fakelauncher".contentEquals(mForegroundPkg)) {
                lastFakePkgTime = SystemClock.elapsedRealtime();
            }
        }
        if (event != null && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            try {
                String pkg = event.getPackageName() == null ? "" : event.getPackageName().toString();
                String cls = event.getClassName() == null ? "" : event.getClassName().toString();
                Log.d("HotkeyService", "WINDOW_STATE pkg=" + pkg + " cls=" + cls);
                boolean isGlobalActions = pkg.contains("systemui") && cls.contains("GlobalActions");
                // 透明拦截层自身的窗口事件（cls 为 overlay 根布局），不参与开/关判断
                boolean isSelfOverlay = "com.wtbruh.fakelauncher".contentEquals(pkg)
                        && ("android.view.View".contentEquals(cls)
                            || "android.widget.LinearLayout".contentEquals(cls));
                if (isGlobalActions && !mTouchBlockActive) {
                    // 伪装内长按电源键：系统关机菜单弹出，立即铺透明拦截层禁触
                    boolean fakeActive = PreferenceManager.getDefaultSharedPreferences(this)
                            .getBoolean("fakeui_active", false);
                    if (fakeActive) {
                        showTouchBlockOverlay();
                    }
                }
                else if (!isGlobalActions && mTouchBlockActive && !isSelfOverlay) {
                    // 系统关机菜单已关闭（回到其他窗口），撤掉拦截层
                    hideTouchBlockOverlay();
                }
            } catch (Exception e) {
                Log.d("HotkeyService", "onAccessibilityEvent err", e);
            }
        }
    }
    @Override
    public void onInterrupt() { }
}
