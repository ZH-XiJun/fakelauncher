package com.wtbruh.fakelauncher.ui.phone;

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

    public final static int CALL = 0; // The first app
    public final static int CONTACT = 1;
    public final static int SMS = 2;
    public final static int CAMERA = 3;
    public final static int GALLERY = 4;
    public final static int SETTINGS = 5; // The last app

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
        setFooterBar(SubActivity.L_DEFAULT, SubActivity.R_DEFAULT);
        super.onResume();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (mNumber == CALL) {
                    mNumber = SETTINGS;
                } else {
                    mNumber--;
                }
                switchSection(mNumber);
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (mNumber == SETTINGS) {
                    mNumber = CALL;
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
        setFooterBar(SubActivity.L_DEFAULT, SubActivity.R_DEFAULT);
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
            case CONTACT: // Contact 联系人
                appIcon.setImageResource(R.drawable.menu_contact);
                appName.setText(R.string.menu_contact);
                break;
            case SMS: // SMS 短信
                appIcon.setImageResource(R.drawable.menu_sms);
                appName.setText(R.string.menu_sms);
                break;
            case CAMERA: // Camera 相机
                appIcon.setImageResource(R.drawable.menu_camera);
                appName.setText(R.string.menu_camera);
                break;
            case GALLERY: // Gallery 相册
                appIcon.setImageResource(R.drawable.menu_gallery);
                appName.setText(R.string.menu_gallery);
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
            case CONTACT:
                fragment = ContactsFragment.newInstance();
                break;
            case SMS:
                fragment = MessageFragment.newInstance();
                break;
            case CAMERA:
                fragment = CameraFragment.newInstance();
                break;
            case GALLERY:
                fragment = GalleryFragment.newInstance();
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