package com.wtbruh.fakelauncher.ui.fragment.phone;

import android.Manifest;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
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
import com.wtbruh.fakelauncher.ui.view.DualTextviewAdapter;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 短信：会话列表（按号码聚合，显示最后一条消息与时间）。
 * 老人机风格：黑底白字，方向键选择，确认键进入会话。
 */
public class MessageFragment extends BaseFragment {

    private static final String TAG = MessageFragment.class.getSimpleName();

    public static final String URI_HEADER = "content://sms/";
    public static final String PATH_INBOX = "inbox";
    public static final String PATH_SEND = "sent";
    public static final String PATH_DRAFT = "draft";
    public static final String PATH_OUTBOX = "outbox";
    public static final String PATH_FAILED = "failed";
    public static final String PATH_QUEUED = "queued";

    private RecyclerView messageView;
    private DualTextviewAdapter adapter;
    private final List<Bundle> data = new ArrayList<>();
    private final List<SmsThread> threads = new ArrayList<>();

    /** 一条会话：号码 + 联系人名 + 最后一条消息 */
    public static class SmsThread {
        public String address;
        public String name;
        public String body;
        public long date;
        public boolean incoming;
    }

    public MessageFragment() {
        // Required empty public constructor
    }

    public static MessageFragment newInstance() {
        return new MessageFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_message, container, false);
        init();
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadThreads();
    }

    private void init() {
        messageView = rootView.findViewById(R.id.message_list);
        messageView.setLayoutManager(new LinearLayoutManager(requireContext()));
        messageView.setFocusable(false);
        messageView.setItemAnimator(null);
        // 彻底禁用触屏：消费所有触摸事件，列表不可触摸（功能机纯物理键操作）
        messageView.setOnTouchListener((v, e) -> true);
        setFooterBar(L_WRITE, R_DEFAULT);
        reloadThreads();
    }

    private void reloadThreads() {
        threads.clear();
        data.clear();
        if (!PrivilegeProvider.checkPermission(requireContext(), Manifest.permission.READ_SMS)
                || !loadThreads()) {
            noMessage();
            return;
        }
        rootView.findViewById(R.id.message_textHint).setVisibility(View.INVISIBLE);
        messageView.setVisibility(View.VISIBLE);
        for (SmsThread t : threads) {
            Bundle b = new Bundle();
            String title = TextUtils.isEmpty(t.name) ? t.address : t.name + " " + t.address;
            b.putString(DualTextviewAdapter.ITEM, title);
            String preview = (t.incoming ? "" : getString(R.string.sms_out_prefix)) + t.body;
            if (preview.length() > 24) preview = preview.substring(0, 24) + "…";
            b.putString(DualTextviewAdapter.SUB_ITEM, preview + "\n" + formatTime(t.date));
            data.add(b);
        }
        adapter = new DualTextviewAdapter(data);
        messageView.setAdapter(adapter);
        setFooterBar(L_WRITE, R_DEFAULT);
    }

    private void noMessage() {
        rootView.findViewById(R.id.message_textHint).setVisibility(View.VISIBLE);
        messageView.setVisibility(View.INVISIBLE);
        setFooterBar(L_WRITE, R_DEFAULT);
    }

    /** 读取全部短信并按号码聚合为会话（取每个号码最新一条） */
    private boolean loadThreads() {
        Map<String, SmsThread> map = new LinkedHashMap<>();
        // 只同步最近 3 天的短信（每次打开伪装重新加载）
        long threeDaysAgo = System.currentTimeMillis() - 3L * 24 * 3600 * 1000;
        try (Cursor cursor = requireContext().getContentResolver().query(
                Uri.parse("content://sms"), null, "date >= ?",
                new String[]{String.valueOf(threeDaysAgo)}, "date DESC")) {
            if (cursor == null) return false;
            int addrIdx = cursor.getColumnIndex("address");
            int bodyIdx = cursor.getColumnIndex("body");
            int dateIdx = cursor.getColumnIndex("date");
            int typeIdx = cursor.getColumnIndex("type");
            while (cursor.moveToNext()) {
                String address = cursor.getString(addrIdx);
                if (TextUtils.isEmpty(address)) continue;
                // 运营商短信等以数字开头，正常展示
                SmsThread thread = map.get(address);
                if (thread == null) {
                    thread = new SmsThread();
                    thread.address = address;
                    thread.name = lookupContactName(address);
                    map.put(address, thread);
                }
                if (thread.body == null) {
                    thread.body = cursor.getString(bodyIdx);
                    thread.date = cursor.getLong(dateIdx);
                    int type = cursor.getInt(typeIdx);
                    thread.incoming = (type == Telephony.Sms.Inbox.MESSAGE_TYPE_ALL || type == 1);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to read sms", e);
            return false;
        }
        threads.addAll(map.values());
        return !threads.isEmpty();
    }

    /** 根据号码反查联系人显示名 */
    private String lookupContactName(String number) {
        try {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(number));
            try (Cursor c = requireContext().getContentResolver().query(
                    uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    return c.getString(0);
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "lookup contact failed: " + number);
        }
        return "";
    }

    private static String formatTime(long date) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(date));
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            requireSubActivity().fragmentStarter(SmsComposeFragment.newInstance("", ""));
            return true;
        }
        if (adapter == null || adapter.getItemCount() == 0) {
            return false;
        }
        int position = adapter.getSelectedPosition();
        return switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP -> {
                if (position > 0) {
                    messageView.scrollToPosition(position - 1);
                    adapter.setSelectedPosition(position - 1);
                }
                yield true;
            }
            case KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (position < adapter.getItemCount() - 1) {
                    messageView.scrollToPosition(position + 1);
                    adapter.setSelectedPosition(position + 1);
                }
                yield true;
            }
            // 左下角"写信"：按左软键进入发短信界面
            case KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_CALL,
                 KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                // 确定键：查看具体短信
                if (position >= 0 && position < threads.size()) {
                    SmsThread t = threads.get(position);
                    requireSubActivity().fragmentStarter(
                            SmsConversationFragment.newInstance(t.address, t.name));
                }
                yield true;
            }
            default -> false;
        };
    }
}
