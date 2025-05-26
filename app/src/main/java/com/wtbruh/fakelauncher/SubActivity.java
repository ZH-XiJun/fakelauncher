package com.wtbruh.fakelauncher;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.TextView;

import com.wtbruh.fakelauncher.ui.DialerFragment;
import com.wtbruh.fakelauncher.ui.MenuFragment;
import com.wtbruh.fakelauncher.utils.MyFragment;
import com.wtbruh.fakelauncher.utils.MyAppCompatActivity;

/**
 * <h3>SubActivity</h3>
 * <h5>子界面Activity，管理子界面的Fragment</h5>
 * 可用intent传入字符串数组参数，第一个为Fragment名，后面的是想给Fragment传递的参数，
 * 具体接受哪些参数需要看对应Fragment的代码配置
 */

public class SubActivity extends MyAppCompatActivity {

    public final static int LEFT_BUTTON = R.id.leftButton;
    public final static int CENTER_BUTTON = R.id.centerButton;
    public final static int RIGHT_BUTTON = R.id.rightButton;
    public final static String L_DEFAULT = "L_DEFAULT";
    public final static String L_MENU = "L_MENU";
    public final static String R_DEFAULT = "R_DEFAULT";
    public final static String R_EDITTEXT = "R_EDITTEXT";

    private final static String TAG = SubActivity.class.getSimpleName();

    private Fragment mCurrentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);
        if (savedInstanceState == null) {
            init();
            if (mCurrentFragment == null) mCurrentFragment = MenuFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, mCurrentFragment)
                    .commitNow();
        }
    }

    /**
     * SubActivity 初始化
     */
    private void init() {
        // 设置状态栏颜色为黑色
        getWindow().setStatusBarColor(Color.BLACK);
        // 设置底部状态栏为默认状态
        setFooterBar(L_DEFAULT, R_DEFAULT);
        // 获取附加数据
        Intent intent = getIntent();
        String[] args = intent.getStringArrayExtra("args");
        if (args != null) {
            if (args[0].equals(DialerFragment.class.getSimpleName())) {
                mCurrentFragment = DialerFragment.newInstance(args);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (( (MyFragment) mCurrentFragment ).onKeyDown(keyCode, event)) return true;
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 处理fragment的退出，再处理activity的退出
     */
    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() <= 0) {
            Log.d(TAG, "do back action");
            super.onBackPressed();
        }
        else {
            fm.popBackStackImmediate();
            mCurrentFragment = fm.findFragmentById(R.id.container);
        }
    }

    /**
     * Fragment 启动封装
     * @param fragment Fragment对象
     */
    public void fragmentStarter(Fragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.container, fragment);
        ft.addToBackStack(null);
        ft.commit();
        mCurrentFragment = fragment;
    }

    /**
     * Footer customization<br>
     * 界面底部自定义
     * @param texts 按键提示语
     */
    public void setFooterBar(String... texts) {
        TextView view;
        boolean showCenterButton = false;
        for (String text : texts) {
            Log.d(TAG, "setFooterBar: "+text);
            if (text.contains("L_")) view = findViewById(LEFT_BUTTON);
            else if (text.contains("R_")) view = findViewById(RIGHT_BUTTON);
            else if (text.contains("C_")) {
                view = findViewById(CENTER_BUTTON);
                showCenterButton = true;
            }
            else return;
            switch (text) {
                case L_DEFAULT:
                    view.setText(R.string.common_leftbutton);
                    break;
                case L_MENU:
                    view.setText(R.string.main_leftbutton);
                    break;
                case R_DEFAULT:
                    view.setText(R.string.common_rightbutton);
                    break;
                case R_EDITTEXT:
                    view.setText(R.string.edittext_rightbutton);
            }
        }
        view = findViewById(CENTER_BUTTON);
        if (showCenterButton) {
            view.setVisibility(TextView.VISIBLE);
        } else {
            view.setVisibility(TextView.INVISIBLE);
        }
    }

}