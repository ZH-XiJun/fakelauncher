package com.wtbruh.fakelauncher.xposed;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.wtbruh.fakelauncher.utils.HookHelper;

public class SelfHook extends HookHelper {
    public static int taskId;
    String TAG = PinningHook.class.getSimpleName();
    Context context;
    @Override
    public void init() {
        // Get task id and do changes to setting "fakelauncher_pinmode"
        findAndHookMethod("com.wtbruh.fakelauncher.MainActivity", "onCreate", Bundle.class, new HookAction() {
            @Override
            protected void after(MethodHookParam param) {
                super.after(param);
                taskId = (int) callMethod(param.thisObject, "taskId");
                Log.d(TAG, "Got task id: "+String.format("%d",taskId));
                if (PinningHook.CONTEXT != null) Log.d(TAG, "Got context!"); else Log.d(TAG, "Context is null!");
            }
        });
        findAndHookMethod("com.wtbruh.fakelauncher.MainActivity", "onDestroy", new HookAction() {
            @Override
            protected void after(MethodHookParam param) {
                super.after(param);

            }
        });
    }
}
