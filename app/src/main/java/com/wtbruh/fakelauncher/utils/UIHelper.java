package com.wtbruh.fakelauncher.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
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
import androidx.core.content.ContextCompat;
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
import com.wtbruh.fakelauncher.ui.fragment.phone.ToolboxFragment;
import com.wtbruh.fakelauncher.ui.fragment.player.MusicPlayerFragment;
import com.wtbruh.fakelauncher.ui.widget.FitTextView;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class UIHelper {
    private final static String TAG = UIHelper.class.getSimpleName();
    public final static int
            EXIT_METHOD_DPAD = 0,
            EXIT_METHOD_DIALER = 1,
            EXIT_METHOD_SETTINGS = 2;

    private static long activityLaunchLastTriggerTime = 0;
    public static final long DEBOUNCE_TIME = 300;

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
                    createAppListBundle(R.string.menu_media, R.drawable.ic_menu_media, MusicPlayerFragment.class.getName()),
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
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_custom, null, false);

        // 设置消息文本
        FitTextView messageTv = view.findViewById(R.id.dialogMessage);
        messageTv.setText(msgResId);

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.Theme_FakeLauncher_Dialog);
        AlertDialog dialog = builder.setView(view).create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        // 布局完成后按文字实际宽度适配，让字尽可能大
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                messageTv.fitWidth();
            }
        });

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

    /**
     * Reset lock state to unlocked and force-broadcast it.<br>
     * 重置为未锁定并强制广播：用于进程启动/退出/崩溃重启，
     * 避免 hook 侧残留上一轮的锁定状态（hook 的状态不会随应用进程消失）。
     *
     * @param context Context
     */
    public static void resetLockApp(Context context) {
        ContentProvider.resetTaskId(context);
    }

    // ── 主菜单 / 工具箱自定义（已安装应用） ──────────────────────────────────────────

    public final static String EXT_APP_PKG = "pkg";
    public final static String EXT_APP_LABEL = "label";
    public final static String EXT_APP_ICON = "icon";

    /** 内置功能 + 工具箱入口 + 用户勾选的已安装应用（主菜单完整列表），设置始终排在最后 */
    public static Bundle[] getMainMenuAppList(Context context) {
        List<Bundle> list = new ArrayList<>();
        String uiType = getCurrentUIType(context);
        Bundle settingsBundle = null;
        for (Bundle b : getInternalAppList(uiType)) {
            // 抽出“设置”，最后追加，保证无论添加多少应用设置都在最后一个
            if (PasswordFragment.class.getName().equals(b.getString(TARGET_FRAGMENT))) {
                settingsBundle = b;
            } else {
                list.add(b);
            }
        }
        // 工具箱入口（二级菜单）
        list.add(createAppListBundle(R.string.menu_utils, R.drawable.ic_toolbox, ToolboxFragment.class.getName()));
        // 用户自定义的已安装应用
        list.addAll(getExternalAppBundles(context, SettingsConstants.PREF_MENU_EXTERNAL_APPS));
        // 设置放最后
        if (settingsBundle != null) list.add(settingsBundle);
        return list.toArray(new Bundle[0]);
    }

    /** 工具箱二级菜单：用户勾选的已安装应用 */
    public static Bundle[] getToolboxAppList(Context context) {
        List<Bundle> list = getExternalAppBundles(context, SettingsConstants.PREF_TOOLBOX_EXTERNAL_APPS);
        return list.toArray(new Bundle[0]);
    }

    /** 读取工具箱应用的重命名（pkg=新名; 格式）；无重命名返回 null */
    public static String getToolboxDisplayName(Context context, String pkg) {
        if (pkg == null || pkg.isEmpty()) return null;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String raw = sp.getString(SettingsConstants.PREF_TOOLBOX_DISPLAY_NAMES, "");
        if (raw == null) return null;
        for (String pair : raw.split(";")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(pkg)) {
                return pair.substring(eq + 1);
            }
        }
        return null;
    }

    /** 保存/清除工具箱应用重命名（name 为空则清除） */
    public static void setToolboxDisplayName(Context context, String pkg, String name) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        Map<String, String> map = new LinkedHashMap<>();
        String raw = sp.getString(SettingsConstants.PREF_TOOLBOX_DISPLAY_NAMES, "");
        if (raw != null) {
            for (String pair : raw.split(";")) {
                int eq = pair.indexOf('=');
                if (eq > 0) map.put(pair.substring(0, eq), pair.substring(eq + 1));
            }
        }
        if (name == null || name.trim().isEmpty()) map.remove(pkg);
        else map.put(pkg, name.trim());
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (sb.length() > 0) sb.append(";");
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        sp.edit().putString(SettingsConstants.PREF_TOOLBOX_DISPLAY_NAMES, sb.toString()).apply();
    }

    /** 保存工具箱应用完整顺序（排序后调用） */
    public static void saveToolboxOrder(Context context, List<String> orderedPkgs) {
        saveAppSelection(context, SettingsConstants.PREF_TOOLBOX_EXTERNAL_APPS, orderedPkgs);
    }

    /** 从 SharedPreferences 读取用户勾选的外部应用，组装成菜单 Bundle 列表 */
    private static List<Bundle> getExternalAppBundles(Context context, String prefKey) {
        List<Bundle> result = new ArrayList<>();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String raw = sp.getString(prefKey, "");
        if (raw == null || raw.isEmpty()) return result;
        PackageManager pm = context.getPackageManager();
        for (String pkg : raw.split(";")) {
            if (pkg == null || pkg.isEmpty()) continue;
            String label = pkg;
            try {
                label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
            } catch (Exception ignored) { }
            // 重命名覆盖：伪装界面显示用户自定义名称
            String renamed = getToolboxDisplayName(context, pkg);
            if (renamed != null && !renamed.isEmpty()) label = renamed;
            Bundle b = new Bundle();
            b.putString(EXT_APP_PKG, pkg);
            b.putString(EXT_APP_LABEL, label);
            b.putInt(EXT_APP_ICON, getPresetAppIcon(pkg));
            result.add(b);
        }
        return result;
    }

    /** 列出设备上所有可启动的第三方应用（含名称与预设图标） */
    public static List<Bundle> getInstalledLauncherApps(Context context) {
        List<Bundle> result = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        try {
            for (ResolveInfo ri : pm.queryIntentActivities(mainIntent, 0)) {
                String pkg = ri.activityInfo != null ? ri.activityInfo.packageName : null;
                if (pkg == null || pkg.isEmpty() || pkg.equals(context.getPackageName())) continue;
                String label = ri.loadLabel(pm).toString();
                if (label == null || label.isEmpty()) label = pkg;
                Bundle b = new Bundle();
                b.putString(EXT_APP_PKG, pkg);
                b.putString(EXT_APP_LABEL, label);
                b.putInt(EXT_APP_ICON, getPresetAppIcon(pkg));
                result.add(b);
            }
        } catch (Exception e) {
            Log.e(TAG, "query launcher apps failed", e);
        }
        return result;
    }

    /** 获取已安装应用的真实图标（高清渲染）；未安装/异常时回退通用图标 */
    public static Drawable getExternalAppIcon(Context context, String pkg) {
        try {
            if (pkg == null) return ContextCompat.getDrawable(context, R.drawable.ic_menu_app_generic);
            Drawable d = null;
            // 优先取 xxxhdpi 高清图标源（应用图标按 density 变体存储，getApplicationIcon 只给当前 density）
            try {
                ApplicationInfo info = context.getPackageManager().getApplicationInfo(pkg, 0);
                if (info.icon != 0) {
                    android.content.res.Resources res =
                            context.getPackageManager().getResourcesForApplication(info);
                    d = res.getDrawableForDensity(info.icon, DisplayMetrics.DENSITY_XXXHIGH);
                }
            } catch (Exception ignored) { }
            if (d == null) d = context.getPackageManager().getApplicationIcon(pkg);
            // 渲染到 120dp 分辨率位图，避免主菜单 200dp 大图标放大模糊
            int size = (int) (context.getResources().getDisplayMetrics().density * 120);
            Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            d.setBounds(0, 0, size, size);
            d.draw(canvas);
            return new BitmapDrawable(context.getResources(), bmp);
        } catch (Exception e) {
            Log.e(TAG, "getExternalAppIcon failed: " + pkg, e);
            return ContextCompat.getDrawable(context, R.drawable.ic_menu_app_generic);
        }
    }

    /** 预设功能机风格图标（按常见包名映射，未命中用通用图标） */
    public static int getPresetAppIcon(String pkg) {
        if (pkg == null) return R.drawable.ic_menu_app_generic;
        switch (pkg) {
            case "com.tencent.mm": return R.drawable.ic_menu_app_wechat;
            case "com.tencent.mobileqq": return R.drawable.ic_menu_app_qq;
            case "com.eg.android.AlipayGphone": return R.drawable.ic_menu_app_alipay;
            case "com.taobao.taobao": return R.drawable.ic_menu_app_taobao;
            case "com.jingdong.app.mall": return R.drawable.ic_menu_app_jd;
            case "com.ss.android.ugc.aweme": return R.drawable.ic_menu_app_douyin;
            case "com.smile.gifmaker": return R.drawable.ic_menu_app_kuaishou;
            case "com.sina.weibo": return R.drawable.ic_menu_app_weibo;
            case "com.xunmeng.pinduoduo": return R.drawable.ic_menu_app_pdd;
            case "com.sankuai.meituan": return R.drawable.ic_menu_app_meituan;
            case "tv.danmaku.bili": return R.drawable.ic_menu_app_bilibili;
            case "com.autonavi.minimap": return R.drawable.ic_menu_app_amap;
            default: return R.drawable.ic_menu_app_generic;
        }
    }

    /** 判断一个 Bundle 是否为外部应用（含包名） */
    public static boolean isExternalApp(Bundle appInfo) {
        return appInfo != null && appInfo.getString(EXT_APP_PKG) != null;
    }

    /** 启动外部应用；若处于屏幕固定锁定状态，先解锁再启动 */
    public static void launchExternalApp(Context context, String pkg) {
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(pkg);
            if (intent == null) {
                showCustomDialog(context, R.string.app_not_found, null);
                return;
            }
            if (getLockApp(context) != ContentProvider.NOT_SET) {
                setLockApp(context, ContentProvider.NOT_SET);
                // 同步解除屏幕固定：ContentObserver 解除是异步的，若不等它执行完，
                // 系统仍处于 lock task mode，startActivity 外部应用会被系统拦截
                // （表现为工具箱"按两下才启动"）。
                if (context instanceof Activity) {
                    try {
                        ((Activity) context).stopLockTask();
                    } catch (Throwable ignored) {
                    }
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "launchExternalApp failed: " + pkg, e);
        }
    }

    /** 保存用户勾选的应用包名列表（分号分隔存入偏好） */
    public static void saveAppSelection(Context context, String prefKey, List<String> pkgs) {
        StringBuilder sb = new StringBuilder();
        for (String p : pkgs) {
            if (p == null || p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(';');
            sb.append(p);
        }
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putString(prefKey, sb.toString()).apply();
    }

    /** 读取用户勾选的应用包名集合（保持添加顺序） */
    public static LinkedHashSet<String> loadAppSelection(Context context, String prefKey) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        String raw = PreferenceManager.getDefaultSharedPreferences(context).getString(prefKey, "");
        if (raw != null) {
            for (String p : raw.split(";")) {
                if (!p.isEmpty()) set.add(p);
            }
        }
        return set;
    }

}
