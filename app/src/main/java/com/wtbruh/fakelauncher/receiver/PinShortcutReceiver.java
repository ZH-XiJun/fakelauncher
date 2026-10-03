package com.wtbruh.fakelauncher.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.wtbruh.fakelauncher.R;

/**
 * 快捷方式固定结果回调：用户确认后系统发送广播，Toast 反馈添加结果
 */
public class PinShortcutReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Toast.makeText(context, R.string.pref_shortcut_desktop_done, Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
        }
    }
}
