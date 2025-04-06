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

    private static int taskId;
    private boolean isObserver = false;
    boolean isLock = false;
    String TAG = PinningHook.class.getSimpleName();
    static Context CONTEXT;
    @Override
    public void init() {
        // Get context
        findAndHookMethod("com.android.server.wm.ActivityTaskManagerService", "onSystemReady", new HookAction() {
                    @Override
                    protected void after(XC_MethodHook.MethodHookParam param) {
                        try {
                            CONTEXT = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                            if (CONTEXT == null) {
                                CONTEXT = findContext(FlAG_ONLY_ANDROID);
                                if (CONTEXT == null) {
                                    logE(tag, "onSystemReady context is null!!");
                                }
                            }
                        } catch (Throwable e) {
                            logE(tag, "E: " + e);
                        }
                    }
                });
        // Get task id and do changes to setting "fakelauncher_pinmode"
        findAndHookMethod("com.wtbruh.fakelauncher.MainActivity", "onCreate", new HookAction() {
            @Override
            protected void after(MethodHookParam param) {
                super.after(param);
                Log.d(TAG, "testTEST");
                taskId = (int) callMethod(param.thisObject, "getTaskID");
            }
        });

        // Observe the changes of settings "fakelauncher_pinmode" and call system methods to start/stop pin mode
        findAndHookMethod("com.android.server.wm.ActivityTaskManagerService", "onSystemReady", new HookAction() {
            @Override
            protected void after(XC_MethodHook.MethodHookParam param) {
                Context context = CONTEXT;
                try {
                    if (context == null) {
                        logE(tag, "onSystemReady context is null!!");
                        return;
                        }
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
