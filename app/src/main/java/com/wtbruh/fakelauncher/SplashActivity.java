package com.wtbruh.fakelauncher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.wtbruh.fakelauncher.ui.fragment.settings.SubSettingsFragment;
import com.wtbruh.fakelauncher.utils.UIHelper;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        // Disable touch screen
        UIHelper.setTouchscreenState(false, this);
        // Launch fake ui with flags
        startActivity(new Intent()
                .setClass(SplashActivity.this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
        // Vibrate after fake ui being launched
        if (PreferenceManager.getDefaultSharedPreferences(SplashActivity.this).getBoolean(SubSettingsFragment.PREF_VIBRATE_ON_START,true)) {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(200);
        }
        finish();
    }

}