package com.wtbruh.fakelauncher.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.wtbruh.fakelauncher.MainActivity;

/**
 * Power connection status receiver<br>
 * 获取电源连接状态相关广播的广播接收器
 */

public class PowerConnectionReceiver extends BroadcastReceiver {

    private getStat stat;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            return;
        }
        MainActivity a = new MainActivity();
        stat.getConnectionStatus(action);
    }

    public interface getStat {
        void getConnectionStatus(String str);
    }

    public void setStat (getStat str) {
        this.stat = str;
    }
}
