package com.wtbruh.fakelauncher;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.wtbruh.fakelauncher.utils.UIHelper;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIHelper.intentStarter(
                SplashActivity.this,
                MainActivity.class,
                Intent.FLAG_ACTIVITY_CLEAR_TASK,
                Intent.FLAG_ACTIVITY_NEW_TASK
        );
        finish();
    }
}