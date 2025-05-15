package com.wtbruh.fakelauncher;

import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.wtbruh.fakelauncher.utils.MyAppCompatActivity;
import com.wtbruh.fakelauncher.utils.UIHelper;

public class MenuActivity extends MyAppCompatActivity {
    private int mNumber = 0;

    public final static int CALL = 0;
    public final static int CAMERA = 1;
    public final static int CONTACT = 2;
    public final static int SMS = 3;
    public final static int SETTINGS = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_menu);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.Menu), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        switchSection(mNumber);
    }

    // implement of app switching, using var "number" as index
    // 应用切换实现，以变量number作为索引
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
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
        super.onKeyUp(keyCode, event);
        return true;
    }

    /**
     * Display the corresponding icon and name when switching<br>
     * 切换时显示对应的图标和名字
     *
     * @param number 第几个app
     */
    private void switchSection (int number) {
        ImageView appIcon = findViewById(R.id.appIcon);
        TextView appName = findViewById(R.id.appName);
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
        Class<?> clazz = null;
        switch (number) {
            case CALL:
                clazz = DialerActivity.class;
                break;
            // work in progress...
            case CAMERA:
                clazz = CameraActivity.class;
                break;
            case CONTACT:
                break;
            case SMS:
                break;
            case SETTINGS:
                clazz = PasswordActivity.class;
                break;
        }
        if (clazz == null) {
            return;
        }
        UIHelper.intentStarter(MenuActivity.this, clazz);
    }

}