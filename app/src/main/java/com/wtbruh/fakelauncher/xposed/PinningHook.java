package com.wtbruh.fakelauncher.xposed;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.view.Display;
import android.view.KeyEvent;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wtbruh.fakelauncher.utils.ContentProvider;
import com.wtbruh.fakelauncher.utils.HookHelper;

import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class PinningHook extends HookHelper {

    public final static int LOCK_APP = 1;
    public final static int UNLOCK_APP = 2;
    // TCL T508N
    public final static String MODEL_T508N = "T508N";
    // coolpad Golden Century Y60 | 酷派金世纪Y60
    public final static String MODEL_CP23NV3 = "CP23NV3";
    // BIHEE A89 | 百合A89
    public final static String MODEL_BIHEE_A89 = "BIHEE A89";

    // public static Context CONTEXT;
    private static int mTaskId;
    private boolean mObserver = false;
    private boolean mLock = false;
    public static Handler mHandler = new LockAppHandler();

    private final static String TAG = PinningHook.class.getSimpleName();

    @Override
    public void init() {
        // Observe the changes of settings "fakelauncher_pinmode" and call system methods to start/stop pin mode
        findAndHookMethod("com.android.server.wm.ActivityTaskManagerService", "onSystemReady", new HookAction() {
            @Override
            protected void after(XC_MethodHook.MethodHookParam param) {

                // 机型判断
                String model = (String) callStaticMethod(
                        findClass("android.os.SystemProperties", null),
                        "get",
                        "ro.product.model",
                        MODEL_T508N
                );
                logI(TAG, "Detected device model: " + model);
                hook(model);

                try {
                    Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    if (context == null) {
                        context = findContext(FlAG_ONLY_ANDROID);
                        if (context == null) {
                            logE(tag, "onSystemReady context is null!!");
                            return;
                        }
                    }
                    if (!mObserver) {
                        Context finalContext = context;
                        ContentObserver contentObserver = new ContentObserver(new Handler(finalContext.getMainLooper())) {
                            @Override
                            public void onChange(boolean selfChange, @Nullable Uri uri, int flags) {
                                Cursor cursor = finalContext.getContentResolver().query(uri, null, null, null, null);
                                if (cursor != null && cursor.moveToFirst()) {
                                    mTaskId = cursor.getInt(0);
                                    cursor.close();
                                }
                                logI(TAG, "Get task id: " + mTaskId);
                                mLock = mTaskId != -1;
                                if (mLock) {
                                    callMethod(param.thisObject, "startSystemLockTaskMode", mTaskId);
                                } else {
                                    callMethod(param.thisObject, "stopSystemLockTaskMode");
                                }
                            }
                        };
                        context.getContentResolver().registerContentObserver(
                                ContentProvider.CONTENT_URI, false, contentObserver
                        );
                        mObserver = true;
                    }
                } catch (Throwable e) {
                    logE(tag, "E: " + e);
                }
            }
        });



    }
    @SuppressLint("MissingPermission")
    private void hook(String model) {
        // 获取真实class
        Class<?> PhoneWindowManager = findClassIfExists("com.android.server.policy.PhoneWindowManager");
        switch (model) {
            case MODEL_T508N -> {
                /*
                TCL T508N: 如果检测到我不是系统桌面，短按电源键不会熄屏，而是先返回桌面
                反编译framework后发现相关逻辑在com.android.server.policy.PhoneWindowManager里的powerPress方法里
                检测方法是获取最上层Activity然后跟字符串LAUNCHER_ACTIVITY_NAME对比。这玩意加了final修饰符所以不能用xposed改。
                在此hook该方法，然后增加判断：如果最上层Activity是我自己，也执行熄屏操作
                 */
                findAndHookMethod("com.android.server.policy.PhoneWindowManager", "powerPress",
                        long.class, int.class, boolean.class, new HookAction() {
                            @Override
                            protected void before(MethodHookParam param) {
                                super.before(param);
                                // 获取参数
                                long eventTime = 0;
                                eventTime = (long) param.args[0];

                                // 检查 mShortPressOnPowerBehavior 是否为 1
                                int behavior = XposedHelpers.getIntField(param.thisObject, "mShortPressOnPowerBehavior");
                                if (behavior != 1) {
                                    logI(TAG, "Power button behavior is not 1!");
                                    return; // 如果不是 1，不干预
                                }

                                // 获取参数
                                int count = (int) param.args[1];
                                boolean beganFromNonInteractive = (boolean) param.args[2];

                                // 检查 count 是否为 1 且满足其他条件
                                if (count == 1) {
                                    // 替代 Display.isOnState 的判断
                                    Object mDefaultDisplay = XposedHelpers.getObjectField(param.thisObject, "mDefaultDisplay");
                                    int displayState = (int) XposedHelpers.callMethod(mDefaultDisplay, "getState");

                                    // 直接判断 state 是否为 STATE_ON
                                    boolean interactive = (displayState == Display.STATE_ON);

                                    if (interactive && !beganFromNonInteractive) {
                                        Object activityManager = getObjectField(param.thisObject, "mActivityManager");
                                        List<?> runningTasks = (List<?>) XposedHelpers.callMethod(activityManager, "getRunningTasks", 1);
                                        ActivityManager.RunningTaskInfo runningTaskInfo = (ActivityManager.RunningTaskInfo) runningTasks.get(0);
                                        String TopClass = (String) XposedHelpers.callMethod(runningTaskInfo.topActivity, "getClassName");
                                        boolean isKeyguardShowing = (boolean) XposedHelpers.callMethod(
                                                param.thisObject, "isKeyguardShowing"
                                        );

                                        // 检查条件，如果读到了com.wtbruh.fakelauncher.MainActivity也一样调用熄屏方法，原代码是调用了sleepDefaultDisplayFromPowerButton
                                        if (TopClass.equals("com.wtbruh.fakelauncher.MainActivity") && !isKeyguardShowing) {
                                            Method sleepDefaultDisplayFromPowerButton = XposedHelpers.findMethodExact(
                                                    PhoneWindowManager,
                                                    "sleepDefaultDisplayFromPowerButton",
                                                    long.class,
                                                    int.class);
                                            sleepDefaultDisplayFromPowerButton.setAccessible(true);
                                            try {
                                                sleepDefaultDisplayFromPowerButton.invoke(param.thisObject, eventTime, 0);
                                            } catch (Throwable e) {
                                                throw new RuntimeException(e);
                                            }
                                            // 阻止原方法继续执行
                                            param.setResult(null);
                                        }
                                    }
                                }
                            }
                        });

            }
            case MODEL_BIHEE_A89 -> // BIHEE A89 Power key hook
                    findAndHookMethod("com.android.server.policy.PhoneWindowManager", "getRunningActivityName", new HookAction() {
                        @Override
                        protected void before(MethodHookParam param) {
                            super.before(param);
                            Context mContext = (Context) getObjectField(param.thisObject, "mContext");
                            ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
                            List<ActivityManager.RunningTaskInfo> runningTasks = activityManager.getRunningTasks(1);
                            if ((!runningTasks.isEmpty()) && runningTasks.get(0).topActivity != null) {
                                String runningActivity = runningTasks.get(0).topActivity.getClassName();
                                if (runningActivity.contains("com.wtbruh.fakelauncher") && !runningActivity.equals("com.wtbruh.fakelauncher.SettingsActivity")) {
                                    param.setResult("com.sprd.simple.launcher.Launcher");
                                }
                            }
                        }
                    });

            case MODEL_CP23NV3 -> {
                logI(TAG, MODEL_CP23NV3);
                findAndHookMethod("com.android.server.policy.PhoneWindowManager", "isHomeActivity", new HookAction() {
                    @Override
                    protected void before(MethodHookParam param) {
                        super.before(param);
                        ComponentName fakelauncher = new ComponentName("com.wtbruh.fakelauncher", "com.wtbruh.fakelauncher.SettingsActivity");

                        Method getTopActivity = XposedHelpers.findMethodExact(
                                PhoneWindowManager,
                                "getTopActivity"
                        );
                        getTopActivity.setAccessible(true);
                        ComponentName top;
                        try {
                            top = (ComponentName) getTopActivity.invoke(param.thisObject);
                        } catch (Exception e) {
                            top = null;
                        }

                        if (top != null) {
                            if ((top.getPackageName().equals("com.wtbruh.fakelauncher") && !fakelauncher.equals(top))) {
                                param.setResult(true);
                            }
                        }
                    }
                });

            }
        }
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

    /**
     * Handle message to turn on/off screen pinning<br>
     * 处理消息以开关屏幕固定
     *
     * @noinspection deprecation
     * @author HChenX
     */
    public static class LockAppHandler extends Handler {

        private final static String TAG = LockAppHandler.class.getSimpleName();

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            logI(TAG, "Received message! ");
            Context context = findContext(FlAG_ONLY_ANDROID);
            logI(TAG, "Context package name: " + context.getPackageName());
            if (context == null) {
                logI(TAG, "Context is null!!!");
                mHandler.sendMessageDelayed(mHandler.obtainMessage(msg.what), 500);
                return;
            }
            logI(TAG, "Message content: " + msg.what);
            switch (msg.what) {
                case LOCK_APP:
                    setLockApp(context, (int) msg.obj);
                    break;
                case UNLOCK_APP:
                    setLockApp(context, -1);
                    break;
            }
        }
    }

}

