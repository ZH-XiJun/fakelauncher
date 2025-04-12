package com.wtbruh.fakelauncher.utils;

import android.view.KeyEvent;

public class UIHelper {

    private final static String TAG = UIHelper.class.getSimpleName();

    /**
     * Custom input method, accepts back key, star key, pound key, and 0~9.
     * <p>
     * 自定义小输入法，接受Back键、*#键和0~9键
     *
     * @param keyCode 键值
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
     * Simple package for starting intent
     * <p>
     * 启动intent的简单封装
     *
     * @param activity 你的Activity对象
     * @param cls 要启动的Activity的class
     */
    public static void intentStarter(Activity activity, Class<?> cls) {
        Intent intent = new Intent();
        intent.setClass(activity, cls);
        activity.startActivity(intent);
        // Disable transition anim
        // 去掉过渡动画
        activity.overridePendingTransition(0, 0);
    }

    /**
     * Simple package for starting intent, which will send extra message
     * <p>
     * 启动intent的简单封装，会发送额外信息
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
