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
    Context context;
    ApplicationHelper helper = new ApplicationHelper();

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
        // hook(lpparam);
    }
    private void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        Timer timer = new Timer();
        Log.d("HOOK","im running!");
        Log.d("HOOK", lpparam.packageName);
        Class<?> ViewGroup = XposedHelpers.findClass("android.view.ViewGroup", lpparam.classLoader);
        Class<?> View = XposedHelpers.findClass("android.view.View", lpparam.classLoader);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        XposedHelpers.findAndHookMethod(ViewGroup, "onInterceptTouchEvent", MotionEvent.class, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                super.beforeHookedMethod(param);
                                if (isMyLauncherDefault(context)) {
                                    Log.d("HOOK1","im default launcher, disable touchscreen!");
                                    param.setResult(true);
                                } else {
                                    Log.d("HOOK1","im not default launcher, dont disable touchscreen!");
                                }
                            }
                        });
                        XposedHelpers.findAndHookMethod(View, "onTouchEvent", MotionEvent.class, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                super.beforeHookedMethod(param);
                                if (isMyLauncherDefault(context)) {
                                    Log.d("HOOK2","im default launcher, disable touchscreen!");
                                    param.setResult(true);
                                } else {
                                    Log.d("HOOK2","im not default launcher, dont disable touchscreen!");
                                }
                            }
                        });
                    }
                });
            }
        }, 0, 1000);
    }
}
