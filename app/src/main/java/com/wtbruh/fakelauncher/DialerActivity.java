package com.wtbruh.fakelauncher;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.wtbruh.fakelauncher.utils.InputHelper;

public class DialerActivity extends AppCompatActivity {

    TextView rightbutton;
    TextView editText;
    InputHelper input = new InputHelper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dialer);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        getExtra();
    }

    @Override
    protected void onPause() {
        // Disable transition anim
        // 去掉过渡动画
        overridePendingTransition(0, 0);
        super.onPause();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        editText = findViewById(R.id.dialer);
        String content = editText.getText().toString();

        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_POUND) {
            if (content.equals(getString(R.string.dialer_empty))) {
                rightbutton = findViewById(R.id.dialer_rightButton);
                rightbutton.setText(R.string.edittext_rightbutton);
                editText.setText(input.textEditor(keyCode, ""));
            } else {
                editText.setText(input.textEditor(keyCode, content));
            }
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            // When there's no chars, right button will be used as "back" key
            // 文本框里没有字，右键应作为返回键
            if (content.equals(getString(R.string.dialer_empty))) {
                super.onKeyUp(keyCode, event);
            } else {
                // When there're some chars, right button will be used to delete chars
                // 如果有字，右键应该是删除键
                if (content.length() == 1) {
                    rightbutton = findViewById(R.id.dialer_rightButton);
                    rightbutton.setText(R.string.common_rightbutton);
                    editText.setText(R.string.dialer_empty);
                } else {
                    editText.setText(input.textEditor(keyCode, content));
                }
            }
        } else if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MENU) {
            // passwordCheck(content);
        } else {
            super.onKeyUp(keyCode, event);
        }
        return true;
    }
    void getExtra () {
        Intent intent = getIntent();
        String data = intent.getStringExtra("key");
        if (data == null) {
            return;
        }
        editText = findViewById(R.id.dialer);
        editText.setText(data);
    }
}