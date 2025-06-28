package com.wtbruh.fakelauncher.utils;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.KeyEvent;

import androidx.appcompat.app.AppCompatActivity;

import com.wtbruh.fakelauncher.VolumeActivity;

/**
 * 继承AppCompatActivity，添加自己的代码，并让其他Activity继承自己
 */
public class MyAppCompatActivity extends AppCompatActivity {

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StatusNavigationUtils.setStatusBarColor(this, getStatusBarColorDefault());
        StatusNavigationUtils.setNavigationBarColor(this, getNavigationBarColorDefault());
        // Lock screen orientation to portrait
        // 锁定竖屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

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

    // StatusBar
    public int getStatusBarColorDefault(){
        return 0xfff2f2f2;
    }
    public int getNavigationBarColorDefault(){
        return 0xff000000;
    }

    public void setStatusBarColor(int color) {
        StatusNavigationUtils.setStatusBarColor(this, color);
    }
    public void setNavigationBarColor(int color) {
        StatusNavigationUtils.setNavigationBarColor(this, color);
    }

    public void setFullScreen() {
        StatusNavigationUtils.setFullScreen(this);
    }
    public void setClearFullScreen() {
        StatusNavigationUtils.setClearFullScreen(this);
    }

    public void setHideStatusBar() {
        StatusNavigationUtils.setHideStatusBar(this);
    }
    public void setClearHideStatusBar() {
        StatusNavigationUtils.setClearHideStatusBar(this);
    }

    public void setHideNavigationBar() {
        StatusNavigationUtils.setHideNavigationBar(this);
    }
    public void setClearHideNavigationBar() {
        StatusNavigationUtils.setClearHideNavigationBar(this);
    }

    public void setStatusBarNoFill() {
        StatusNavigationUtils.setStatusBarNoFill(this);
    }

    public void setStatusBarNoFillAndTransParent() {
        setStatusBarNoFill();
        setStatusBarColor(0x00000000);
    }

    public void setStatusBarNoFillAndTransParentHalf() {
        setStatusBarNoFill();
        setStatusBarColor(0x33000000);
    }

    public void setNavigationBarTransparent() {
        StatusNavigationUtils.setNavigationBarTransparent(this);
    }
}
