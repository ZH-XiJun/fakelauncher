package com.wtbruh.fakelauncher.ui.fragment.phone;

import android.content.SharedPreferences;
import android.os.Bundle;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import com.wtbruh.fakelauncher.MainActivity;
import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.ui.fragment.settings.SubSettingsFragment;
import com.wtbruh.fakelauncher.ui.fragment.BaseFragment;
import com.wtbruh.fakelauncher.utils.UIHelper;

public class DialerFragment extends BaseFragment {
    private final static String TAG = DialerFragment.class.getSimpleName();
    private static final String ARG_INPUT = "input";
    private SharedPreferences mPrefs;
    private TextView mEditText;

    public DialerFragment() {
        // Required empty public constructor
    }
    public static DialerFragment newInstance() {
        return new DialerFragment();
    }

    /**
     * 启动该fragment时传入参数<br>
     * 仅接受字符串数组的第二个数据
     *
     * @param params 字符串数组组成的参数
     * @return 带参数的Fragment
     */
    public static DialerFragment newInstance(String[] params) {
        DialerFragment fragment = new DialerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_INPUT, params[1]);
        fragment.setArguments(args);
        return fragment;
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
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        String content = mEditText.getText().toString();

        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_POUND) {
            if (content.equals(getString(R.string.dialer_empty))) {
                setFooterBar(R_EDITTEXT);
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
                    setFooterBar(R_DEFAULT);
                    mEditText.setText(R.string.dialer_empty);
                } else {
                    mEditText.setText(UIHelper.textEditor(keyCode, content));
                }
            }
        } else if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MENU) {
            if (UIHelper.checkExitMethod(getActivity(), 1)) {
                Log.d(TAG,"User set dialer for exit method");
                String secretCode = mPrefs.getString(SubSettingsFragment.PREF_EXIT_FAKEUI_CONFIG, "");
                if (!secretCode.isEmpty()){
                    if (mEditText.getText().equals("*#"+secretCode+"#*")) {
                        Log.d(TAG,"secret code correct!!!");
                        UIHelper.intentStarter(getActivity(), MainActivity.class);
                    }
                }
                Log.d(TAG,"secret code incorrect or user didn't set secret code");
            }
        } else {
            return false;
        }
        return true;
    }

    private void init() {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        mEditText = rootView.findViewById(R.id.dialer);
        if (getArguments() != null) {
            mEditText.setText(getArguments().getString(ARG_INPUT));
            setFooterBar(R_EDITTEXT);
        }

    }


}