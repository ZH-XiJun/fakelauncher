package com.wtbruh.fakelauncher.ui.fragment.phone;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.SettingsActivity;
import com.wtbruh.fakelauncher.ui.fragment.settings.SubSettingsFragment;
import com.wtbruh.fakelauncher.ui.fragment.BaseFragment;
import com.wtbruh.fakelauncher.utils.UIHelper;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PasswordFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PasswordFragment extends BaseFragment {
    private final static String TAG = PasswordFragment.class.getSimpleName();

    public PasswordFragment() {
        // Required empty public constructor
    }

    public static PasswordFragment newInstance() {
        return new PasswordFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_password, container, false);
        return rootView;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        EditText editText = rootView.findViewById(R.id.editText);
        String content = editText.getText().toString();

        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            if (content.isEmpty()) {
                setFooterBar(R_EDITTEXT);
            }
            editText.setText(UIHelper.textEditor(keyCode, content));
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            // When there's no chars, right button will be used as "back" key
            // 文本框里没有字，右键应作为返回键
            if (content.isEmpty()) {
                return false;
            } else {
                // When there are some chars, right button will be used to delete chars
                // 如果有字，右键应该是删除键
                editText.setText(UIHelper.textEditor(keyCode, content));
                if (content.length() == 1) {
                    setFooterBar(R_DEFAULT);
                }
            }
        } else if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            passwordCheck(content);
        } else {
            return false;
        }
        return true;
    }

    /**
     * Password check | 检查输入的密码
     *
     * @param passwd 密码
     */
    private void passwordCheck(String passwd) {
        TextView error = rootView.findViewById(R.id.passwdError);
        if (passwd.equals("5418814250")) {
            error.setVisibility(View.INVISIBLE);
            UIHelper.intentStarter(requireActivity(), SettingsActivity.class);
        } else if (passwd.isEmpty()) {
            error.setText(R.string.password_empty);
            error.setVisibility(View.VISIBLE);
        } else if (UIHelper.checkExitMethod(requireContext(), 2)) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(requireContext());
            if (passwd.equals(pref.getString(SubSettingsFragment.PREF_EXIT_FAKEUI_CONFIG, ""))) {
                Log.d(TAG,"password correct!!!");
                UIHelper.doExit(requireActivity());
            } else {
                error.setText(R.string.password_wrong);
                error.setVisibility(View.VISIBLE);
            }
        } else {
            error.setText(R.string.password_wrong);
            error.setVisibility(View.VISIBLE);
        }
    }
}