package com.wtbruh.fakelauncher.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.KeyEvent;

import androidx.preference.PreferenceManager;

import com.wtbruh.fakelauncher.R;

public class UIHelper {
    private static long lastTriggerTime = 0;
    private static final long DEBOUNCE_TIME = 300;

    private final static String TAG = UIHelper.class.getSimpleName();

    /**
     * <h3>Text editor | 文本内容编辑器</h3>
     * <p>Custom input method for scenes that require input,<br>
     * accepts back key, star key, pound key, and 0~9.</p>
     * 为程序内需要输入文本的场景而自定义的小输入法，<br>
     * 接受Back键、*#键和0~9键</p>
     *
     * @param keyCode 输入的键值
     * @param content 输入前文本框里的文本内容
     * @return 最终文本内容
     */
    public static String textEditor(int keyCode, String content) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                return content.substring(0, content.length() - 1);
            case KeyEvent.KEYCODE_POUND:
                return content + "#";
            case KeyEvent.KEYCODE_STAR:
                return content + "*";
            default:
                int num = keyCode - KeyEvent.KEYCODE_0;
                return content + num;
        }
    }

    /**
     * 检查用户当前设置的退出方式
     *
     * @param context 上下文
     * @param expected 预期的退出方式
     * @return 如果与预期不符，返回false，否则返回true
     */
    public static boolean checkExitMethod(Context context, int expected) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String[] valueArray = context.getResources().getStringArray(R.array.pref_exit_fakeui_method);
        String exitMethod = pref.getString("exit_fakeui_method", valueArray[0]);
        return exitMethod.equals(valueArray[expected]);
    }

    /**
     * <h3>Intent Starter | Intent启动器</h3>
     * <p>Simple package for starting intent<br>
     * 启动intent的简单封装</p>
     *
     * @param activity 你的Activity对象
     * @param cls 要启动的Activity的class
     */
    public static void intentStarter(Activity activity, Class<?> cls) {
        if (intentStarterDebounce(cls)) return;
        Intent intent = new Intent();
        intent.setClass(activity, cls);
        activity.startActivity(intent);
        // Disable transition anim
        // 去掉过渡动画
        activity.overridePendingTransition(0, 0);
    }

    /**
     * <h3>Intent Starter | Intent启动器</h3>
     * <p>Simple package for starting intent, which will send intent flags<br>
     * 启动intent的简单封装，会发送Intent标志</p>
     *
     * @param activity 你的Activity对象
     * @param cls 要启动的Activity的class
     * @param flags 要添加的标志，允许多个
     */
    public static void intentStarter(Activity activity, Class<?> cls, int... flags) {
        if (intentStarterDebounce(cls)) return;
        Intent intent = new Intent();
        intent.setClass(activity, cls);
        for (int flag : flags) intent.addFlags(flag);
        activity.startActivity(intent);
        // Disable transition anim
        // 去掉过渡动画
        activity.overridePendingTransition(0, 0);
    }

    /**
     * <h3>Intent Starter | Intent启动器</h3>
     * <p>Simple package for starting intent, which will send extra message<br>
     * 启动intent的简单封装，会发送额外信息</p>
     *
     * @param activity 你的Activity对象
     * @param cls 要启动的Activity的class
     * @param extraName 额外数据名称
     * @param extra 额外数据内容
     */
    public static void intentStarter(Activity activity, Class<?> cls, String extraName, String extra) {
        if (intentStarterDebounce(cls)) return;
        Intent intent = new Intent();
        intent.setClass(activity, cls);
        intent.putExtra(extraName, extra);
        activity.startActivity(intent);
        // Disable transition anim
        // 去掉过渡动画
        activity.overridePendingTransition(0, 0);
    }
    /**
     * <h3>Intent Starter Debounce<br>
     * Intent启动器 防抖机制</h3>
     * <p>Prevent calling intentStarter too frequently<br>
     * 防止过于频繁地调用intentStarter</p>
     *
     * @param cls 要启动的Activity的class
     * @return true为调用过于频繁，false为调用频率正常
     */
    public static boolean intentStarterDebounce(Class<?> cls){
        // Only trigger intent starter at regularly intervals
        // 只在一定间隔时间内触发代码执行
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTriggerTime <= DEBOUNCE_TIME) return true;
        lastTriggerTime = currentTime;
        // If the activity is already on top, do not launch
        return ApplicationHelper.topActivity.contains(cls.getSimpleName());
    }
}
