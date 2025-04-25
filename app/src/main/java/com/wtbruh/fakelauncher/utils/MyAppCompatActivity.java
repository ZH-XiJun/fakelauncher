package com.wtbruh.fakelauncher.utils;

import android.view.KeyEvent;

import androidx.appcompat.app.AppCompatActivity;

import com.wtbruh.fakelauncher.VolumeActivity;

/**
 * 继承AppCompatActivity，添加自己的代码，并让其他Activity继承自己
 */
public class MyAppCompatActivity extends AppCompatActivity {

    @Override
    protected void onPause() {
        // Disable transition anim
        // 去掉过渡动画
        overridePendingTransition(0, 0);
        super.onPause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode){
            // Volume key interruption
            // 音量键拦截
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                UIHelper.intentStarter(this, VolumeActivity.class);
                // 阻止系统音量面板弹出
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
