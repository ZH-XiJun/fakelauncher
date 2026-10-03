package com.wtbruh.fakelauncher.ui;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
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
import com.wtbruh.fakelauncher.utils.TelephonyHelper;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Overlay 式状态栏控制器。
 * 在目标 View 的 {@link android.view.ViewOverlay} 上叠加绘制状态图标：
 * 双卡信号、蓝牙、电池。
 * <p>
 * 用法：
 * <pre>
 *   StatusBarOverlay sbo = new StatusBarOverlay(findViewById(R.id.Main));
 *   sbo.start();
 *   ...
 *   sbo.stop();
 *   sbo.destroy();
 * </pre>
 */
public class StatusBarOverlay implements PowerConnectionReceiver.getStat {

    private static final String TAG = "StatusBarOverlay";
    private static final long DEFAULT_POLL_MS = 3000;
    private static final int MARGIN = 10;   // px，与屏幕边缘的间距
    private static final int GAP = 4;       // px，图标之间的间距

    private final View mTarget;
    private final Context mContext;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final TelephonyHelper mTelephonyHelper;

    // ── 电池 ──
    private Drawable mBatteryDrawable;
    private int mBatteryLevel = 4;
    private int mPreviousBattery = -1;
    private Timer mChargingAnimTimer;

    // ── 双卡信号 ──
    private Drawable mSignal1Drawable, mSignal2Drawable;
    private int mSignal1Level = 0, mSignal2Level = 0;
    private boolean mDualSim;           // 设备是否双卡
    private boolean mSim1Ready, mSim2Ready;

    // ── 蓝牙 ──
    private Drawable mBluetoothDrawable;
    private boolean mBluetoothOn;

    // ── 轮询 ──
    private Timer mPollTimer;
    private boolean mPolling;

    // ── 广播 ──
    private BroadcastReceiver mStateReceiver;
    private boolean mReceiverRegistered;

    // ── 图标资源 ──
    private static final int[] SIGNAL_ICONS = {
            R.drawable.ic_signal_0, R.drawable.ic_signal_1,
            R.drawable.ic_signal_2, R.drawable.ic_signal_3, R.drawable.ic_signal_4
    };
    private static final int[] BATTERY_ICONS = {
            R.drawable.ic_battery_0,  // 0-10%  电量告急，将军救急
            R.drawable.ic_battery_1,  // 11-25% 还能争
            R.drawable.ic_battery_2,  // 26-50% 2格绿
            R.drawable.ic_battery_3,  // 51-75% 3格绿
            R.drawable.ic_battery_4   // 76-100% 4格绿
    };

    public StatusBarOverlay(View target) {
        mTarget = target;
        mContext = target.getContext();
        mTelephonyHelper = new TelephonyHelper(mContext);
    }

    // ═══════════════════════════════════════════
    // 生命周期
    // ═══════════════════════════════════════════


    /**
     * 启动状态栏数据更新，使用默认轮询间隔 3000ms
     */
    public void start() {
        start(DEFAULT_POLL_MS);
    }

    /**
     * 启动状态栏数据更新
     * @param pollMs 轮询间隔
     */
    public void start(long pollMs) {
        // 设备信息（只取一次）
        mDualSim = mTelephonyHelper.getPhoneCount() >= 2;

        // 初始状态立即同步
        Intent batteryStatus = mContext.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus != null) {
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            setCharging(status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL);
        }
        syncAll();
        registerStateReceiver();
        startPolling(pollMs);
    }

    /** 停止轮询 + 注销广播。Activity onPause 中调用。 */
    public void stop() {
        stopPolling();
        stopChargingAnim();
        unregisterStateReceiver();
    }

    /** 清理所有 drawable、定时器、广播。Activity onDestroy 中调用。 */
    public void destroy() {
        stop();
        mTarget.post(() -> {
            removeDrawable(mBatteryDrawable);   mBatteryDrawable = null;
            removeDrawable(mSignal1Drawable);   mSignal1Drawable = null;
            removeDrawable(mSignal2Drawable);   mSignal2Drawable = null;
            removeDrawable(mBluetoothDrawable); mBluetoothDrawable = null;
        });
    }

    // ═══════════════════════════════════════════
    // 电源连接（PowerConnectionReceiver 回调）
    // ═══════════════════════════════════════════

    @Override
    public void getConnectionStatus(String status) {
        if (status.equals(Intent.ACTION_POWER_CONNECTED)) {
            setCharging(true);
        } else if (status.equals(Intent.ACTION_POWER_DISCONNECTED)) {
            setCharging(false);
        }
    }

    public void setCharging(boolean charging) {
        if (charging) startChargingAnim(); else stopChargingAnim();
    }

    // ═══════════════════════════════════════════
    // 电池（公共）
    // ═══════════════════════════════════════════

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
                mHandler.post(StatusBarOverlay.this::syncAll);
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
    // 同步所有状态
    // ═══════════════════════════════════════════

    private void syncAll() {
        syncBattery();
        syncSignals();
        syncBluetooth();
        redrawAll();
    }

    private void syncBattery() {
        Intent batteryStatus = mContext.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) return;
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int pct = (level >= 0 && scale > 0) ? level * 100 / scale : 100;
        if (pct != mPreviousBattery) {
            setBattery(pct);
            mPreviousBattery = pct;
        }
    }

    private void syncSignals() {
        mSim1Ready = mTelephonyHelper.isSimReady(0);
        mSignal1Level = mSim1Ready ? mTelephonyHelper.getSignalLevel(0) : 0;

        if (mDualSim) {
            mSim2Ready = mTelephonyHelper.isSimReady(1);
            mSignal2Level = mSim2Ready ? mTelephonyHelper.getSignalLevel(1) : 0;
        } else {
            mSim2Ready = false;
            mSignal2Level = 0;
        }
    }

    private void syncBluetooth() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        try {
            mBluetoothOn = adapter != null && adapter.isEnabled();
        } catch (SecurityException ignore) {
            mBluetoothOn = false;
        }
    }

    // ═══════════════════════════════════════════
    // 状态广播（蓝牙 / 电池 实时更新）
    // ═══════════════════════════════════════════

    private void registerStateReceiver() {
        if (mReceiverRegistered) return;
        mStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) return;
                switch (action) {
                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        syncBluetooth();
                        redrawAll();
                        break;
                    case Intent.ACTION_BATTERY_CHANGED:
                        syncBattery();
                        break;
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        mContext.registerReceiver(mStateReceiver, filter);
        mReceiverRegistered = true;
    }

    private void unregisterStateReceiver() {
        if (!mReceiverRegistered) return;
        try {
            mContext.unregisterReceiver(mStateReceiver);
        } catch (Exception ignore) { }
        mReceiverRegistered = false;
        mStateReceiver = null;
    }

    // ═══════════════════════════════════════════
    // 绘制
    // ═══════════════════════════════════════════

    private void redrawAll() {
        mTarget.post(() -> {
            int sw = mTarget.getWidth();
            if (sw <= 0) return;

            removeAllDrawables();

            // ── 左侧图标组：SIM1 → SIM2 → 蓝牙 ──
            int x = MARGIN;
            int y = MARGIN;

            // SIM1 信号（无 SIM 时隐藏）
            if (mSim1Ready) {
                mSignal1Drawable = ContextCompat.getDrawable(mContext, SIGNAL_ICONS[mSignal1Level]);
                x = placeIcon(mSignal1Drawable, x, y);
            }

            // SIM2 信号（仅双卡 + SIM 就绪时显示）
            if (mDualSim && mSim2Ready) {
                mSignal2Drawable = ContextCompat.getDrawable(mContext, SIGNAL_ICONS[mSignal2Level]);
                x = placeIcon(mSignal2Drawable, x, y);
            }

            // 蓝牙
            if (mBluetoothOn) {
                mBluetoothDrawable = ContextCompat.getDrawable(mContext, R.drawable.ic_bluetooth);
                x = placeIcon(mBluetoothDrawable, x, y);
            }

            // ── 右侧：电池 ──
            try {
                mBatteryDrawable = ContextCompat.getDrawable(mContext, BATTERY_ICONS[mBatteryLevel]);
                if (mBatteryDrawable != null) {
                    int iw = mBatteryDrawable.getIntrinsicWidth();
                    int ih = mBatteryDrawable.getIntrinsicHeight();
                    mBatteryDrawable.setBounds(sw - MARGIN - iw, y, sw - MARGIN, y + ih);
                    mTarget.getOverlay().add(mBatteryDrawable);
                }
            } catch (Exception e) {
                Log.e(TAG, "drawBattery failed", e);
            }
        });
    }

    /** 放置一个图标并返回下一个图标应放置的 x 坐标。 */
    private int placeIcon(Drawable d, int x, int y) {
        if (d == null) return x;
        int iw = d.getIntrinsicWidth();
        int ih = d.getIntrinsicHeight();
        d.setBounds(x, y, x + iw, y + ih);
        mTarget.getOverlay().add(d);
        return x + iw + GAP;
    }

    /** 仅更新电池图标（充电动画/外部 setBattery 使用），不重绘全部。 */
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
                int iw = mBatteryDrawable.getIntrinsicWidth();
                int ih = mBatteryDrawable.getIntrinsicHeight();
                mBatteryDrawable.setBounds(sw - MARGIN - iw, MARGIN,
                        sw - MARGIN, MARGIN + ih);
                mTarget.getOverlay().add(mBatteryDrawable);
            }
        });
    }

    private void removeDrawable(Drawable d) {
        if (d != null) {
            try { mTarget.getOverlay().remove(d); } catch (Exception ignore) { }
        }
    }

    private void removeAllDrawables() {
        removeDrawable(mBatteryDrawable);   mBatteryDrawable = null;
        removeDrawable(mSignal1Drawable);   mSignal1Drawable = null;
        removeDrawable(mSignal2Drawable);   mSignal2Drawable = null;
        removeDrawable(mBluetoothDrawable); mBluetoothDrawable = null;
    }

    // ═══════════════════════════════════════════
    // 充电动画
    // ═══════════════════════════════════════════

    private void startChargingAnim() {
        stopChargingAnim();
        if (mBatteryLevel == 4) return; // 满电
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
}
