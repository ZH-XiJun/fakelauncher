package com.wtbruh.fakelauncher;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.wtbruh.fakelauncher.utils.MyAppCompatActivity;
import com.wtbruh.fakelauncher.utils.UIHelper;

public class DialerActivity extends MyAppCompatActivity {

    private TextView mRightButton;
    private TextView mEditText;

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
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        mEditText = findViewById(R.id.dialer);
        String content = mEditText.getText().toString();

        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_POUND) {
            if (content.equals(getString(R.string.dialer_empty))) {
                mRightButton = findViewById(R.id.dialer_rightButton);
                mRightButton.setText(R.string.edittext_rightbutton);
                mEditText.setText(UIHelper.textEditor(keyCode, ""));
            } else {
                mEditText.setText(UIHelper.textEditor(keyCode, content));
            }
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            // When there's no chars, right button will be used as "back" key
            // 文本框里没有字，右键应作为返回键
            if (content.equals(getString(R.string.dialer_empty))) {
                super.onKeyUp(keyCode, event);
            } else {
                // When there are some chars, right button will be used to delete chars
                // 如果有字，右键应该是删除键
                if (content.length() == 1) {
                    mRightButton = findViewById(R.id.dialer_rightButton);
                    mRightButton.setText(R.string.common_rightbutton);
                    mEditText.setText(R.string.dialer_empty);
                } else {
                    mEditText.setText(UIHelper.textEditor(keyCode, content));
                }
            }
        } else if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MENU) {
            // maybe do something?
        } else {
            return super.onKeyUp(keyCode, event);
        }
        return true;
    }

    private void getExtra () {
        Intent intent = getIntent();
        String data = intent.getStringExtra("key");
        if (data == null) {
            return;
        }
        mEditText = findViewById(R.id.dialer);
        mEditText.setText(data);
        mRightButton = findViewById(R.id.dialer_rightButton);
        mRightButton.setText(R.string.edittext_rightbutton);
    }
}