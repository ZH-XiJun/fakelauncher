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

    int number = 0;
    ImageView appicon;
    TextView appname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_menu);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.Main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        switchSection(number);
    }

    // implement of app switching, using var "number" as index
    // 应用切换实现，以变量number作为索引
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (number == 0) {
                    number = 4;
                } else {
                    number--;
                }
                switchSection(number);
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (number == 4) {
                    number = 0;
                } else {
                    number++;
                }
                switchSection(number);
                break;
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_ENTER:
                startApp(number);
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
        appicon = findViewById(R.id.appIcon);
        appname = findViewById(R.id.appName);
        switch (number) {
            case 0: // Call 电话
                appicon.setImageResource(R.drawable.menu_call);
                appname.setText(R.string.menu_call);
                break;
            case 1: // Camera 相机
                appicon.setImageResource(R.drawable.menu_camera);
                appname.setText(R.string.menu_camera);
                break;
            case 2: // Contact 联系人
                appicon.setImageResource(R.drawable.menu_contact);
                appname.setText(R.string.menu_contact);
                break;
            case 3: // SMS 短信
                appicon.setImageResource(R.drawable.menu_sms);
                appname.setText(R.string.menu_sms);
                break;
            case 4: // Settings 设置
                appicon.setImageResource(R.drawable.menu_set);
                appname.setText(R.string.menu_set);
                break;
        }
    }
    // Launch the corresponding activity using the var "number"
    // 通过number启动对应的Activity
    private void startApp (int number) {
        Class<?> clazz = null;
        switch (number) {
            case 0:
                clazz = DialerActivity.class;
                break;
            // work in progress...
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                clazz = PasswordActivity.class;
                break;
        }
        if (clazz == null) {
            return;
        }
        UIHelper.intentStarter(MenuActivity.this, clazz);
    }

}