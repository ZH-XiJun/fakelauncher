package com.wtbruh.fakelauncher.receiver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.wtbruh.fakelauncher.ApplicationHelper;
import com.wtbruh.fakelauncher.IncomingCallActivity;
import com.wtbruh.fakelauncher.R;

/**
 * 来电监听：电话呼入（RINGING）时通过「全屏来电通知」自动亮屏并弹出老人机风格的来电显示界面。
 * 用全屏通知而非直接 startActivity：Android 10+ 对后台启动 Activity 有限制，
 * 而全屏通知（category=CALL）的 Activity 启动由系统代办，来电场景不受限。
 */
public class IncomingCallReceiver extends BroadcastReceiver {

    private static final String TAG = IncomingCallReceiver.class.getSimpleName();

    private static final String CHANNEL_ID = "incoming_call";
    private static final int NOTIFY_ID = 20260926;

    /** 通话结束（IDLE）时发送，用于关闭来电显示界面 */
    public static final String ACTION_CLOSE_INCOMING =
            "com.wtbruh.fakelauncher.action.CLOSE_INCOMING";

    /** PhoneStateListener 兜底带来的来电号码（广播 extra 为空时用） */
    public static final String ACTION_INCOMING_NUMBER =
            "com.wtbruh.fakelauncher.action.INCOMING_NUMBER";
    public static final String EXTRA_NUMBER = "number";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_INCOMING_NUMBER.equals(action)) {
            // 第二通道：号码已拿到，若界面在显示则更新，否则启动
            String number = intent.getStringExtra(EXTRA_NUMBER);
            String top = ApplicationHelper.topActivity;
            if (top != null && top.contains(IncomingCallActivity.class.getSimpleName())) {
                context.sendBroadcast(new Intent(IncomingCallActivity.ACTION_UPDATE_NUMBER)
                        .putExtra(IncomingCallActivity.EXTRA_NUMBER, number)
                        .setPackage(context.getPackageName()));
            }
            return;
        }
        if (!TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) return;

        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            Log.d(TAG, "Incoming call from: " + number);

            // 来电界面已在顶部则直接更新，避免重复启动
            String top = ApplicationHelper.topActivity;
            if (top != null && top.contains(IncomingCallActivity.class.getSimpleName())) {
                context.sendBroadcast(new Intent(IncomingCallActivity.ACTION_UPDATE_NUMBER)
                        .putExtra(IncomingCallActivity.EXTRA_NUMBER, number)
                        .setPackage(context.getPackageName()));
                return;
            }

            // 优先直接启动：持有悬浮窗权限时后台启动 Activity 不受系统限制（屏幕亮着也能弹出）
            boolean overlayGranted = android.provider.Settings.canDrawOverlays(context);
            Log.d(TAG, "overlay permission granted: " + overlayGranted);
            if (overlayGranted) {
                try {
                    Intent i = new Intent(context, IncomingCallActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            .putExtra(IncomingCallActivity.EXTRA_NUMBER, number);
                    context.startActivity(i);
                } catch (Exception e) {
                    Log.e(TAG, "direct start failed, fallback to fullscreen notification", e);
                    showFullScreenIncoming(context, number);
                }
            } else {
                // 无悬浮窗权限：走全屏通知（息屏/锁屏来电时系统会自动全屏弹出）
                showFullScreenIncoming(context, number);
            }
        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            // 来电结束：关闭来电显示界面并移除通知
            Log.d(TAG, "Call ended, closing incoming UI");
            try {
                NotificationManager nm = (NotificationManager)
                        context.getSystemService(Context.NOTIFICATION_SERVICE);
                nm.cancel(NOTIFY_ID);
            } catch (Exception ignored) { }
            context.sendBroadcast(new Intent(ACTION_CLOSE_INCOMING).setPackage(context.getPackageName()));
        }
    }

    /** 发全屏来电通知：系统代为启动 IncomingCallActivity（锁屏/后台均显示）。 */
    private void showFullScreenIncoming(Context context, String number) {
        try {
            NotificationManager nm = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID, "来电显示",
                        NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("电话呼入时全屏显示来电号码");
                channel.enableVibration(true);
                nm.createNotificationChannel(channel);
            }

            Intent fullScreen = new Intent(context, IncomingCallActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(IncomingCallActivity.EXTRA_NUMBER, number);
            PendingIntent pi = PendingIntent.getActivity(
                    context, 0, fullScreen,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            Notification n = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_menu_call)
                    .setContentTitle(context.getString(R.string.incoming_call_title))
                    .setContentText(number)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setFullScreenIntent(pi, true)
                    .setAutoCancel(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .build();
            nm.notify(NOTIFY_ID, n);
            Log.d(TAG, "full-screen incoming notification posted");
        } catch (Exception e) {
            Log.e(TAG, "showFullScreenIncoming failed", e);
            // 兜底：直接尝试启动（部分系统允许来电场景后台启动）
            try {
                Intent i = new Intent(context, IncomingCallActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        .putExtra(IncomingCallActivity.EXTRA_NUMBER, number);
                context.startActivity(i);
            } catch (Exception ignored2) { }
        }
    }
}
