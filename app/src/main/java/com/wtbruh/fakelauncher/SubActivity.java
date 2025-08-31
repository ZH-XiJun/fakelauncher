package com.wtbruh.fakelauncher;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.TextView;

import com.wtbruh.fakelauncher.ui.fragment.phone.MenuFragment;
import com.wtbruh.fakelauncher.ui.fragment.BaseFragment;
import com.wtbruh.fakelauncher.ui.BaseAppCompatActivity;
import com.wtbruh.fakelauncher.ui.fragment.phone.OptionMenuFragment;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <h3>SubActivity</h3>
 * <h5>子界面Activity，管理子界面的Fragment</h5>
 * 可用intent传入字符串数组参数，第一个为Fragment名，后面的是想给Fragment传递的参数，
 * 具体接受哪些参数需要看对应Fragment的代码配置
 */

public class SubActivity extends BaseAppCompatActivity {

    public final static int LEFT_BUTTON = R.id.leftButton;
    public final static int CENTER_BUTTON = R.id.centerButton;
    public final static int RIGHT_BUTTON = R.id.rightButton;

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
        setFooterBar(BaseFragment.L_DEFAULT, BaseFragment.R_DEFAULT);
        // 获取附加数据
        Intent intent = getIntent();
        String[] args = intent.getStringArrayExtra("args");
        // 通过反射找到对应Fragment
        String fragmentName;
        Class<?> fragmentClass = null;
        if (args != null && ! (fragmentName = args[0]).isEmpty()) {
            // Get newInstance(String[] args) method
            try {
                fragmentClass = Class.forName(fragmentName);
                if (Fragment.class.isAssignableFrom(fragmentClass)) {
                    Method newInstance = fragmentClass.getMethod("newInstance", String[].class);
                    newInstance.setAccessible(true);
                    mCurrentFragment = (Fragment) newInstance.invoke(fragmentClass, (Object) args);
                    return;
                }
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "Got a non-existent class: "+e);
                return;
            } catch (NoSuchMethodException e) {
                Log.e(TAG, "Class doesn't have newInstance() accepts parameters: " + e);
            } catch (Exception e) {
                Log.e(TAG, "Got error: "+e);
                return;
            }
            // Get newInstance() method
            try {
                mCurrentFragment = (Fragment) fragmentClass.newInstance();
            } catch (Exception e) {
                Log.e(TAG, "Got error: "+e);
            }
        }
    }

    /**
     * No touch event | 禁用触控
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (((BaseFragment) mCurrentFragment).onKeyUp(keyCode, event)) return true;
        return super.onKeyUp(keyCode, event);
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

    public void showOptionMenu(String[] strings, OptionMenuFragment.onKeyUpListener listener) {
        List<String> data = new ArrayList<>(Arrays.asList(strings));
        showOptionMenu(data, listener);
    }

    public void showOptionMenu(List<String> items, OptionMenuFragment.onKeyUpListener listener) {
        Fragment f = new OptionMenuFragment(items, listener);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.optionMenu, f);
        ft.commit();
        mCurrentFragment = f;
    }

    public void closeOptionMenu() {
        if (mCurrentFragment instanceof OptionMenuFragment) {
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.remove(mCurrentFragment);
            ft.commit();
            mCurrentFragment = fm.findFragmentById(R.id.container);
        }
    }

    /**
     * Footer customization<br>
     * 界面底部自定义
     * @param texts 按键提示语
     */
    public void setFooterBar(String[]... texts) {
        TextView view;
        boolean showCenterButton = false;
        for (String[] text : texts) {
            // 先判断是给哪个键设置的
            view = switch (text[0]) {
                case BaseFragment.LEFT_BTN -> findViewById(LEFT_BUTTON);
                case BaseFragment.RIGHT_BTN -> findViewById(RIGHT_BUTTON);
                case BaseFragment.CENTER_BTN -> findViewById(CENTER_BUTTON);
                default -> null;
            };
            if (view == null) return;
            // Check if center button need to show. If showCenterButton is already true, don't do duplicate check
            // 检查中键是否需要显示。如果showCenterButton在先前的循环中已被设置为true则不要重复检查
            if (!showCenterButton) showCenterButton = BaseFragment.CENTER_BTN.equals(text[0]);
            // 获取资源id | get resource id
            int resId = Integer.parseInt(text[1]);
            if (resId == -1) view.setVisibility(TextView.INVISIBLE);
            else {
                view.setText(resId);
                view.setVisibility(TextView.VISIBLE);
            }
        }
        view = findViewById(CENTER_BUTTON);
        if (!showCenterButton) {
            view.setVisibility(TextView.INVISIBLE);
        }
    }

}