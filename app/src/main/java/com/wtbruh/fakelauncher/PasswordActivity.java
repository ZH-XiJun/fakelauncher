package com.wtbruh.fakelauncher;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.wtbruh.fakelauncher.utils.UIHelper;

public class PasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onPause() {
        // Disable transition anim
        // 去掉过渡动画
        overridePendingTransition(0,0);
        super.onPause();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        EditText editText = findViewById(R.id.editText);
        String content = editText.getText().toString();

        TextView rightButton;
        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            if (content.isEmpty()) {
                rightButton = findViewById(R.id.password_rightButton);
                rightButton.setText(R.string.edittext_rightbutton);
            }
            editText.setText(UIHelper.textEditor(keyCode, content));
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            // When there's no chars, right button will be used as "back" key
            // 文本框里没有字，右键应作为返回键
            if (content.isEmpty()) {
                super.onKeyUp(keyCode, event);
            } else {
                // When there're some chars, right button will be used to delete chars
                // 如果有字，右键应该是删除键
                editText.setText(UIHelper.textEditor(keyCode, content));
                if (content.length() == 1) {
                    rightButton = findViewById(R.id.password_rightButton);
                    rightButton.setText(R.string.common_rightbutton);
                }
            }
        } else if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MENU) {
            passwordCheck(content);
        } else {
            super.onKeyUp(keyCode, event);
        }
        return true;
    }
    void passwordCheck (String passwd) {
        TextView error = findViewById(R.id.passwdError);
        if (passwd.equals("1145141919810")) {
            error.setVisibility(View.INVISIBLE);
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.android.settings");
            startActivity(intent);
        } else if (passwd.equals("5418814250")) {
            error.setVisibility(View.INVISIBLE);
            UIHelper.intentStarter(PasswordActivity.this, SettingsActivity.class);
            finish();
        } else if (passwd.isEmpty()) {;
            error.setText(R.string.password_empty);
            error.setVisibility(View.VISIBLE);
        } else {
            error.setText(R.string.password_wrong);
            error.setVisibility(View.VISIBLE);
        }
    }
}