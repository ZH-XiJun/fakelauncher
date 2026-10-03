package com.wtbruh.fakelauncher.ui.fragment.phone;

import android.Manifest;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.ui.fragment.BaseFragment;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;
import com.wtbruh.fakelauncher.utils.UIHelper;

/**
 * 短信编辑发送：向指定号码发送短信。
 * 支持触摸弹出系统输入法；也支持方向键 + 确认键发送、返回键逐字删除。
 */
public class SmsComposeFragment extends BaseFragment {

    private static final String TAG = SmsComposeFragment.class.getSimpleName();
    private static final String ARG_ADDRESS = "address";
    private static final String ARG_NAME = "name";

    private EditText inputView;
    private String address = "";
    private String name = "";

    public SmsComposeFragment() {
        // Required empty public constructor
    }

    public static SmsComposeFragment newInstance(String address, String name) {
        SmsComposeFragment fragment = new SmsComposeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ADDRESS, address);
        args.putString(ARG_NAME, name);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            address = getArguments().getString(ARG_ADDRESS, "");
            name = getArguments().getString(ARG_NAME, "");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_sms_compose, container, false);
        init();
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        setFooterBar(L_SAVE, R_EDITTEXT);
    }

    private void init() {
        TextView titleView = rootView.findViewById(R.id.sms_compose_title);
        String title = TextUtils.isEmpty(name) ? address : name;
        if (!TextUtils.isEmpty(name)) title = name + " " + address;
        titleView.setText(title);

        inputView = rootView.findViewById(R.id.sms_compose_input);
        // 支持触摸输入法；默认聚焦，点击可弹出键盘
        inputView.setFocusable(true);
        inputView.setFocusableInTouchMode(true);
        inputView.requestFocus();
        setFooterBar(L_SAVE, R_EDITTEXT);
    }

    private void send() {
        String body = inputView.getText().toString().trim();
        if (TextUtils.isEmpty(body)) {
            UIHelper.showCustomDialog(requireContext(), R.string.sms_empty_body, null);
            return;
        }
        if (TextUtils.isEmpty(address)) {
            UIHelper.showCustomDialog(requireContext(), R.string.sms_no_address, null);
            return;
        }
        if (!PrivilegeProvider.checkPermission(requireContext(), Manifest.permission.SEND_SMS)) {
            UIHelper.showCustomDialog(requireContext(), R.string.sms_no_permission, null);
            return;
        }
        try {
            SmsManager smsManager = SmsManager.getDefault();
            // 长短信自动拆分发送
            if (body.length() > 70) {
                smsManager.sendMultipartTextMessage(address, null,
                        smsManager.divideMessage(body), null, null);
            } else {
                smsManager.sendTextMessage(address, null, body, null, null);
            }
            insertSentRecord(body);
            UIHelper.showCustomDialog(requireContext(), R.string.sms_sent, null)
                    .setOnDismissListener(dialogInterface -> requireActivity().onBackPressed());
        } catch (Exception e) {
            Log.e(TAG, "send sms failed", e);
            UIHelper.showCustomDialog(requireContext(), R.string.sms_send_fail, null);
        }
    }

    /** 尝试把发送记录写入系统短信库（需要 WRITE_SMS，无权限则跳过） */
    private void insertSentRecord(String body) {
        try {
            if (!PrivilegeProvider.checkPermission(requireContext(), "android.permission.WRITE_SMS")) return;
            ContentValues values = new ContentValues();
            values.put("address", address);
            values.put("body", body);
            values.put("date", System.currentTimeMillis());
            values.put("read", 1);
            values.put("type", 2); // SENT
            requireContext().getContentResolver().insert(Uri.parse("content://sms/sent"), values);
        } catch (Exception e) {
            Log.d(TAG, "insert sent record failed", e);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                send();
                return true;
            case KeyEvent.KEYCODE_BACK:
                String content = inputView.getText().toString();
                if (content.isEmpty()) return false;
                inputView.setText(content.substring(0, content.length() - 1));
                inputView.setSelection(inputView.length());
                return true;
            default:
                return false;
        }
    }
}
