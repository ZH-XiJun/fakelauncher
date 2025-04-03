package com.wtbruh.fakelauncher.xposed;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.utils.ApplicationHelper;

import java.util.Timer;
import java.util.TimerTask;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {
    static String TAG = "MainHook";
    ApplicationHelper helper = new ApplicationHelper();
    Context context;
    public boolean isMyLauncherDefault(Context context) {

        PackageManager pm = context.getPackageManager();

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);

        ResolveInfo homeActivities = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (homeActivities == null) return false;
        return context.getPackageName().equals(homeActivities.activityInfo.packageName);
    }
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // get context
        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                context = (Context) param.args[0];
            }
        });
        // hook method
        hook(lpparam);
    }
    private void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        Log.d(TAG, lpparam.packageName);
        if (lpparam.packageName.equals("com.android.systemui")) {
            Log.d(TAG, "Hooked systemui!");
            int state = 0;
            while (true) {
                if (state == 0 && isMyLauncherDefault(context)) {
                    state = 1;
                    disableTouch(lpparam, true);
                    Log.d(TAG, "Disable touch");
                } else if (state == 1 && !isMyLauncherDefault(context)) {
                    state = 0;
                    disableTouch(lpparam, false);
                    Log.d(TAG, "Enable touch");
                }
            }
        }
    }
    private void disableTouch(XC_LoadPackage.LoadPackageParam lpparam, boolean mode) {
        Class<?> view = XposedHelpers.findClass("android.view.View", lpparam.classLoader);
        XposedHelpers.findAndHookMethod(view, "onTouchEvent", MotionEvent.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                param.setResult(mode);
            }
        });
        Class<?> viewGroup = XposedHelpers.findClass("android.view.ViewGroup", lpparam.classLoader);
        XposedHelpers.findAndHookMethod(viewGroup, "onInterceptTouchEvent", MotionEvent.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                param.setResult(mode);
            }
        });
    }
}
