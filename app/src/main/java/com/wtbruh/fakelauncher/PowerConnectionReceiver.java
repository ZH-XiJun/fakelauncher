package com.wtbruh.fakelauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

public class PowerConnectionReceiver extends BroadcastReceiver {

    private getstat stat;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            return;
        }
        MainActivity a = new MainActivity();
        stat.getConnectionStatus(action);
    }

    interface getstat {
        public void getConnectionStatus (String str);
    }

    public void setstat (getstat str) {
        this.stat = str;
    }
}
