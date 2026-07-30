package com.wtbruh.fakelauncher;

import android.app.Activity;
import android.app.Application;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.rosan.dhizuku.api.Dhizuku;
import com.wtbruh.fakelauncher.receiver.DeviceAdminReceiver;
import com.wtbruh.fakelauncher.service.NotificationListenerService;
import com.wtbruh.fakelauncher.utils.ContentProvider;
import com.wtbruh.fakelauncher.utils.CrashHandler;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;

import java.lang.ref.WeakReference;

public class ApplicationHelper extends Application {

    public static String topActivity;
    public static volatile boolean dialing = false;
    public static volatile String fakeCallNumber = "";
    private final static String TAG = ApplicationHelper.class.getSimpleName();
    /** 当前前台 Activity（WeakReference 避免泄漏）*/
    private static WeakReference<Activity> sCurrentActivity = new WeakReference<>(null);

    /** 设备管理权限等级 */
    private int mDeviceAdminType = PrivilegeProvider.DEACTIVATED;
    /** 设备策略管理器 */
    private DevicePolicyManager mDpm;

    // ── ContentObserver：监听 ContentProvider taskId 变化，非 Xposed 下触发原生 LockTask ─────────────────

    private final ContentObserver mLockTaskObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (isXposedModuleActivated()) return;

            int taskId = ContentProvider.getTaskId(ApplicationHelper.this);
            Activity activity = sCurrentActivity.get();

            if (taskId != -1 && activity != null && !activity.isFinishing()) {
                // 锁定
                if (mDpm != null) {
                    ComponentName receiver = switch (mDeviceAdminType) {
                        case PrivilegeProvider.DHIZUKU -> Dhizuku.getOwnerComponent();
                        default -> new ComponentName(ApplicationHelper.this, DeviceAdminReceiver.class);
                    };
                    mDpm.setLockTaskPackages(receiver, new String[]{BuildConfig.APPLICATION_ID});
                }
                try {
                    activity.startLockTask();
                } catch (Exception e) {
                    Log.e(TAG, "startLockTask failed", e);
                }
            } else if (taskId == -1 && activity != null && !activity.isFinishing()) {
                // 解锁
                try {
                    activity.stopLockTask();
                } catch (Exception e) {
                    Log.e(TAG, "stopLockTask failed", e);
                }
            }
        }
    };

    // ── onCreate ──────────────────────────────────────────────────────────────────────────────────────────

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化设备管理权限
        initDeviceOwner();

        // 注册 ContentObserver，监听 taskId 变化
        getContentResolver().registerContentObserver(
                ContentProvider.CONTENT_URI, true, mLockTaskObserver);

        // 生命周期回调：追踪前台 Activity
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

            @Override
            public void onActivityStarted(@NonNull Activity activity) {}

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                topActivity = activity.toString();
                sCurrentActivity = new WeakReference<>(activity);
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                topActivity = "";
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {}

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {}
        });

        new CrashHandler().init(this);
    }

    // ── 设备管理权限初始化 ──────────────────────────────────────────────────────────────────────────────────

    /**
     * Xposed 模块自检测
     * @return 如果已激活，返回结果会被hook修改为true
     */
    public static boolean isXposedModuleActivated() {
        return false;
    }

    /**
     * Check privilege level and prepare DevicePolicyManager.
     */
    private void initDeviceOwner() {
        mDeviceAdminType = PrivilegeProvider.checkDeviceAdmin(this);
        mDpm = switch (mDeviceAdminType) {
            case PrivilegeProvider.DHIZUKU -> PrivilegeProvider.binderWrapperDevicePolicyManager(this);
            case PrivilegeProvider.DEVICE_OWNER ->
                    ContextCompat.getSystemService(this, DevicePolicyManager.class);
            default -> null;
        };
    }

    // ── Services ──────────────────────────────────────────────────────────────────────────────────────────

    /**
     * 启动所有服务。 Start all services.
     */
    public void startAllServices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, NotificationListenerService.class));
        } else {
            startService(new Intent(this, NotificationListenerService.class));
        }
    }

    /**
     * Get the current foreground Activity, or null.
     */
    public static Activity getCurrentActivity() {
        return sCurrentActivity.get();
    }

    public ComponentName getComponentName(Class<?> cls) {
        return new ComponentName(getPackageName(), cls.toString());
    }

    public String getAppPackageName() {
        return getPackageName();
    }
}
