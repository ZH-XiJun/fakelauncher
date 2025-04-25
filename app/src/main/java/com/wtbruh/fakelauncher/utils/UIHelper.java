package com.wtbruh.fakelauncher.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.KeyEvent;

public class UIHelper {

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
                return content + String.format("%d",num);
        }
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
        // If the activity is already on top, do not launch
        if (ApplicationHelper.topActivity.contains(cls.getSimpleName())) return;
        Intent intent = new Intent();
        intent.setClass(activity, cls);
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
        Intent intent = new Intent();
        intent.setClass(activity, cls);
        intent.putExtra(extraName, extra);
        activity.startActivity(intent);
        // Disable transition anim
        // 去掉过渡动画
        activity.overridePendingTransition(0, 0);
    }
}
