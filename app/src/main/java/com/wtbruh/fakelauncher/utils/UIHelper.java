package com.wtbruh.fakelauncher.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.ui.fragment.settings.SubSettingsFragment;

public class UIHelper {
    private static long lastTriggerTime = 0;
    private static final long DEBOUNCE_TIME = 300;

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
        return switch (keyCode) {
            case KeyEvent.KEYCODE_BACK -> content.substring(0, content.length() - 1);
            case KeyEvent.KEYCODE_POUND -> content + "#";
            case KeyEvent.KEYCODE_STAR -> content + "*";
            default -> {
                int num = keyCode - KeyEvent.KEYCODE_0;
                yield content + num;
            }
        };
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
        String exitMethod = pref.getString(SubSettingsFragment.PREF_EXIT_FAKEUI_METHOD, valueArray[0]);
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

    /**
     * <h3>Custom dialog | 自定义弹窗</h3>
     * <p>Imitate the style of dialog in feature phone<br>
     * 模仿老人机的弹窗样式</p>
     * @param context 上下文
     * @param msgResId 要显示的文本的资源id
     * @param listener 按键监听器
     */
    public static Dialog showCustomDialog(Context context, int msgResId, DialogInterface.OnKeyListener listener) {
        // Load custom layout
        // 加载自制布局
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_custom, null, false);
        // Set message
        // 设置消息文本
        TextView messageTv = view.findViewById(R.id.dialogMessage);
        messageTv.setText(msgResId);

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.Theme_FakeLauncher_Dialog);
        AlertDialog dialog =  builder.setView(view).create();
        // Touch event is not allowed
        // 杜绝触屏操作，不然穿帮了
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        if (listener != null) dialog.setOnKeyListener(listener);

        Window window = dialog.getWindow();
        if (window != null) {
            // Disable transition anim
            // 去掉过渡动画
            window.setWindowAnimations(0);
        }

        // 展示3秒后关闭
        new Handler().postDelayed(() -> {
            if (dialog.isShowing()) dialog.dismiss();
        }, 3000);
        return dialog;
    }

    public static Dialog showConfirmDialog(Context context, String title, String msg, DialogInterface.OnKeyListener keyListener, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        return builder.setTitle(title)
                .setMessage(msg)
                .setOnKeyListener(keyListener)
                .setPositiveButton(android.R.string.yes, positiveListener)
                .setNegativeButton(android.R.string.no, negativeListener)
                .show();
    }

    public static Dialog showConfirmDialog(Context context, String title, String msg, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener) {
        return showConfirmDialog(context, title, msg, null, positiveListener, negativeListener);
    }
}
