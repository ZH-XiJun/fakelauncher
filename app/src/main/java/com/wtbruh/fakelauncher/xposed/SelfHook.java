package com.wtbruh.fakelauncher.xposed;

import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.wtbruh.fakelauncher.utils.HookHelper;

public class SelfHook extends HookHelper implements SharedPreferences.OnSharedPreferenceChangeListener{
    private final static String TAG = SelfHook.class.getSimpleName();

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        logI(TAG, "shared preference changed");
    }

    @Override
    public void init() {

    }
}
