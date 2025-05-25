package com.wtbruh.fakelauncher;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import com.wtbruh.fakelauncher.utils.MyAppCompatActivity;
import com.wtbruh.fakelauncher.utils.UIHelper;

public class PasswordActivity extends MyAppCompatActivity {

    public final static String TAG = PasswordActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.Password), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
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
                // When there are some chars, right button will be used to delete chars
                // 如果有字，右键应该是删除键
                editText.setText(UIHelper.textEditor(keyCode, content));
                if (content.length() == 1) {
                    rightButton = findViewById(R.id.password_rightButton);
                    rightButton.setText(R.string.common_rightbutton);
                }
            }
        } else if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            passwordCheck(content);
        } else {
            super.onKeyUp(keyCode, event);
        }
        return true;
    }

    /**
     * Password check | 检查输入的密码
     *
     * @param passwd 密码
     */
    private void passwordCheck (String passwd) {
        TextView error = findViewById(R.id.passwdError);
        if (passwd.equals("5418814250")) {
            error.setVisibility(View.INVISIBLE);
            UIHelper.intentStarter(PasswordActivity.this, SettingsActivity.class);
            finish();
        } else if (passwd.isEmpty()) {
            error.setText(R.string.password_empty);
            error.setVisibility(View.VISIBLE);
        } else if (UIHelper.checkExitMethod(this, 2)) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            if (passwd.equals(pref.getString(SettingsFragment.PREF_EXIT_FAKEUI_CONFIG, ""))) {
                Log.d(TAG,"password correct!!!");
                UIHelper.intentStarter(PasswordActivity.this, MainActivity.class);
            }
        } else {
            error.setText(R.string.password_wrong);
            error.setVisibility(View.VISIBLE);
        }
    }
}