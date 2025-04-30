package com.wtbruh.fakelauncher.xposed;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wtbruh.fakelauncher.utils.HookHelper;

import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class PinningHook extends HookHelper {

    public final static int LOCK_APP = 1;
    public final static int UNLOCK_APP = 2;

    // public static Context CONTEXT;
    private static int mTaskId;
    private boolean mObserver = false;
    private boolean mLock = false;
    public static Handler mHandler = new LockAppHandler();

    public static Context context;
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
                    // CONTEXT = context;
                    if (!mObserver) {
                        Context finalContext = context;
                        ContentObserver contentObserver = new ContentObserver(new Handler(finalContext.getMainLooper())) {
                            @Override
                            public void onChange(boolean selfChange, @Nullable Uri uri, int flags) {
                                mLock = getLockApp(finalContext) != -1;
                                if (mLock) {
                                    mTaskId = getLockApp(finalContext);
                                    callMethod(param.thisObject, "startSystemLockTaskMode", mTaskId);
                                } else {
                                    callMethod(param.thisObject, "stopSystemLockTaskMode");
                                }
                            }
                        };
                        context.getContentResolver().registerContentObserver(
                            Settings.Global.getUriFor("fakelauncher_pinmode"), false, contentObserver
                        );
                        mObserver = true;
                    }
                } catch (Throwable e) {
                    logE(tag, "E: " + e);
                }
            }
        });

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
                        // 检查 mShortPressOnPowerBehavior 是否为 1
                        int behavior = XposedHelpers.getIntField(param.thisObject, "mShortPressOnPowerBehavior");
                        if (behavior != 1) {
                            return; // 如果不是 1，不干预
                        }

                        // 获取参数
                        long eventTime = (long) param.args[0];
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
                                // 获取 TopClass 和 isKeyguardShowing

                                Object activityManager = XposedHelpers.getObjectField(param.thisObject, "mActivityManager");
                                List<?> runningTasks = (List<?>) XposedHelpers.callMethod(activityManager, "getRunningTasks", 1);
                                ActivityManager.RunningTaskInfo runningTaskInfo = (ActivityManager.RunningTaskInfo) runningTasks.get(0);
                                String TopClass = (String) XposedHelpers.callMethod(runningTaskInfo.topActivity, "getClassName");
                                boolean isKeyguardShowing = (boolean) XposedHelpers.callMethod(
                                        param.thisObject, "isKeyguardShowing"
                                );

                                // 检查条件，如果读到了com.wtbruh.fakelauncher.MainActivity也一样调用熄屏方法，原代码是调用了sleepDefaultDisplayFromPowerButton
                                if (TopClass.equals("com.wtbruh.fakelauncher.MainActivity") && !isKeyguardShowing) {
                                    /*
                                    如果直接调用方法 sleepDefaultDisplayFromPowerButton 熄屏会报错
                                    因为我们实际上是hook到了UnisocPhoneWindowManager类
                                    而这个方法只在PhoneWindowManager类里有
                                    所以需要用到反射调用方法

                                    XposedHelpers.callMethod(
                                            param.thisObject,
                                            "sleepDefaultDisplayFromPowerButton",
                                            eventTime,
                                            0
                                    );

                                    另一种实现：直接绕过sleepDefaultDisplayFromPowerButton，调用PowerManager熄屏
                                    感觉这种实现应该会更适配其他机型
                                    import android.os.PowerManager;

                                    PowerManager pm = (PowerManager) currentApplication().getSystemService(Context.POWER_SERVICE);
                                    try {
                                        // 尝试调用 Android 7.0+ 的方法
                                        XposedHelpers.callMethod(
                                                pm,
                                                "goToSleep",
                                                eventTime,
                                                0,
                                                0
                                        );
                                    } catch (NoSuchMethodError e) {
                                        // 回退到 Android 6.0 的方法
                                        XposedHelpers.callMethod(
                                                pm,
                                                "goToSleep",
                                                eventTime
                                        );
                                    }
                                     */
                                    Class<?> PhoneWindowManager = findClassIfExists("com.android.server.policy.PhoneWindowManager");
                                    Method sleepDefaultDisplayFromPowerButton = XposedHelpers.findMethodExact(
                                            PhoneWindowManager,
                                            "sleepDefaultDisplayFromPowerButton",
                                            long.class,
                                            int.class);
                                    sleepDefaultDisplayFromPowerButton.setAccessible(true);
                                    try {
                                        sleepDefaultDisplayFromPowerButton.invoke(param.thisObject,eventTime, 0);
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

        /*
        还有一种方法，hook用来获取最上层Activity信息的方法getRunningTasks，
        如果返回结果是com.wtbruh.fakelauncher.MainActivity
        就给他替换成com.android.launcher3.uioverrides.QuickstepLauncher
        这样powerPress那边就认为最上层Activity是系统桌面然后执行熄屏操作
        这种写法看着比上面省代码，但后果是整个系统都会把这两个混淆在一起（例如启动系统桌面结果启动了fakelauncher），所以注释掉了
        import android.content.ComponentName;

        findAndHookMethod("android.app.ActivityManager",
                "getRunningTasks",
                int.class,
                new HookAction() {
                    @Override
                    protected void after(MethodHookParam param) {
                        super.after(param);
                        List<?> tasks = (List<?>) param.getResult();
                        if (tasks != null && !tasks.isEmpty()) {
                            Object taskInfo = tasks.get(0);
                            ComponentName topActivity = (ComponentName) XposedHelpers.getObjectField(taskInfo, "topActivity");

                            // 如果当前 Activity 是我们的假 Launcher，替换成系统 Launcher
                            if (topActivity != null && topActivity.getClassName().equals("com.wtbruh.fakelauncher.MainActivity")) {
                                XposedHelpers.setObjectField(
                                        taskInfo,
                                        "topActivity",
                                        new ComponentName(topActivity.getPackageName(), "com.android.launcher3.uioverrides.QuickstepLauncher")
                                );
                            }
                        }
                    }
                });
         */

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
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            Log.d("test", "666888");
            Context context = findContext(FLAG_CURRENT_APP);
            if (context == null) {
                Log.d("test", "66666666688888888");
                mHandler.sendMessageDelayed(mHandler.obtainMessage(msg.what), 500);
                return;
            }
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

