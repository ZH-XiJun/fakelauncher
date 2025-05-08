package com.wtbruh.fakelauncher.receiver;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

public class DeviceAdminReceiver extends android.app.admin.DeviceAdminReceiver {
    private final static String TAG = DeviceAdminReceiver.class.getSimpleName();

    @Override
    public void onEnabled(@NonNull Context context, @NonNull Intent intent) {
        Log.d(TAG, "Device admin is enabled");
    }

    @Override
    public void onDisabled(@NonNull Context context, @NonNull Intent intent) {
        Log.d(TAG, "Device admin is disabled");
    }
}
