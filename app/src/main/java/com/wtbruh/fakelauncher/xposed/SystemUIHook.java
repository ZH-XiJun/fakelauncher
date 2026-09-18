package com.wtbruh.fakelauncher.xposed;

import com.wtbruh.fakelauncher.utils.HookHelper;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;

public class SystemUIHook extends HookHelper {
    String TAG = PinningHook.class.getSimpleName();
    @Override
    public void init() {
        // 不要屏幕固定那个toast
        Class<?> ScreenPinningNotify = findClassIfExists("com.android.systemui.navigationbar.ScreenPinningNotify");
        if (ScreenPinningNotify != null) {
            Method[] methods = ScreenPinningNotify.getDeclaredMethods();
            for (Method method : methods) {
                switch (method.getName()) {
                    case "showPinningStartToast":
                    case "showPinningExitToast":
                    case "showEscapeToast":
                        if (method.getReturnType().equals(void.class)) hookToast(method);
                }
            }
        }
    }

    public void hookToast(Method method) {
        hookMethod(method,
                new HookAction() {
                    @Override
                    protected void before(XC_MethodHook.MethodHookParam param) {
                        param.setResult(null);
                    }
                }
        );
    }
}
