package com.wtbruh.fakelauncher.xposed;

import android.util.Log;

import com.wtbruh.fakelauncher.utils.HookHelper;

public class SelfHook extends HookHelper {
    public static int taskId;
    String TAG = PinningHook.class.getSimpleName();
    @Override
    public void init() {
        // Get task id and do changes to setting "fakelauncher_pinmode"
        findAndHookMethod("com.wtbruh.fakelauncher.MainActivity", "onCreate", new HookAction() {
            @Override
            protected void after(MethodHookParam param) {
                super.after(param);
                Log.d(TAG, "testTEST");
                taskId = (int) callMethod(param.thisObject, "getTaskID");
            }
        });
    }
}
