package com.wtbruh.fakelauncher.service;

import android.content.Intent;
import android.os.Build;
import android.telecom.Call;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.wtbruh.fakelauncher.IncomingCallActivity;
import com.wtbruh.fakelauncher.receiver.IncomingCallReceiver;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统来电绑定服务（Manifest 声明了 IN_CALL_SERVICE_RINGING）：
 * 来电/通话时由 Telecom 绑定，负责弹出来电显示界面并展示号码。
 * 号码直接取自 Call.Details.getHandle()，比广播 EXTRA_INCOMING_NUMBER 更可靠。
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public class InCallService extends android.telecom.InCallService {

    private static final String TAG = InCallService.class.getSimpleName();

    private final Map<Call, Call.Callback> callbacks = new HashMap<>();

    /** 当前通话引用（供 IncomingCallActivity 直接挂断/忽略）。 */
    public static volatile Call sCurrentCall = null;

    /** 通话状态广播（呼出/呼入共用：FakeInCallFragment 等监听拨号中->通话中->挂断）。 */
    public static final String ACTION_CALL_STATE = "com.wtbruh.fakelauncher.action.CALL_STATE";
    public static final String EXTRA_CALL_STATE = "call_state";

    @Override
    public void onCreate() {
        super.onCreate();
        // 进程重启后 InCallService 重新绑定：同步已经存在的通话引用，避免 sCurrentCall 为 null
        try {
            for (Call call : getCalls()) {
                if (call.getState() != Call.STATE_DISCONNECTED) {
                    sCurrentCall = call;
                    Log.d(TAG, "onCreate sync existing call state=" + call.getState());
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "onCreate sync calls failed", t);
        }
    }

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        Log.d(TAG, "onCallAdded state=" + call.getState());
        sCurrentCall = call;
        // 仅响铃（来电）时弹出来电显示界面；呼出/通话中由拨号界面自行处理
        if (call.getState() == Call.STATE_RINGING) {
            showIncoming(extractNumber(call));
        }

        Call.Callback cb = new Call.Callback() {
            @Override
            public void onStateChanged(Call call, int state) {
                Log.d(TAG, "onStateChanged " + state);
                if (state == Call.STATE_ACTIVE) {
                    // 来电接通：通知来电界面切换到"通话中"
                    notifyActive();
                    broadcastCallState(state);
                } else if (state == Call.STATE_DISCONNECTED) {
                    closeIncoming();
                    broadcastCallState(state);
                } else if (state == Call.STATE_DIALING || state == Call.STATE_CONNECTING) {
                    broadcastCallState(state);
                }
            }

            @Override
            public void onDetailsChanged(Call c, Call.Details details) {
                // 号码可能延迟填充，有值就刷新界面
                String n = extractNumber(c);
                if (n != null && !n.isEmpty()) updateIncoming(n);
            }
        };
        call.registerCallback(cb);
        callbacks.put(call, cb);
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        if (sCurrentCall == call) sCurrentCall = null;
        Call.Callback cb = callbacks.remove(call);
        if (cb != null) {
            try {
                call.unregisterCallback(cb);
            } catch (Exception ignored) { }
        }
        closeIncoming();
    }

    /** 从通话详情提取号码/姓名（优先号码，其次显示名）。 */
    private String extractNumber(Call call) {
        try {
            Call.Details d = call.getDetails();
            if (d.getHandle() != null) {
                String n = d.getHandle().getSchemeSpecificPart();
                if (n != null && !n.isEmpty()) return n;
            }
            String name = d.getCallerDisplayName();
            if (name != null && !name.isEmpty()) return name;
        } catch (Exception e) {
            Log.e(TAG, "extractNumber failed", e);
        }
        return "";
    }

    /** 弹出来电显示界面（InCallService 前台绑定，可正常启动 Activity）。 */
    private void showIncoming(String number) {
        try {
            Intent i = new Intent(this, IncomingCallActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(IncomingCallActivity.EXTRA_NUMBER, number);
            startActivity(i);
        } catch (Exception e) {
            Log.e(TAG, "showIncoming failed", e);
        }
    }

    /** 广播通话状态变化（呼出界面监听：拨号中/通话中/挂断）。 */
    private void broadcastCallState(int state) {
        try {
            Intent i = new Intent(ACTION_CALL_STATE)
                    .setPackage(getPackageName())
                    .putExtra(EXTRA_CALL_STATE, state);
            sendBroadcast(i);
        } catch (Exception e) {
            Log.e(TAG, "broadcastCallState failed", e);
        }
    }

    /** 来电接通（ACTIVE）：广播通知 IncomingCallActivity 切换到通话中界面。 */
    private void notifyActive() {
        try {
            Intent i = new Intent(IncomingCallActivity.ACTION_CALL_ACTIVE)
                    .setPackage(getPackageName());
            sendBroadcast(i);
        } catch (Exception e) {
            Log.e(TAG, "notifyActive failed", e);
        }
    }

    /** 号码变化时更新界面（复用 IncomingCallActivity 的更新广播）。 */
    private void updateIncoming(String number) {
        try {
            Intent i = new Intent(IncomingCallActivity.ACTION_UPDATE_NUMBER)
                    .setPackage(getPackageName())
                    .putExtra(IncomingCallActivity.EXTRA_NUMBER, number);
            sendBroadcast(i);
        } catch (Exception e) {
            Log.e(TAG, "updateIncoming failed", e);
        }
    }

    /** 通话结束：关闭来电显示界面。 */
    private void closeIncoming() {
        try {
            Intent i = new Intent(IncomingCallReceiver.ACTION_CLOSE_INCOMING)
                    .setPackage(getPackageName());
            sendBroadcast(i);
        } catch (Exception e) {
            Log.e(TAG, "closeIncoming failed", e);
        }
    }
}
