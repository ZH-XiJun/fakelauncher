package com.wtbruh.fakelauncher.xposed;

import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;

import com.wtbruh.fakelauncher.utils.HookHelper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class PinningHook extends HookHelper {
    public static Context CONTEXT;
    private static int taskId;
    private boolean isObserver = false;
    boolean isLock = false;
    String TAG = PinningHook.class.getSimpleName();
    @Override
    public void init() {
        // Observe the changes of settings "fakelauncher_pinmode" and call system methods to start/stop pin mode
        findAndHookMethod("com.android.server.wm.ActivityTaskManagerService", "onSystemReady", new HookAction() {
            @Override
            protected void after(XC_MethodHook.MethodHookParam param) {
                try {
                    Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    if (context == null) {
                        context = findContext(FlAG_ONLY_ANDROID);
                        if (context == null) {
                            logE(tag, "onSystemReady context is null!!");
                            return;
                        }
                    }
                    CONTEXT = context;
                    if (!isObserver) {
                        Context finalContext = context;
                        ContentObserver contentObserver = new ContentObserver(new Handler(finalContext.getMainLooper())) {
                            @Override
                            public void onChange(boolean selfChange, @Nullable Uri uri, int flags) {
                                isLock = getLockApp(finalContext) != -1;
                                if (isLock) {
                                    taskId = getLockApp(finalContext);
                                    callMethod(param.thisObject, "startSystemLockTaskMode", taskId);
                                } else {
                                    callMethod(param.thisObject, "stopSystemLockTaskMode");
                                }
                            }
                        };
                        context.getContentResolver().registerContentObserver(
                            Settings.Global.getUriFor("fakelauncher_pinmode"), false, contentObserver
                        );
                        isObserver = true;
                    }
                } catch (Throwable e) {
                    logE(tag, "E: " + e);
                }
            }
        });


    }
    public int getLockApp(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(), "fakelauncher_pinmode");
        } catch (Settings.SettingNotFoundException e) {
            logE("LockApp", "getInt fakelauncher_pinmode will set -1 E: " + e);
            setLockApp(context, -1);
        }
        return -1;
    }

    public static void setLockApp(Context context, int id) {
        Settings.Global.putInt(context.getContentResolver(), "fakelauncher_pinmode", id);
    }

    private static Context currentApplication() {
        return (Application) XposedHelpers.callStaticMethod(XposedHelpers.findClass(
                        "android.app.ActivityThread", null),
                "currentApplication");
    }

}
