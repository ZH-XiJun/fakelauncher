package com.wtbruh.fakelauncher.ui;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.SubActivity;
import com.wtbruh.fakelauncher.utils.MyFragment;

public class MenuFragment extends MyFragment {

    private int mNumber = 0;

    public final static int CALL = 0;
    public final static int CAMERA = 1;
    public final static int CONTACT = 2;
    public final static int SMS = 3;
    public final static int SETTINGS = 4;

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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (mNumber == 0) {
                    mNumber = 4;
                } else {
                    mNumber--;
                }
                switchSection(mNumber);
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (mNumber == 4) {
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
        switchSection(mNumber);
        ((SubActivity) getActivity()).setFooterBar(SubActivity.L_DEFAULT, SubActivity.R_DEFAULT);
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
        switch (number) {
            case CALL: // Call 电话
                appIcon.setImageResource(R.drawable.menu_call);
                appName.setText(R.string.menu_call);
                break;
            case CAMERA: // Camera 相机
                appIcon.setImageResource(R.drawable.menu_camera);
                appName.setText(R.string.menu_camera);
                break;
            case CONTACT: // Contact 联系人
                appIcon.setImageResource(R.drawable.menu_contact);
                appName.setText(R.string.menu_contact);
                break;
            case SMS: // SMS 短信
                appIcon.setImageResource(R.drawable.menu_sms);
                appName.setText(R.string.menu_sms);
                break;
            case SETTINGS: // Settings 设置
                appIcon.setImageResource(R.drawable.menu_set);
                appName.setText(R.string.menu_set);
                break;
        }
    }

    /**
     * Launch the corresponding activity using the var "number"<br>
     * 通过number启动对应的Activity
     * @param number 第几个App
     */
    private void startApp (int number) {
        Fragment fragment = null;
        switch (number) {
            case CALL:
                fragment = DialerFragment.newInstance();
                break;
            // work in progress...
            case CAMERA:
                fragment = CameraFragment.newInstance();
                break;
            case CONTACT:
                break;
            case SMS:
                break;
            case SETTINGS:
                fragment = PasswordFragment.newInstance();
                break;
        }
        if (fragment == null) {
            return;
        }
        ((SubActivity)getActivity()).fragmentStarter(fragment);
    }

}