package com.wtbruh.fakelauncher.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;

public class ScreenObserver {
    private final Context mContext;
    private final ScreenBroadcastReceiver mScreenReceiver;
    private ScreenStateListener mScreenStateListener;
    boolean isObserver;

    public ScreenObserver(Context context) {
        mContext = context;
        mScreenReceiver = new ScreenBroadcastReceiver();
    }

    public void startScreenObserver(ScreenStateListener listener) {
        if (!isObserver) {
            mScreenStateListener = listener;
            registerListener();
            getScreenState();
            isObserver = true;
        }
    }

    public void stopScreenObserver() {
        if (isObserver) {
            unregisterListener();
            isObserver = false;
        }
    }

    private void getScreenState() {
        PowerManager manager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        if (manager.isInteractive()) {
            if (mScreenStateListener != null) {
                mScreenStateListener.onScreenOn();
            }
        } else {
            if (mScreenStateListener != null) {
                mScreenStateListener.onScreenOff();
            }
        }
    }

    private void registerListener() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        mContext.registerReceiver(mScreenReceiver, filter);
    }

    private void unregisterListener() {
        mContext.unregisterReceiver(mScreenReceiver);
    }

    private class ScreenBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                mScreenStateListener.onScreenOn();
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                mScreenStateListener.onScreenOff();
            } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                mScreenStateListener.onUserPresent();
            }
        }
    }

    public interface ScreenStateListener {
        void onScreenOn();
        void onScreenOff();
        void onUserPresent();
    }
}
