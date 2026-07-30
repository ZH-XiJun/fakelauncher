package com.wtbruh.fakelauncher.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.wtbruh.fakelauncher.ApplicationHelper;
import com.wtbruh.fakelauncher.MainActivity;
import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.constants.SettingsConstants;
import com.wtbruh.fakelauncher.ui.fragment.phone.CameraFragment;
import com.wtbruh.fakelauncher.ui.fragment.phone.ContactsFragment;
import com.wtbruh.fakelauncher.ui.fragment.phone.DialerFragment;
import com.wtbruh.fakelauncher.ui.fragment.phone.GalleryFragment;
import com.wtbruh.fakelauncher.ui.fragment.phone.MessageFragment;
import com.wtbruh.fakelauncher.ui.fragment.phone.PasswordFragment;
import com.wtbruh.fakelauncher.ui.fragment.player.MusicPlayerFragment;
import com.wtbruh.fakelauncher.ui.widget.FitTextView;

import java.io.File;
import java.lang.reflect.Method;

public class UIHelper {
    private final static String TAG = UIHelper.class.getSimpleName();
    public final static int
            EXIT_METHOD_DPAD = 0,
            EXIT_METHOD_DIALER = 1,
            EXIT_METHOD_SETTINGS = 2;

    private static long activityLaunchLastTriggerTime = 0;
    private static final long DEBOUNCE_TIME = 300;

    public static final String STYLE_PHONE = "phone";
    public static final String STYLE_PLAYER = "player";

    public final static String APP_NAME = "name";
    public final static String APP_ICON_RES = "iconRes";
    public final static String TARGET_FRAGMENT = "target_fragment";
    public static Bundle[] getInternalAppList(String uiType) {
        if (uiType.equals(STYLE_PHONE)) {
            return new Bundle[] {
                    createAppListBundle(R.string.menu_call, R.drawable.ic_menu_call, DialerFragment.class.getName()),
                    createAppListBundle(R.string.menu_contact, R.drawable.ic_menu_contact, ContactsFragment.class.getName()),
                    createAppListBundle(R.string.menu_sms, R.drawable.ic_menu_sms, MessageFragment.class.getName()),
                    createAppListBundle(R.string.menu_camera, R.drawable.ic_menu_camera, CameraFragment.class.getName()),
                    createAppListBundle(R.string.menu_gallery, R.drawable.ic_menu_gallery, GalleryFragment.class.getName()),
                    createAppListBundle(R.string.menu_set, R.drawable.ic_menu_set, PasswordFragment.class.getName()),
            };
        } else if (uiType.equals(STYLE_PLAYER)) {
            return new Bundle[] {
                    createAppListBundle(R.string.menu_media, R.drawable.ic_menu_media, MusicPlayerFragment.class.getName()),
                    createAppListBundle(R.string.menu_set, R.drawable.ic_menu_set, PasswordFragment.class.getName()),
            };
        } else {
            Log.e(TAG, "Unknown UI type: " + uiType + ", Falling back to phone ui.");
            return getInternalAppList(UIHelper.STYLE_PHONE);
        }
    }

    private static Bundle createAppListBundle(int nameRes, int iconRes, String fragment) {
        Bundle bundle = new Bundle();
        bundle.putInt(APP_NAME, nameRes);
        bundle.putInt(APP_ICON_RES, iconRes);
        bundle.putString(TARGET_FRAGMENT, fragment);
        return bundle;
    }

    /**
     * Find fragment by its class name<br>
     * 通过类名查找Fragment
     * @param fragmentName Fragment的类名 | the class name of the fragment
     * @return Fragment对象 | Fragment object
     */
    public static Fragment findFragmentByName(String fragmentName) {
        return findFragmentByName(fragmentName, null);
    }

    /**
     * Find fragment which accepts args by its class name<br>
     * 通过类名查找支持传入参数的Fragment
     * @param fragmentName Fragment的类名 | the class name of the fragment
     * @param args 传入的参数 | the arguments you wanna pass to the fragment
     * @return Fragment对象 | Fragment object
     */
    public static Fragment findFragmentByName(String fragmentName, Bundle args) {
        Class<?> fragmentClass = null;
        if (fragmentName != null &&! fragmentName.isEmpty()) {
            try {
                fragmentClass = Class.forName(fragmentName);
                if (args != null && Fragment.class.isAssignableFrom(fragmentClass)) {
                    // Get newInstance(Bundle args) method
                    // 尝试获取newInstance(Bundle args)方法
                    Method newInstance = fragmentClass.getMethod("newInstance", Bundle.class);
                    newInstance.setAccessible(true);
                    return (Fragment) newInstance.invoke(fragmentClass, args);
                }
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "Got a non-existent class", e);
                return null;
            } catch (NoSuchMethodException e) {
                Log.e(TAG, "Args were given, but the class doesn't have newInstance() accepts parameters. Falling back to default newInstance()", e);
            } catch (Exception e) {
                Log.e(TAG, "Got unexpected error", e);
                return null;
            }
            // Get newInstance() method
            // 尝试获取newInstance()方法
            try {
                return (Fragment) fragmentClass.newInstance();
            } catch (Exception e) {
                Log.e(TAG, "Got unexpected error", e);
                return null;
            }
        }
        return null;
    }

    public static String getCurrentUIType(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getString(SettingsConstants.PREF_STYLE,
                context.getResources().getString(R.string.pref_style_default));
    }

    /**
     * <h3>Text editor | 文本内容编辑器</h3>
     * <p>Custom input method for scenes that require input,<br>
     * accepts back key, star key, pound key, and 0~9.</p>
     * 为程序内需要输入文本的场景而自定义的小输入法，<br>
     * 接受Back键、*#键和0~9键</p>
     *
     * @param keyCode 输入的键值 | the keycode user input
     * @param content 输入前文本框里的文本内容 | current content in TextView
     * @return 最终文本内容 | final content
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
     * @param context 上下文 | Context object
     * @param expected 预期的退出方式 | the exit method you expected
     * @return 如果与预期不符，返回false，否则返回true | If it does not match the expectations, return false, or return true
     */
    public static boolean checkExitMethod(Context context, int expected) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String[] valueArray = context.getResources().getStringArray(R.array.pref_exit_fakeui_method);
        String exitMethod = pref.getString(SettingsConstants.PREF_EXIT_FAKEUI_METHOD, valueArray[0]);
        return exitMethod.equals(valueArray[expected]);
    }

    /**
     * Simple intent maker | 简易Intent速成
     * @param activity 你的Activity对象 | the current activity
     * @param cls 要启动的Activity的class | the class object of the activity you wanna launch
     * @return intent
     */
    public static Intent makeIntent(Activity activity, Class<?> cls) {
        return new Intent().setClass(activity, cls);
    }

    /**
     * <h3>Intent Starter | Intent启动器</h3>
     * <p>Simple package for starting intent<br>
     * 启动intent的简单封装</p>
     *
     * @param activity 你的Activity对象 | the current activity
     * @param cls 要启动的Activity的class | the class object of the activity you wanna launch
     */
    public static void startIntent(Activity activity, Class<?> cls) {
        if (intentStarterDebounce(cls)) return;
        activity.startActivity(makeIntent(activity, cls));
        // Disable transition anim
        // 去掉过渡动画
        activity.overridePendingTransition(0, 0);
    }

    /**
     * Do exit now!!! | 现在给我退出软件！！！
     * @param activity 你的Activity对象 | the current activity
     */
    public static void doExit(Activity activity) {
        if (intentStarterDebounce(MainActivity.class)) return;
        activity.startActivity(
                makeIntent(activity, MainActivity.class)
                        .putExtra(MainActivity.EXTRA_EXIT, true)
        );
    }

    /**
     * Resize views with given scale<br>
     * 用所给比例缩放控件
     * @param scale 缩放比例 | zoom scale
     * @param view 需要调整的控件 | View that needs resize
     * @param fitWidthViews 需要在该控件完成缩放后适配宽度的TextView | TextViews that need fit its width after the view finished resizing
     */
    public static void resizeView(float scale, View view, FitTextView... fitWidthViews) {
        resizeViewWithCustomListener(scale, view,
                fitWidthViews.length >0? getFitWidthViewsListener(view, fitWidthViews): null);
    }

    /**
     * Do fitWidth() after resizing<br>
     * 用于在调整高度完毕后调用fitWidth()
     * @param view 需要调整的控件 | View that needs resize
     * @param fitWidthViews 需要在该控件完成缩放后适配宽度的TextView | TextViews that need fit its width after the view finished resizing
     */
    public static ViewTreeObserver.OnGlobalLayoutListener getFitWidthViewsListener(View view, FitTextView... fitWidthViews) {
        return new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                for (FitTextView fitWidthView : fitWidthViews) {
                    fitWidthView.fitWidth();
                }
            }
        };
    }

    /**
     * Resize views with given scale, custom listener will be triggered after finished resizing<br>
     * 用所给比例缩放控件，操作完成后会触发监听器
     * @param scale 缩放比例 | zoom scale
     * @param view 需要调整的控件 | View that needs resize
     * @param listener Listener | 监听器
     */
    public static void resizeViewWithCustomListener(float scale, View view, ViewTreeObserver.OnGlobalLayoutListener listener) {
        view.post(() -> {
            if (view.getLayoutParams().height > 0) {
                view.getLayoutParams().height = (int) (view.getHeight() * scale);
            }
            if (listener != null) view.getViewTreeObserver().addOnGlobalLayoutListener(listener);
            view.requestLayout();
        });
    }

    /**
     * <h3>Debounce for Intent Starter<br>
     * Intent启动器 防抖机制</h3>
     * <p>Prevent calling intentStarter too frequently<br>
     * 防止过于频繁地调用intentStarter</p>
     *
     * @param cls 要启动的Activity的class | the class object of the activity you wanna launch
     * @return true为调用过于频繁，false为调用频率正常 | true means too frequently, false means normal
     */
    public static boolean intentStarterDebounce(Class<?> cls) {
        // Only trigger intent starter at regularly intervals
        // 只在一定间隔时间内触发代码执行
        long currentTime = System.currentTimeMillis();
        if (currentTime - activityLaunchLastTriggerTime <= DEBOUNCE_TIME) return true;
        activityLaunchLastTriggerTime = currentTime;
        // If the activity is already on top, do not launch
        return ApplicationHelper.topActivity.contains(cls.getSimpleName());
    }

    /**
     * <h3>Method Call Debounce<br>
     * 防抖机制</h3>
     * <p>Prevent calling method too frequently<br>
     * 防止过于频繁地调用showDialog</p>
     *
     * @return true为调用过于频繁，false为调用频率正常 | true means too frequently, false means normal
     */
    public static boolean debounce(long lastTriggerTime, long expectedGap) {
        long currentTime = System.currentTimeMillis();
        return currentTime - lastTriggerTime <= expectedGap;
    }

    /**
     * <h3>Custom dialog | 自定义弹窗</h3>
     * <p>Imitate the style of dialog in feature phone<br>
     * 模仿老人机的弹窗样式</p>
     * @param context 上下文 | Context object
     * @param msgResId 要显示的文本的资源id | the resources id of message texts
     * @param listener 按键监听器 | key listener
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
        AlertDialog dialog = builder.setView(view).create();
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

    /**
     * Disable touchscreen through moving files in /dev/input<br>
     * 通过变动/dev/input内的文件来实现禁用触控
     * @param state true为启用触控，false为禁用触控 | true means enable touch, false means disable touch
     * @param context 上下文对象 | Context object
     */
    public static void setTouchscreenState(boolean state, Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        File dir = context.getFilesDir().getAbsoluteFile();
        String dirPath = dir.getPath();
        boolean isInputExists = !getTouchscreenState(context);
        String[] disable_cmd = {
                "cp -pr /dev/input/ " + dirPath,
                "rm $(getevent -pl 2>&1 | sed -n '/^add/{h}/ABS_MT_TOUCH/{x;s/[^/]*//p}')"
        };
        String[] enable_cmd = {
                "cp -pr " + dirPath + "/input /dev",
                "rm -rf " + dirPath + "/input"
        };
        if (state && isInputExists) {
            PrivilegeProvider.runCommand(PrivilegeProvider.PRIVILEGE_ROOT, enable_cmd);
        } else if (!state && !isInputExists && sp.getBoolean(SettingsConstants.PREF_ENHANCED_TOUCH_BLOCKING, false)) {
            PrivilegeProvider.runCommand(PrivilegeProvider.PRIVILEGE_ROOT, disable_cmd);
        }
    }

    /**
     * Get touchscreen state by checking if dir "input" exists in app private dir
     * 通过检查应用私有目录内是否存在目录"input"来判断触控禁用状态
     * @param context 上下文 | Context
     * @return true为工作中，false为已禁用 | true refers to working, false refers to disabled
     */
    public static boolean getTouchscreenState(Context context) {
        return !new File(context.getFilesDir().getAbsoluteFile(), "input").exists();
    }

    /**
     * Write taskId to ContentProvider to request screen pinning lock/unlock.
     * 向ContentProvider写入taskId以请求锁定/解锁屏幕固定。
     * <p>
     * On Xposed-enabled devices, {@code PinningHook} intercepts the ContentProvider
     * change and triggers {@code startSystemLockTaskMode}.<br>
     * 在已激活Xposed的设备上，PinningHook拦截ContentProvider变化并触发startSystemLockTaskMode。
     * <p>
     * On non-Xposed devices, {@link ApplicationHelper} has a ContentObserver that
     * calls the native {@code startLockTask()}/{@code stopLockTask()} APIs.<br>
     * 在无Xposed的设备上，ApplicationHelper的ContentObserver会调用原生startLockTask/stopLockTask。
     *
     * @param context Context
     * @param taskId  -1 to unlock, or the taskId to lock | -1解锁，其他值锁定
     */
    public static void setLockApp(Context context, int taskId) {
        ContentProvider.setTaskId(context, taskId);
    }

    /**
     * Read the current lock taskId from ContentProvider.
     * 从ContentProvider读取当前锁定的taskId。
     *
     * @param context Context
     * @return -1 if not locked, otherwise the pinned taskId | -1表示未锁定，其他值表示已锁定
     */
    public static int getLockApp(Context context) {
        return ContentProvider.getTaskId(context);
    }

}
