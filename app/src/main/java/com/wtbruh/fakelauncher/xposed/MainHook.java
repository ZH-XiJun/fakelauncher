package com.wtbruh.fakelauncher.xposed;

import android.util.Log;
import android.view.MotionEvent;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        hook(lpparam);
    }
    private void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> ViewGroup = XposedHelpers.findClass("android.view.ViewGroup", lpparam.classLoader);
        XposedHelpers.findAndHookMethod(ViewGroup, "onInterceptTouchEvent", MotionEvent.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Log.e("bruhhhh","hooked123");
                param.setResult(true);
            }
        });
        Class<?> View = XposedHelpers.findClass("android.view.View", lpparam.classLoader);
        XposedHelpers.findAndHookMethod(View, "onTouchEvent", MotionEvent.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Log.e("bruhhhh","hooked456");
                param.setResult(true);
            }
        });
    }
}
