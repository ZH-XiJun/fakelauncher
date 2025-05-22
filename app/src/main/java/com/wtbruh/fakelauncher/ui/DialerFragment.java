package com.wtbruh.fakelauncher.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wtbruh.fakelauncher.DialerActivity;
import com.wtbruh.fakelauncher.MainActivity;
import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.SettingsFragment;
import com.wtbruh.fakelauncher.SubActivity;
import com.wtbruh.fakelauncher.utils.MyFragment;
import com.wtbruh.fakelauncher.utils.UIHelper;

public class DialerFragment extends MyFragment {
    private final static String TAG = DialerFragment.class.getSimpleName();
    private SharedPreferences mPrefs;
    private TextView mEditText;
    private View rootView;
    private SubActivity mActivity;

    public DialerFragment() {
        // Required empty public constructor
    }
    public static DialerFragment newInstance() {
        return new DialerFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_dialer, container, false);
        init();
        return rootView;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        String content = mEditText.getText().toString();

        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_POUND) {
            if (content.equals(getString(R.string.dialer_empty))) {
                mActivity.setActionBar(SubActivity.R_EDITTEXT);
                mEditText.setText(UIHelper.textEditor(keyCode, ""));
            } else {
                mEditText.setText(UIHelper.textEditor(keyCode, content));
            }
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            // When there's no chars, right button will be used as "back" key
            // 文本框里没有字，右键应作为返回键
            if (content.equals(getString(R.string.dialer_empty))) {
                return false;
            } else {
                // When there are some chars, right button will be used to delete chars
                // 如果有字，右键应该是删除键
                if (content.length() == 1) {
                    mActivity.setActionBar(SubActivity.R_DEFAULT);
                    mEditText.setText(R.string.dialer_empty);
                } else {
                    mEditText.setText(UIHelper.textEditor(keyCode, content));
                }
            }
        } else if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MENU) {
            String[] valueArray = getResources().getStringArray(R.array.pref_exit_fakeui_method);
            String exitMethod = mPrefs.getString("exit_fakeui_method", valueArray[0]);
            Log.d(TAG,"User set exit method: " + exitMethod);
            if (exitMethod.equals(valueArray[1])) {
                String secretCode = mPrefs.getString(SettingsFragment.PREF_EXIT_FAKEUI_CONFIG, "");
                Log.d(TAG,"passwd:"+secretCode);
                if (!secretCode.isEmpty()){
                    if (mEditText.getText().equals("*#"+secretCode+"#*")) {
                        // todo: do exit code
                    }
                }
            }
        } else {
            return false;
        }
        return true;
    }

    private void init() {
        mEditText = rootView.findViewById(R.id.dialer);
        mActivity = (SubActivity) getActivity();
    }


}