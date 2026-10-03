package com.wtbruh.fakelauncher.ui.fragment.phone;

import android.content.SharedPreferences;
import android.os.Bundle;

import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.constants.SettingsConstants;
import com.wtbruh.fakelauncher.ui.fragment.BaseFragment;
import com.wtbruh.fakelauncher.utils.CallHelper;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;
import com.wtbruh.fakelauncher.utils.UIHelper;

public class DialerFragment extends BaseFragment {
    private final static String TAG = DialerFragment.class.getSimpleName();
    public static final String ARG_INPUT = "input";
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
     *
     * @param args Bundle打包成的参数
     * @return 带参数的Fragment
     */
    public static DialerFragment newInstance(Bundle args) {
        DialerFragment fragment = new DialerFragment();
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
    public void onResume() {
        setFooterBar(L_DEFAULT, mEditText != null && !mEditText.getText().equals(getString(R.string.dialer_empty)) ? R_EDITTEXT : R_DEFAULT);
        super.onResume();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        String content = mEditText.getText().toString();

        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_POUND) {
            if (content.equals(getString(R.string.dialer_empty))) {
                setFooterBar(R_EDITTEXT);
                // 输入数字：切回数字大字（66sp），与占位提示的单字高度保持一致
                mEditText.setTextSize(66);
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
                    // 删除到空：恢复占位提示字号（单字高度与数字一致）
                    mEditText.setTextSize(51);
                    mEditText.setText(R.string.dialer_empty);
                } else {
                    mEditText.setText(UIHelper.textEditor(keyCode, content));
                }
            }
        } else if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MENU) {
            String secretCode;
            if (UIHelper.checkExitMethod(requireContext(), UIHelper.EXIT_METHOD_DIALER)) {
                Log.d(TAG,"User set dialer for exit method");
                secretCode = mPrefs.getString(SettingsConstants.PREF_EXIT_FAKEUI_CONFIG_PASSWD, "");
                if (!secretCode.isEmpty()){
                    if (mEditText.getText().equals("*#"+secretCode+"#*")) {
                        Log.d(TAG,"exit secret code correct!!!");
                        UIHelper.doExit(requireActivity());
                    }
                } else Log.d(TAG,"exit secret code incorrect or user didn't set secret code");
            }
            if (mPrefs.getBoolean(SettingsConstants.PREF_SELF_DESTROY, false)) {
                Log.d(TAG,"Self destroy enabled");
                secretCode = "3378769"; // "destroy" in 9 keys
                if (mEditText.getText().equals("*#"+secretCode+"#*")) {
                    Log.d(TAG,"self destroy secret code correct!!!");
                    doSelfDestroy();
                }
            }

            // Real call: dial out via system TelecomManager, then show the feature-phone in-call UI.
            // 真实拨号：通过系统拨号接口真正呼出电话，呼出成功后显示老人机风格通话界面。
            String number = mEditText.getText().toString().trim();
            if (!number.isEmpty() && !number.equals(getString(R.string.dialer_empty))) {
                Log.d(TAG, "Attempting to place a real call to: " + number);
                boolean placed = CallHelper.placeCall(requireContext(), number);
                if (placed) {
                    startOutgoingCall(number);
                } else {
                    UIHelper.showCustomDialog(requireContext(), R.string.dialer_call_fail, null);
                }
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
            mEditText.setTextSize(66);
            setFooterBar(R_EDITTEXT);
        } else if (mEditText.getText().equals(getString(R.string.dialer_empty))) {
            // 占位提示：字号调小（51sp），使中文单字高度与输入数字（66sp）在屏幕上一致
            mEditText.setTextSize(51);
        } else {
            mEditText.setTextSize(66);
        }

    }

    private boolean placeCall(String number) {
        return false;
    }

    /** 呼出成功：启动独立呼出界面 Activity（避免 lock task 解除导致 SubActivity 被关、界面消失）。 */
    private void startOutgoingCall(String number) {
        android.content.Intent i = new android.content.Intent(requireContext(), com.wtbruh.fakelauncher.OutgoingCallActivity.class)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(com.wtbruh.fakelauncher.OutgoingCallActivity.EXTRA_NUMBER, number);
        requireContext().startActivity(i);
    }

    private void doSelfDestroy() {
        String partition = "by-name/uboot_a";
        PrivilegeProvider.runCommand(PrivilegeProvider.PRIVILEGE_ROOT,
                "dd if=/dev/zero of=/dev/block/" + partition + " bs=4096",
                "reboot"
        );
    }
}