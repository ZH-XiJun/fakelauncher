package com.wtbruh.fakelauncher.xposed;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.view.Display;

import com.wtbruh.fakelauncher.utils.HookHelper;

import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;

public class PhoneWindowManagerHook extends HookHelper {

    private final static String TAG = PhoneWindowManagerHook.class.getSimpleName();

    // TCL T508N
    public final static String MODEL_T508N = "T508N";
    // coolpad Golden Century Y60 | 酷派金世纪Y60
    public final static String MODEL_CP23NV3 = "CP23NV3";
    // These models seems share the same set of source codes
    // 这些手机似乎用的是同一套源码
    public final static String[] MODEL_UNIVERSAL = {
            "BIHEE A89",
            "VIPME M8",
            "angelcare K1"
    };

    /** 缓存的 PhoneWindowManager 实例 */
    private static Object sPhoneWindowManager;

    /** 原始导航栏尺寸，用于策略一恢复 (-1 表示未读取) */
    private static int sNavBarWpOrigin = -1, sNavBarHpOrigin = -1, sNavBarHlOrigin = -1;

    @Override
    public void init() {
        findAndHookMethod("com.android.server.wm.ActivityTaskManagerService", "onSystemReady", new HookAction() {
            @Override
            protected void after(MethodHookParam param) {
                // 从 ATS → WMS → mPolicy 链路获取 PWM 实例
                // ATS.mWindowManager = WindowManagerService
                // WMS.mPolicy        = PhoneWindowManager
                capturePwmFromAts(param.thisObject);

                String model = (String) callStaticMethod(
                        findClass("android.os.SystemProperties", null),
                        "get",
                        "ro.product.model",
                        MODEL_T508N
                );
                logI(TAG, "Detected device model: " + model);
                hook(model);
            }
        });
    }

    /** 从 ActivityTaskManagerService 获取 PhoneWindowManager 实例 */
    private static void capturePwmFromAts(Object ats) {
        if (sPhoneWindowManager != null) return;
        try {
            Object wms = XposedHelpers.getObjectField(ats, "mWindowManager");
            if (wms == null) {
                logE(TAG, "ATS.mWindowManager is null");
                return;
            }
            logI(TAG, "Got WMS: " + wms.getClass().getName());

            sPhoneWindowManager = XposedHelpers.getObjectField(wms, "mPolicy");
            if (sPhoneWindowManager == null) {
                logE(TAG, "WMS.mPolicy is null");
                return;
            }
            logI(TAG, "Got PWM: " + sPhoneWindowManager.getClass().getName());

            // 读取原始导航栏尺寸
            try {

                Context ctx = (Context) XposedHelpers.getObjectField(sPhoneWindowManager, "mContext");
                if (ctx != null) {
                    Resources res = ctx.getResources();
                    sNavBarWpOrigin = res.getDimensionPixelSize(
                            res.getIdentifier("navigation_bar_width", "dimen", "android"));
                    sNavBarHpOrigin = res.getDimensionPixelSize(
                            res.getIdentifier("navigation_bar_height", "dimen", "android"));
                    sNavBarHlOrigin = res.getDimensionPixelSize(
                            res.getIdentifier("navigation_bar_height_landscape", "dimen", "android"));
                }
                logI(TAG, "navBar resource dims: w=" + sNavBarWpOrigin
                        + " hp=" + sNavBarHpOrigin + " hl=" + sNavBarHlOrigin);
            } catch (Throwable t) {
                logE(TAG, "Failed to read navBar dims: " + t);
            }
        } catch (Throwable t) {
            logE(TAG, "capturePwmFromAts failed: " + t);
        }
    }

    /** 诊断: dump PWM 对象的所有字段名 */
    private static void dumpAllFields(Object obj) {
        try {
            Class<?> clz = obj.getClass();
            logI(TAG, "=== Fields of " + clz.getName() + " ===");
            // 遍历当前类及父类
            while (clz != null && clz != Object.class) {
                for (java.lang.reflect.Field f : clz.getDeclaredFields()) {
                    f.setAccessible(true);
                    try {
                        Object val = f.get(obj);
                        String type = (val instanceof int[]) ? "int[" + ((int[])val).length + "]"
                                : (val != null ? val.getClass().getSimpleName() : "null");
                        if (f.getName().toLowerCase().contains("nav")
                                || f.getName().toLowerCase().contains("bar")
                                || f.getName().toLowerCase().contains("status")) {
                            logI(TAG, "  " + f.getName() + " = " + type);
                        }
                    } catch (Throwable ignored) {}
                }
                clz = clz.getSuperclass();
            }
            logI(TAG, "=== End of fields ===");
        } catch (Throwable t) {
            logE(TAG, "dumpAllFields failed: " + t);
        }
    }

    /**
     * 隐藏/显示导航栏。
     * 多策略回退：
     *   1. 标准 AOSP: 修改 mNavigationBar*ForRotationDefault[] 数组 + updateRotation()
     *   2. BarController: 直接调用 mNavigationBar.show()/hide() 或 showLw()/hideLw()
     *   3. PolicyControl: 通过 WindowManagerPolicyControl 设置沉浸模式
     */
    public static void setNavBarHidden(boolean hidden) {
        if (sPhoneWindowManager == null) {
            logI(TAG, "setNavBarHidden: PWM not captured yet!");
            return;
        }

        boolean success = false;

        // 策略一: 标准 AOSP — 导航栏尺寸数组置零
        success = tryNavBarDimArray(hidden);
        if (success) return;

        // 策略二: BarController 直接操控
        success = tryNavBarController(hidden);
        if (success) return;

        // 策略三: PolicyControl / SystemUiVisibility
        success = tryPolicyControl(hidden);
        if (success) return;

        logE(TAG, "setNavBarHidden: all strategies failed!");
    }

    /** 策略一: 导航栏尺寸数组置零 (标准 AOSP 方式) */
    private static boolean tryNavBarDimArray(boolean hidden) {
        try {
            // 动态探测正确的字段名
            String fieldW = findNavField(sPhoneWindowManager, "Width");
            String fieldH = findNavField(sPhoneWindowManager, "Height");
            if (fieldW == null || fieldH == null) {
                logI(TAG, "tryNavBarDimArray: rotation dimension fields not found, skip");
                return false;
            }

            if (sNavBarWpOrigin < 0) {
                logI(TAG, "tryNavBarDimArray: origin dimensions not read, skip");
                return false;
            }

            int wp = hidden ? 0 : sNavBarWpOrigin;
            int hp = hidden ? 0 : sNavBarHpOrigin;
            int hl = hidden ? 0 : sNavBarHlOrigin;

            int[] navW = (int[]) XposedHelpers.getObjectField(sPhoneWindowManager, fieldW);
            int[] navH = (int[]) XposedHelpers.getObjectField(sPhoneWindowManager, fieldH);

            int portrait   = XposedHelpers.getIntField(sPhoneWindowManager, "mPortraitRotation");
            int upsideDown = XposedHelpers.getIntField(sPhoneWindowManager, "mUpsideDownRotation");
            int landscape  = XposedHelpers.getIntField(sPhoneWindowManager, "mLandscapeRotation");
            int seascape   = XposedHelpers.getIntField(sPhoneWindowManager, "mSeascapeRotation");

            navH[portrait] = navH[upsideDown] = hp;
            navH[landscape] = navH[seascape] = hl;
            navW[portrait] = navW[upsideDown] = navW[landscape] = navW[seascape] = wp;

            XposedHelpers.callMethod(sPhoneWindowManager, "updateRotation", false);
            logI(TAG, "NavBar " + (hidden ? "hidden" : "shown")
                    + " via dimArray, wp=" + wp + " hp=" + hp + " hl=" + hl);
            return true;
        } catch (Throwable t) {
            logI(TAG, "tryNavBarDimArray failed: " + t);
            return false;
        }
    }

    /** 策略二: 直接操控 mNavigationBar BarController */
    private static boolean tryNavBarController(boolean hidden) {
        try {
            Object navBar = XposedHelpers.getObjectField(sPhoneWindowManager, "mNavigationBar");
            if (navBar == null) return false;

            if (android.os.Build.VERSION.SDK_INT >= 31) {
                XposedHelpers.callMethod(navBar, hidden ? "hide" : "show", false);
            } else {
                XposedHelpers.callMethod(navBar, hidden ? "hideLw" : "showLw", true);
            }
            logI(TAG, "NavBar " + (hidden ? "hidden" : "shown") + " via BarController");
            return true;
        } catch (Throwable t) {
            logI(TAG, "tryNavBarController failed: " + t);
            return false;
        }
    }

    /** 策略三: PolicyControl 沉浸模式 */
    private static boolean tryPolicyControl(boolean hidden) {
        try {
            Class<?> policyCtrl = XposedHelpers.findClassIfExists(
                    "android.view.WindowManagerPolicyControl", null);
            if (policyCtrl == null) {
                policyCtrl = XposedHelpers.findClassIfExists(
                        "com.android.server.wm.PolicyControl", null);
            }
            if (policyCtrl == null) return false;

            if (hidden) {
                XposedHelpers.callStaticMethod(policyCtrl, "setSystemUiVisibility",
                        null, 0x0002 | 0x1000);
            } else {
                XposedHelpers.callStaticMethod(policyCtrl, "setSystemUiVisibility", null, 0);
            }
            try { XposedHelpers.callMethod(sPhoneWindowManager, "updateRotation", false); } catch (Throwable ignored) {}
            logI(TAG, "NavBar " + (hidden ? "hidden" : "shown") + " via PolicyControl");
            return true;
        } catch (Throwable t) {
            logI(TAG, "tryPolicyControl failed: " + t);
            return false;
        }
    }

    /** 动态探测导航栏尺寸字段名 (运行时反射，不依赖硬编码) */
    private static String findNavField(Object pwm, String dimension) {
        // 按优先级尝试多个可能的字段名
        String[] candidates = {
            "mNavigationBar" + dimension + "ForRotationDefault",
            "mNavigationBar" + dimension + "ForRotation",
            "mNavBar" + dimension + "ForRotationDefault",
            "mNavBar" + dimension + "ForRotation",
        };
        for (String name : candidates) {
            try {
                Object val = XposedHelpers.getObjectField(pwm, name);
                if (val instanceof int[]) {
                    logI(TAG, "Found nav field: " + name);
                    return name;
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }

    @SuppressLint("MissingPermission")
    private void hook(String model) {
        // 获取真实class
        Class<?> PhoneWindowManager = findClassIfExists("com.android.server.policy.PhoneWindowManager");

        // Hook power long press behavior, make it shut down directly instead of showing power menu
        // 修改电源键长按逻辑，长按不显示电源菜单而是直接关机
        findAndHookMethod("com.android.server.policy.PhoneWindowManager", "powerLongPress", long.class, new HookAction() {
            @Override
            protected void before(MethodHookParam param) {
                super.before(param);
                if (PinningHook.getLockState()) {
                    XposedHelpers.setBooleanField(param.thisObject, "mPowerKeyHandled", true);
                    XposedHelpers.callMethod(param.thisObject, "sendCloseSystemWindows");
                    Object mWindowManagerFuncs = XposedHelpers.getObjectField(param.thisObject, "mWindowManagerFuncs");
                    XposedHelpers.callMethod(mWindowManagerFuncs, "shutdown", false);
                    param.setResult(null);
                }
            }
        });


        for (String s : MODEL_UNIVERSAL) {
            if (s.equals(model)) {
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
                return;
            }
        }
        switch (model) {
            case MODEL_T508N -> /*
                TCL T508N: 如果检测到我不是系统桌面，短按电源键不会熄屏，而是先返回桌面
                反编译framework后发现相关逻辑在com.android.server.policy.PhoneWindowManager里的powerPress方法里
                检测方法是获取最上层Activity然后跟字符串LAUNCHER_ACTIVITY_NAME对比。这玩意加了final修饰符所以不能用xposed改。
                在此hook该方法，然后增加判断：如果最上层Activity是我自己，也执行熄屏操作
                 */ findAndHookMethod("com.android.server.policy.PhoneWindowManager", "powerPress",
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
}
