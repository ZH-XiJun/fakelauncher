package com.wtbruh.fakelauncher.ui;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.receiver.PowerConnectionReceiver;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Overlay 式状态栏控制器。
 * 在目标 View 的 {@link android.view.ViewOverlay} 上叠加绘制状态图标（电池、WiFi 等），
 * 各图标独立管理（add/remove），互不干扰。
 * 内置电池轮询，Activity 只需 startPolling / stopPolling / destroy。
 * <p>
 * 用法：
 * <pre>
 *   StatusBarOverlay sbo = new StatusBarOverlay(findViewById(R.id.Main));
 *   sbo.start();                                // 初始同步 + 启动轮询
 *   ...
 *   sbo.stop();                                 // onPause / onStop
 *   sbo.destroy();                              // onDestroy
 * </pre>
 */
public class StatusBarOverlay implements PowerConnectionReceiver.getStat {

    private static final String TAG = "StatusBarOverlay";
    private static final long DEFAULT_POLL_MS = 3000;

    private final View mTarget;
    private final Context mContext;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    // ── 电池 ──
    private Drawable mBatteryDrawable;
    private int mBatteryLevel = 4; // 默认满电
    private int mPreviousBattery = -1;
    private boolean mCharging;
    private Timer mChargingAnimTimer;

    // ── 轮询 ──
    private Timer mPollTimer;
    private boolean mPolling;

    private static final int[] BATTERY_ICONS = {
            R.drawable.ic_battery_0,  // 0-10%  红色告急
            R.drawable.ic_battery_1,  // 11-25% 橙色
            R.drawable.ic_battery_2,  // 26-50% 2格绿
            R.drawable.ic_battery_3,  // 51-75% 3格绿
            R.drawable.ic_battery_4   // 76-100% 4格绿
    };

    public StatusBarOverlay(View target) {
        mTarget = target;
        mContext = target.getContext();
    }

    // ═══════════════════════════════════════════
    // 生命周期
    // ═══════════════════════════════════════════

    /** 初始同步一次 + 启动默认间隔轮询。在 Activity onResume 中调用。 */
    public void start() {
        start(DEFAULT_POLL_MS);
    }

    /** 初始同步一次 + 按指定间隔轮询。 */
    public void start(long pollMs) {
        Intent batteryStatus = mContext.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus != null) {
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            setCharging(status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL);
        }

        syncBattery();
        startPolling(pollMs);
    }

    /** 停止轮询 + 充电动画。在 Activity onPause 中调用。 */
    public void stop() {
        stopPolling();
        stopChargingAnim();
    }

    /** 清理所有 drawable 和定时器。Activity onDestroy 中调用。 */
    public void destroy() {
        stop();
        removeDrawable(mBatteryDrawable);
        mBatteryDrawable = null;
    }

    public void setCharging(boolean charging) {
        if (charging) startChargingAnim(); else stopChargingAnim();
    }

    // ═══════════════════════════════════════════
    // 电池（公共 — 外部也可直接更新）
    // ═══════════════════════════════════════════

    /** 手动更新电池百分比（0-100）。*/
    public void setBattery(int pct) {
        int level;
        if (pct >= 76)      level = 4;
        else if (pct >= 51) level = 3;
        else if (pct >= 26) level = 2;
        else if (pct >= 11) level = 1;
        else                level = 0;
        if (level == mBatteryLevel) return;
        mBatteryLevel = level;
        drawBattery(level);
    }

    // ═══════════════════════════════════════════
    // 轮询
    // ═══════════════════════════════════════════

    private void startPolling(long intervalMs) {
        if (mPolling) return;
        mPolling = true;
        mPollTimer = new Timer("StatusBarPoll", true);
        mPollTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mHandler.post(StatusBarOverlay.this::syncBattery);
            }
        }, intervalMs, intervalMs);
    }

    private void stopPolling() {
        mPolling = false;
        if (mPollTimer != null) {
            mPollTimer.cancel();
            mPollTimer = null;
        }
    }

    // ═══════════════════════════════════════════
    // 系统同步
    // ═══════════════════════════════════════════

    private void syncBattery() {
        Intent batteryStatus = mContext.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) return;

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int pct = (level >= 0 && scale > 0) ? level * 100 / scale : 100;

        Log.d(TAG, "syncBattery: level=" + level + ", scale=" + scale + ", pct=" + pct);

        if (pct != mPreviousBattery) {
            setBattery(pct);
            mPreviousBattery = pct;
        }
    }

    // ═══════════════════════════════════════════
    // 绘制
    // ═══════════════════════════════════════════

    private void drawBattery(int level) {
        mTarget.post(() -> {
            removeDrawable(mBatteryDrawable);
            try {
                mBatteryDrawable = ContextCompat.getDrawable(mContext, BATTERY_ICONS[level]);
            } catch (Exception e) {
                Log.e(TAG, "drawBattery failed", e);
                mBatteryDrawable = null;
                return;
            }
            if (mBatteryDrawable != null) {
                int sw = mTarget.getWidth();
                int margin = 10;
                int iw = mBatteryDrawable.getIntrinsicWidth();
                int ih = mBatteryDrawable.getIntrinsicHeight();
                mBatteryDrawable.setBounds(sw - margin - iw, margin, sw - margin, margin + ih);
                mTarget.getOverlay().add(mBatteryDrawable);
            }
        });
    }

    private void removeDrawable(Drawable d) {
        if (d != null) {
            try {
                mTarget.getOverlay().remove(d);
            } catch (Exception ignore) { }
        }
    }

    // ═══════════════════════════════════════════
    // 充电动画
    // ═══════════════════════════════════════════

    private void startChargingAnim() {
        stopChargingAnim();
        if (mBatteryLevel == 3) return; // 已满电不需要动画
        mChargingAnimTimer = new Timer("BatteryChargingAnim", true);
        mChargingAnimTimer.schedule(new TimerTask() {
            int i = mBatteryLevel;
            @Override
            public void run() {
                if (i < BATTERY_ICONS.length - 1) i++;
                else i = Math.max(1, mBatteryLevel);
                drawBattery(i);
            }
        }, 0, 1000);
    }

    private void stopChargingAnim() {
        if (mChargingAnimTimer != null) {
            mChargingAnimTimer.cancel();
            mChargingAnimTimer = null;
        }
        drawBattery(mBatteryLevel);
    }

    @Override
    public void getConnectionStatus(String status) {
        if (status.equals(Intent.ACTION_POWER_CONNECTED)) {
            setCharging(true);
        } else if (status.equals(Intent.ACTION_POWER_DISCONNECTED)) {
            setCharging(false);
        }
    }
}
