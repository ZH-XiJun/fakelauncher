package com.wtbruh.fakelauncher.ui.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.SubActivity;
import com.wtbruh.fakelauncher.utils.UIHelper;

public class MenuFragment extends BaseFragment {
    private final static String TAG = MenuFragment.class.getSimpleName();

    private GestureDetector mGesture;

    private int mNumber = 0;

    private Bundle[] mAppList;

    public MenuFragment() {
        // Required empty public constructor
    }

    public static MenuFragment newInstance() {
        return new MenuFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_menu, container, false);
        init();
        return rootView;
    }
    @Override
    public void onResume() {
        super.onResume();
        // 重新读取菜单列表：菜单管理配置可能已变更
        Bundle[] newList = UIHelper.getMainMenuAppList(requireContext());
        boolean changed = newList.length != (mAppList == null ? 0 : mAppList.length);
        mAppList = newList;
        if (mNumber >= mAppList.length) mNumber = 0;
        switchSection(mNumber);
        setFooterBar(L_DEFAULT, R_DEFAULT);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (mNumber == 0) {
                    mNumber = mAppList.length - 1;
                } else {
                    mNumber--;
                }
                switchSection(mNumber);
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (mNumber == mAppList.length - 1) {
                    mNumber = 0;
                } else {
                    mNumber++;
                }
                switchSection(mNumber);
                break;
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                startApp(mNumber);
                break;
        }
        return false;
    }

    private void init() {
        mAppList = UIHelper.getMainMenuAppList(requireContext());
        switchSection(mNumber);
        setFooterBar(L_DEFAULT, R_DEFAULT);
        mGesture = new GestureDetector(requireContext(), new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onDown(@NonNull MotionEvent e) {

                return !UIHelper.getCurrentUIType(requireContext()).equals(UIHelper.STYLE_PHONE);
            }
            @Override
            public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
                if (e1.getX() - e2.getX() > 100) {
                    // Swipe left
                    if (mNumber == mAppList.length - 1) {
                        mNumber = 0;
                    } else {
                        mNumber++;
                    }
                    switchSection(mNumber);
                    return true;
                } else if (e2.getX() - e1.getX() > 100) {
                    // Swipe right
                    if (mNumber == 0) {
                        mNumber = mAppList.length - 1;
                    } else {
                        mNumber--;
                    }
                    switchSection(mNumber);
                    return true;
                }
                return false;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                // 手指抬起时立即触发
                startApp(mNumber);
                return true;
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 将触摸事件交给 GestureDetector 处理
        return mGesture.onTouchEvent(event);
    }

    /**
     * Display the corresponding icon and name when switching<br>
     * 切换时显示对应的图标和名字
     *
     * @param number 第几个app
     */
    private void switchSection (int number) {
        ImageView appIcon = rootView.findViewById(R.id.appIcon);
        TextView appName = rootView.findViewById(R.id.appName);
        try {
            Bundle appInfo = mAppList[number];
            if (UIHelper.isExternalApp(appInfo)) {
                // 外部应用：真实应用图标 + 应用名（未安装时回退通用图标）
                appIcon.setImageDrawable(UIHelper.getExternalAppIcon(requireContext(),
                        appInfo.getString(UIHelper.EXT_APP_PKG)));
                appName.setText(appInfo.getString(UIHelper.EXT_APP_LABEL));
            } else {
                appIcon.setImageResource(appInfo.getInt(UIHelper.APP_ICON_RES));
                appName.setText(appInfo.getInt(UIHelper.APP_NAME));
            }
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            Log.e(TAG, "WTF is going on? Got null mAppList!", e);
        }
    }

    /**
     * Launch the corresponding activity using the var "number"<br>
     * 通过number启动对应的App
     * @param number 第几个App
     */
    private void startApp (int number) {
        if (number < 0 || number >= mAppList.length) return;
        Bundle appInfo = mAppList[number];
        if (UIHelper.isExternalApp(appInfo)) {
            // 外部应用：直接启动（自动处理屏幕固定解锁）
            UIHelper.launchExternalApp(requireContext(), appInfo.getString(UIHelper.EXT_APP_PKG));
            return;
        }
        Fragment fragment = UIHelper.findFragmentByName(
                appInfo.getString(UIHelper.TARGET_FRAGMENT));

        if (fragment != null) ((SubActivity)requireActivity()).fragmentStarter(fragment);
    }

}