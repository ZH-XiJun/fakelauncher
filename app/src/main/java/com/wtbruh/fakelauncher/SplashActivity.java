package com.wtbruh.fakelauncher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.wtbruh.fakelauncher.ui.SettingsFragment;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Vibrate on launching fake ui
        if (PreferenceManager.getDefaultSharedPreferences(SplashActivity.this).getBoolean(SettingsFragment.PREF_VIBRATE_ON_START,true)) {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(200);
        }
        startActivity(new Intent()
                .setClass(SplashActivity.this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }

}