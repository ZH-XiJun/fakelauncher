package com.wtbruh.fakelauncher.ui.fragment.phone;

import android.Manifest;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.ui.fragment.BaseFragment;
import com.wtbruh.fakelauncher.ui.view.SmsMessageAdapter;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 短信会话详情：展示与某号码的全部往来短信。
 * 左键（菜单键）或确认键进入“回复”编辑界面。
 */
public class SmsConversationFragment extends BaseFragment {

    private static final String TAG = SmsConversationFragment.class.getSimpleName();
    private static final String ARG_ADDRESS = "address";
    private static final String ARG_NAME = "name";

    private RecyclerView msgView;
    private SmsMessageAdapter adapter;
    private final List<String> messages = new ArrayList<>();
    private String address = "";
    private String name = "";

    public SmsConversationFragment() {
        // Required empty public constructor
    }

    public static SmsConversationFragment newInstance(String address, String name) {
        SmsConversationFragment fragment = new SmsConversationFragment();
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
        Log.d(TAG, "onCreateView start");
        rootView = inflater.inflate(R.layout.fragment_sms_conversation, container, false);
        init();
        Log.d(TAG, "onCreateView done");
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMessages();
    }

    private void init() {
        msgView = rootView.findViewById(R.id.sms_conv_list);
        msgView.setLayoutManager(new LinearLayoutManager(requireContext()));
        msgView.setFocusable(false);
        msgView.setItemAnimator(null);
        // 彻底禁用触屏：消费所有触摸事件，会话不可触摸
        msgView.setOnTouchListener((v, e) -> true);
        String title = TextUtils.isEmpty(name) ? address : name;
        ((android.widget.TextView) rootView.findViewById(R.id.sms_conv_title)).setText(title);
        loadMessages();
        setFooterBar(L_REPLY, R_DEFAULT);
    }

    private void loadMessages() {
        Log.d(TAG, "loadMessages start address=" + address);
        messages.clear();
        if (TextUtils.isEmpty(address)
                || !PrivilegeProvider.checkPermission(requireContext(), Manifest.permission.READ_SMS)) {
            noMessage();
            return;
        }
        // 读取与该号码近 3 天的收/发短信，按时间升序（与列表同步保持一致）
        long threeDaysAgo = System.currentTimeMillis() - 3L * 24 * 3600 * 1000;
        try (Cursor cursor = requireContext().getContentResolver().query(
                Uri.parse("content://sms"), null, "address=? AND date >= ?",
                new String[]{address, String.valueOf(threeDaysAgo)}, "date ASC")) {
            if (cursor == null) {
                noMessage();
                return;
            }
            int bodyIdx = cursor.getColumnIndex("body");
            int dateIdx = cursor.getColumnIndex("date");
            int typeIdx = cursor.getColumnIndex("type");
            while (cursor.moveToNext()) {
                String body = cursor.getString(bodyIdx);
                if (body == null) body = "";
                int type = cursor.getInt(typeIdx);
                boolean incoming = (type == Telephony.Sms.Inbox.MESSAGE_TYPE_ALL || type == 1);
                String who = incoming ? getString(R.string.sms_in_prefix) : getString(R.string.sms_out_prefix);
                messages.add(who + body + "\n" + formatTime(cursor.getLong(dateIdx)));
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to read conversation", e);
            noMessage();
            return;
        }
        if (messages.isEmpty()) {
            noMessage();
            return;
        }
        rootView.findViewById(R.id.sms_conv_textHint).setVisibility(View.INVISIBLE);
        msgView.setVisibility(View.VISIBLE);
        Log.d(TAG, "loadMessages loaded count=" + messages.size());
        adapter = new SmsMessageAdapter(messages);
        msgView.setAdapter(adapter);
        // 自动滚到底部（最新消息）
        msgView.scrollToPosition(adapter.getItemCount() - 1);
        adapter.setSelectedPosition(adapter.getItemCount() - 1);
    }

    private void noMessage() {
        rootView.findViewById(R.id.sms_conv_textHint).setVisibility(View.VISIBLE);
        msgView.setVisibility(View.INVISIBLE);
    }

    private static String formatTime(long date) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(date));
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                // 回复
                requireSubActivity().fragmentStarter(
                        SmsComposeFragment.newInstance(address, name));
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                // 向上滚动当前短信内容（不切换条目），长短信可完整查看
                if (adapter != null) msgView.scrollBy(0, -40);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                // 向下滚动当前短信内容
                if (adapter != null) msgView.scrollBy(0, 40);
                return true;
            case KeyEvent.KEYCODE_BACK:
                return false;
            default:
                return false;
        }
    }
}
