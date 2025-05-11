package com.wtbruh.fakelauncher;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Keep;

public class UserService extends IUserService.Stub {
    private Context context;
    private DevicePolicyManager dpm;

    private final static String TAG = UserService.class.getSimpleName();

    @Keep
    public UserService(Context context) {
        this.context = context;
        dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm == null) Log.e(TAG, "dpm is null!!!");
    }

    @Override
    public void onCreate() throws RemoteException {

    }

    @Override
    public void onDestroy() throws RemoteException {

    }

    @Override
    public void setLockTaskPackages(ComponentName receiver, String[] packageNames){
        Log.d(TAG, "setLockTaskPackages()");
        dpm.setLockTaskPackages(receiver, packageNames);
    }
}