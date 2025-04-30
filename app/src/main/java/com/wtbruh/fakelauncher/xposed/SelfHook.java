package com.wtbruh.fakelauncher.xposed;

import com.wtbruh.fakelauncher.utils.HookHelper;

public class SelfHook extends HookHelper {
    private final static String TAG = SelfHook.class.getSimpleName();

    @Override
    public void init() {
        // Hook myself if I'm activated
        findAndHookMethod("com.wtbruh.fakelauncher.MainActivity", "isXposedModuleActivated", new HookAction() {
            @Override
            protected void before(MethodHookParam param) {
                super.before(param);
                param.setResult(true);
            }
        });
    }
}
