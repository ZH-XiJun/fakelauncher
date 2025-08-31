package com.wtbruh.fakelauncher.utils;

import static androidx.core.app.ActivityCompat.requestPermissions;
import static androidx.core.content.ContextCompat.getString;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.app.admin.IDevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuBinderWrapper;
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener;
import com.wtbruh.fakelauncher.IUserService;
import com.wtbruh.fakelauncher.MainActivity;
import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.service.UserService;
import com.wtbruh.fakelauncher.ui.fragment.settings.SubSettingsFragment;
import com.wtbruh.fakelauncher.receiver.DeviceAdminReceiver;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import rikka.shizuku.Shizuku;
import rikka.sui.Sui;

/**
 *
 * Permission/Privilege provider<br>
 * 权限/特权 工具类
 *
 * @author ZH-XiJun
 */

public class PrivilegeProvider {
    private final static String TAG = PrivilegeProvider.class.getSimpleName();
    // Device admin level to int
    // 设备管理员等级
    public final static int DEACTIVATED = 0;
    public final static int DHIZUKU = 1;
    public final static int DEVICE_OWNER = 2;
    public final static int DEVICE_ADMIN = 3;
    // Operate method to int
    // 执行操作所需权限
    public final static int PRIVILEGE_NORMAL = 4;
    public final static int PRIVILEGE_ROOT = 5;
    public final static int PRIVILEGE_SHIZUKU = 6;
    // Command define
    // 固定指令
    public final static String CMD_SU = "su";
    public final static String CMD_SH = "sh";
    // Result code for handling and recognizing command output
    public final static String CMD_RESULT_CODE = "c";
    public final static String CMD_STDOUT = "o";
    public final static String CMD_STDERR = "e";

    public final static int PERMISSION_REQUEST_CODE = 123;

    /**
     * Check if permission has been granted<br>
     * 检查是否已获得权限
     *
     * @param context Activity的上下文数据
     * @param item 要请求的权限
     * @return true或false
     */
    public static boolean checkPermission(Context context, String item) {
        if (item.equals(Manifest.permission.WRITE_SETTINGS) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
     * @param activity Activity对象 | Activity object
     * @param item 要请求的权限，可以多个 | Permissions to request. Allow multiple
     */
    public static void requestPermission(Activity activity, String... item) {
        requestPermissions(activity, item, PERMISSION_REQUEST_CODE);
    }

    /**
     * Get all permissions declared by program in AndroidManifest.xml<br>
     * 获取App在 AndroidManifest.xml 内声明的所有权限<br>
     *
     * @param context 上下文 | Context
     * @return 所有权限组成的字符串数组 | An string array made up of all permissions
     */
    public static String[] getAllPermissions(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] raw = packageInfo.requestedPermissions;

            if (raw == null) return new String[]{};

            ArrayList<String> arrayList = new ArrayList<>();
            for (String permission: raw) {
                // Filter out permissions defined by Android, other program defined permissions are not required
                // 筛选出Android的权限，其他程序定义的权限不需要
                if (permission.contains("android.permission.")) {
                    arrayList.add(permission);
                }
            }

            return arrayList.toArray(new String[0]);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error on getting permissions: "+e);
            return new String[]{};
        }
    }

    /**
     * Request all permissions declared in AndroidManifest.xml<br>
     * 请求AndroidManifest.xml声明的所有权限
     * @param activity Activity对象 | Activity object
     * @param method 请求方式（请求所需的特权） | request method (Privilege the operation needs)
     */
    public static void requestAllPermissions(Activity activity, int method) {
        if (method == PRIVILEGE_NORMAL) {
            requestPermissions(activity, getAllPermissions(activity), PERMISSION_REQUEST_CODE);
            return;
        }
        ArrayList<String> arrayList = new ArrayList<>();
        for (String permission: getAllPermissions(activity)) {
            arrayList.add("pm grant com.wtbruh.fakelauncher " + permission);
        }
        runCommand(activity, method, arrayList.toArray(new String[0]));
    }

    /**
     * Args for launching UserService defined by Shizuku<br>
     * 用于启动Shizuku UserService的参数
     *
     * @param context 上下文 | Context
     * @return ShizukuUserServiceArgs
     */
    public static Shizuku.UserServiceArgs getShizukuUserServiceArgs(Context context) {
        return new Shizuku.UserServiceArgs(new ComponentName(context, UserService.class))
                .daemon(false)
                .processNameSuffix("shizuku-service")
                .debuggable(false)
                .version(1);
    }

    /**
     * Do operation using Shizuku privilege<br>
     * 使用Shizuku权限进行操作
     *
     * @param context 上下文 | Context
     * @param action 操作 | Actions
     */
    public static void useShizuku(Context context, ShizukuAction action) {
        // Shizuku only support Android 6+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder iBinder) {
                Log.d(TAG, "Successfully connected to UserService");
                action.invoke(iBinder, this);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "Disconnected from UserService");
            }
        };
        if (checkPrivilege(PRIVILEGE_SHIZUKU)) {
            Shizuku.UserServiceArgs args = getShizukuUserServiceArgs(context);
            Shizuku.bindUserService(args, connection);
        }
    }

    /**
     * Get DPM using Dhizuku privilege<br>
     * 使用Dhizuku权限获取DPM
     *
     * @param appContext Context | 上下文
     * @return DPM 对象 | DPM object
     */
    @SuppressLint("PrivateApi")
    public static DevicePolicyManager binderWrapperDevicePolicyManager(Context appContext) {
        try {
            Context context = appContext.createPackageContext(Dhizuku.getOwnerPackageName(), Context.CONTEXT_IGNORE_SECURITY);
            DevicePolicyManager manager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            Field field = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // need HiddenApiBypass
                List<Field> fields = HiddenApiBypass.getInstanceFields(manager.getClass());
                int index = 0;
                while (index <= fields.size()) {
                    Field f = fields.get(index);
                    if (f.getName().equals("mService")) {
                        field = f;
                        break;
                    }
                    index++;
                }
            } else { // Common reflect operation
                field = manager.getClass().getDeclaredField("mService");
            }
            if (field == null) throw new NoSuchFieldException("unable to get field \"mService\"");
            field.setAccessible(true);
            IDevicePolicyManager oldInterface = (IDevicePolicyManager) field.get(manager);
            if (oldInterface instanceof DhizukuBinderWrapper) return manager;
            if (oldInterface == null) throw new NullPointerException("Got null IDevicePolicyManager");
            IBinder oldBinder = oldInterface.asBinder();
            IBinder newBinder = Dhizuku.binderWrapper(oldBinder);
            IDevicePolicyManager newInterface = IDevicePolicyManager.Stub.asInterface(newBinder);
            field.set(manager, newInterface);
            return manager;
        } catch (Exception e) {
            Log.e(TAG, "Error on wrapping dpm: "+e);
        }
        return null;
    }

    /**
     * Get current privilege provider<br>
     * 获取当前授权器
     * @param context Context
     * @return 以int形式的特权类型 | privilege type in int
     */
    public static int getCurrentPrivilegeProvider(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return privilegeToInt(
                sp.getString(SubSettingsFragment.PREF_PRIVILEGE_PROVIDER,
                getString(context, R.string.pref_privilege_provider_default))
        );

    }
    
    /**
     * Check privilege | 检查特殊权限
     *
     * @param privilege 特殊权限类型 | Privilege type
     * @return 是否已获得授权 | Is permission granted
     */
    public static boolean checkPrivilege(int privilege) {
        switch (privilege) {
            case PRIVILEGE_ROOT -> {
                try {
                    Log.d(TAG, "Checking root permission");
                    Bundle bundle = runCommand(PRIVILEGE_ROOT, "id");
                    return bundle.getInt(CMD_RESULT_CODE) == 0;
                } catch (RuntimeException e) {
                    return false;
                }
            }
            case PRIVILEGE_SHIZUKU -> {
                // Shizuku requires Android 6+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
                    } catch (IllegalStateException e) {
                        return false;
                    }
                } else return false;
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * In convenient of conversion between SharedPreference data and constants defined in PrivilegeProvider<br>
     * 将SharedPreference获取到的字符串数据转换为便于PrivilegeProvider识别的常量
     *
     * @param str 字符串数据 | String data
     * @return 对应的常量 | Corresponding constant
     */
    public static int privilegeToInt(String str) {
        return switch (str) {
            case "Root" -> PRIVILEGE_ROOT;
            case "Shizuku" -> PRIVILEGE_SHIZUKU;
            default -> PRIVILEGE_NORMAL;
        };
    }

    /**
     * Check device owner 检查设备所有者权限
     * @param context 上下文 | Context
     * @return 设备管理员权限级别对应的常量 | Constant corresponding to device admin level
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

    public static void requestDeviceOwner(Context context, int method) {
        String cmd = getDeviceOwnerActiveCmd(context);
        runCommand(context, method, cmd);
    }

    public static String getDeviceOwnerActiveCmd(Context context) {
        return "dpm set-device-owner " + new ComponentName(context.getPackageName(), DeviceAdminReceiver.class.getName()).flattenToShortString();
    }

    /**
     *
     * <h3>Multiple command running</h3>
     * <h3>多条指令运行</h3>
     *
     * @param context 上下文 | Context
     * @param method 想以什么权限运行 | Privilege type
     * @param cmd 指令 | Commands
     * @return 指令运行后的输出数据 | Command output data
     */
    public static Bundle runCommand(Context context, int method, String... cmd) {
        if (method == PRIVILEGE_SHIZUKU && context != null) {
            AtomicReference<Bundle> bundle = new AtomicReference<>();
            useShizuku(context, (iBinder, connection) -> {
                try {
                    bundle.set(IUserService.Stub.asInterface(iBinder).runMultiCmd(cmd));
                    Shizuku.unbindUserService(getShizukuUserServiceArgs(context), connection, true);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            });
            return bundle.get();
        }

        String s;
        if (method == PRIVILEGE_ROOT) {
            s = CMD_SU;
        } else {
            s = CMD_SH;
        }
        try {
            Process ps = Runtime.getRuntime().exec(s);
            DataOutputStream o = new DataOutputStream(ps.getOutputStream());
            for (String command : cmd) {
                o.writeBytes(command + "\n");
            }
            o.writeBytes("exit\n");
            o.flush();
            o.close();

            return processOutput(ps, "gbk");
        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "An error occurred when executing commands: ",e);
            Bundle bundle = new Bundle();
            bundle.putString(CMD_STDOUT, "");
            bundle.putString(CMD_STDERR, Arrays.toString(e.getStackTrace()));
            bundle.putInt(CMD_RESULT_CODE, 1);
            return bundle;
        }
    }

    /**
     *
     * <h3>Multiple command running</h3>
     * <h3>多条指令运行</h3>
     * <h5>If need Shizuku support, context is required.</h5>
     * <h5>如要调用Shizuku，还需要上下文！</h5>
     *
     * @param method 想以什么权限运行 | Privilege type
     * @param cmds 多条指令 | Commands
     * @return 指令运行后的输出数据 | Command output data
     */
    public static Bundle runCommand(int method, String... cmds) {
        return runCommand(null, method, cmds);
    }

    /**
     * Pack output data into a bundle<br>
     * 将输出数据打包成一个bundle
     *
     * @param process 进程对象 | Process object
     * @param charsetName 指定文本编码（如utf-8，gbk）| Charset (utf-8, gbk, etc.)
     * @return 打包成bundle的数据 | Data packaged into bundle
     * @throws IOException InputStream抛出来的，我不到啊 | Thrown by InputStream
     * @throws InterruptedException Process抛出来的，我补刀啊 | Thrown by Process
     */
    public static Bundle processOutput(Process process, String charsetName) throws IOException, InterruptedException {
        BufferedReader i = new BufferedReader(new InputStreamReader(process.getInputStream(), charsetName));
        BufferedReader e = new BufferedReader(new InputStreamReader(process.getErrorStream(), charsetName));

        StringBuilder sb = new StringBuilder(); // 看什么看？sb骂的就是你
        String line;

        while ((line = i.readLine()) != null) {
            sb.append(line).append("\n");
        }
        String outputData = sb.toString();

        sb.setLength(0);

        while ((line = e.readLine()) != null) {
            sb.append(line).append("\n");
        }
        String errorData = sb.toString();

        i.close();
        e.close();

        Bundle bundle = new Bundle();
        bundle.putString(CMD_STDOUT, outputData);
        bundle.putString(CMD_STDERR, errorData);
        bundle.putInt(CMD_RESULT_CODE, process.waitFor());
        return bundle;
    }

    public interface ShizukuAction {
        void invoke(IBinder binder, ServiceConnection connection);
    }
}
