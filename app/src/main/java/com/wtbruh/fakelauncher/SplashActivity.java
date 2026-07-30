package com.wtbruh.fakelauncher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.wtbruh.fakelauncher.constants.SettingsConstants;
import com.wtbruh.fakelauncher.utils.UIHelper;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        // Disable touch screen
        // 禁用触摸
        UIHelper.setTouchscreenState(false, this);
        // Launch fake ui with flags
        // 带flag启动伪装界面
        startActivity(new Intent()
                .setClass(SplashActivity.this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
        // Vibrate after fake ui being launched
        // 让手机振一下
        if (PreferenceManager.getDefaultSharedPreferences(SplashActivity.this).getBoolean(SettingsConstants.PREF_VIBRATE_ON_START,true)) {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(200);
        }
        finish();
    }

}