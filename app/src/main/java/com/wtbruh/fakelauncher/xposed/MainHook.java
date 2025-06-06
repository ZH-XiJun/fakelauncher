package com.wtbruh.fakelauncher.xposed;

import com.wtbruh.fakelauncher.utils.HookHelper;
import com.wtbruh.fakelauncher.utils.LogHelper;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook extends LogHelper implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    public static String modulePath;
    private final static String TAG = MainHook.class.getSimpleName();

    public static void initHook(HookHelper hook, XC_LoadPackage.LoadPackageParam lpparam) {
        hook.runHook(lpparam);
    }

    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
        modulePath = startupParam.modulePath;
    }
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        // hook method
        switch (lpparam.packageName) {
            case "android":
                initHook(new PinningHook(), lpparam);
                logI(TAG, "Android framework hooked");
                break;
            case "com.android.systemui":
                initHook(new SystemUIHook(), lpparam);
                logI(TAG, "Android SystemUI hooked");
                break;
            case "com.wtbruh.fakelauncher":
                initHook(new SelfHook(), lpparam);
                logI(TAG, "Hooked myself");
                break;
        }
    }
}
