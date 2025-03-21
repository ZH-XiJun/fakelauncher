package com.wtbruh.fakelauncher;

import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MenuActivity extends AppCompatActivity {

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

    @Override
    protected void onPause() {
        // Disable transition anim
        overridePendingTransition(0,0);
        super.onPause();
    }

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
//            case KeyEvent.KEYCODE_ENTER:
//                Intent intent = new Intent();
//                intent.setClass(MenuActivity.this, MenuActivity.class);
//                startActivity(intent);
//                // Disable transition anim
//                MenuActivity.this.overridePendingTransition(0,0);
            default:
        }
        super.onKeyUp(keyCode, event);
        return true;
    }

    void switchSection(int number) {
        appicon = findViewById(R.id.appIcon);
        appname = findViewById(R.id.appName);
        switch (number) {
            case 0:
                appicon.setImageResource(R.drawable.menu_call);
                appname.setText(R.string.menu_call);
                break;
            case 1:
                appicon.setImageResource(R.drawable.menu_camera);
                appname.setText(R.string.menu_camera);
                break;
            case 2:
                appicon.setImageResource(R.drawable.menu_contact);
                appname.setText(R.string.menu_contact);
                break;
            case 3:
                appicon.setImageResource(R.drawable.menu_sms);
                appname.setText(R.string.menu_sms);
                break;
            case 4:
                appicon.setImageResource(R.drawable.menu_set);
                appname.setText(R.string.menu_set);
                break;
        }
    }

}