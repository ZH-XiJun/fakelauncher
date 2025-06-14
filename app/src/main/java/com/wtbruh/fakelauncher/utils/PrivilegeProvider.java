package com.wtbruh.fakelauncher.utils;

import static androidx.core.app.ActivityCompat.requestPermissions;

import android.Manifest;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener;
import com.wtbruh.fakelauncher.MainActivity;
import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.ui.settings.SubSettingsFragment;
import com.wtbruh.fakelauncher.receiver.DeviceAdminReceiver;

import rikka.shizuku.Shizuku;
import rikka.sui.Sui;

/**
 *
 * Privilege provider
 * <p>
 * 提权管理
 *
 * @author ZH-XiJun
 *
 */

public class PrivilegeProvider {
    private final static String TAG = PrivilegeProvider.class.getSimpleName();
    // Privilege type to int
    public final static int DEACTIVATED = 0;
    public final static int DHIZUKU = 1;
    public final static int DEVICE_OWNER = 2;
    public final static int DEVICE_ADMIN = 3;
    // Run method to int
    public final static int METHOD_NORMAL = 0;
    public final static int METHOD_ROOT = 1;
    public final static int METHOD_SHIZUKU = 2;
    // Command define
    public final static String CMD_SU = "su";
    public final static String CMD_BUSYBOX = "busybox";
    public final static String CMD_SH = "sh";
    public final static String CMD_SH_FULL = "/system/bin/sh";
    public final static String CMD_RISH= "rish";

    public final static int PERMISSION_REQUEST_CODE = 123;

    /**
     * Check if permission has been granted<br>
     * 检查是否已获得权限
     *
     * @param context Activity的上下文数据
     * @param item 要请求的权限
     * @return true或false
     */
    public static boolean CheckPermission(Context context, String item) {
        if (item.equals(Manifest.permission.WRITE_SETTINGS)) {
            return Settings.System.canWrite(context);
        }
        PackageManager pm = context.getPackageManager();
        int result = pm.checkPermission(item, context.getPackageName());
        return result == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request permission<br>
     * 请求权限
     *
     * @param activity Activity对象
     * @param item 要请求的权限
     */
    public static void requestPermission(Activity activity, String item) {
        switch (item) {
            case Manifest.permission.WRITE_SETTINGS: // WRITE_SETTINGS 修改系统设置权限
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:" + activity.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
                break;
            default:
                requestPermissions(activity, new String[] {item}, PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Add prefix for command<br>
     * 为指令添加前缀
     *
     * @param method 要添加什么样的前缀
     * @param cmd 你的指令（字符串数组）
     * @return 添加了前缀的指令（字符串数组）
     */
    private static String[] prefix(int method, String[] cmd) {
        String[] prefix;
        switch (method) {
            case METHOD_ROOT:
                prefix = new String[]{CMD_SU, "-c"};
                break;
            // case METHOD_SHIZUKU:
                // wip
            case METHOD_NORMAL:
            default:
                return cmd;
        }

        String[] result = new String[cmd.length + prefix.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(cmd, 0, result, prefix.length, cmd.length);
        return result;
    }

    /**
     * Check privilege 检查特殊权限
     *
     * @param str 权限名
     * @return 是否已获得授权
     */
    public static boolean checkPrivilege(String str) {
        switch (str) {
            case "Root":
                try {
                    Log.d(TAG, "Checking root permission");
                    return runCmd(new String[]{"id"}, METHOD_ROOT) == 0;
                } catch (RuntimeException e) {
                    return false;
                }
            case "Shizuku":
                if (Sui.init("com.wtbruh.fakelauncher")) Log.d(TAG, "Sui is available");
                if (Shizuku.isPreV11()) {
                    // Pre-v11 is unsupported
                    return false;
                }
                try {
                    if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                        // Granted
                        return true;
                    } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                        // Users choose "Deny and don't ask again"
                        return false;
                    } else {
                        // Request the permission
                        Shizuku.requestPermission(114514);
                        return false;
                    }
                } catch (IllegalStateException e){
                    return false;
                }
            default:
                return false;
        }
    }
    /**
     * Check device owner 检查设备所有者权限
     * @param context 应用上下文
     * @return 特权对应的resource id
     */
    public static int checkDeviceAdmin(Context context) {
        boolean dhizuku = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(SubSettingsFragment.PREF_ENABLE_DHIZUKU, false);

        if (dhizuku) {
            Log.d(TAG, "Requesting dhizuku");
            if (!Dhizuku.init(context)) return DEACTIVATED;
            if (Dhizuku.isPermissionGranted()) return DHIZUKU;
            if (MainActivity.getLockApp(context) != -1 ) {
                Toast.makeText(context, R.string.toast_open_settings_from_launcher, Toast.LENGTH_LONG).show();
                return DEACTIVATED;
            }
            Dhizuku.requestPermission(new DhizukuRequestPermissionListener() {
                @Override
                public void onRequestPermission(int grantResult){
                }
            });

        } else {
            Log.d(TAG, "Requesting DeviceOwner/DeviceAdmin");
            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (dpm.isDeviceOwnerApp(context.getPackageName())) return DEVICE_OWNER;
            else if (dpm.isAdminActive(new ComponentName(context, DeviceAdminReceiver.class))) return DEVICE_ADMIN;
        }
        return DEACTIVATED;
    }

    /**
     *
     * Single command running<br>
     * 单条指令运行
     *
     * @param cmd 指令（字符串数组）
     * @param method 想以什么权限运行（会调用prefix方法添加对应前缀）
     * @return 指令运行完成的返回值
     */
    public static int runCmd(String[] cmd, int method){
        ProcessBuilder psb = new ProcessBuilder();
        psb.command(prefix(method, cmd));
        try {
            Process ps = psb.start();
            return ps.waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
