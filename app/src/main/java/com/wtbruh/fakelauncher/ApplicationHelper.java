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
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.tencent.mmkv.MMKV;
import com.rosan.dhizuku.api.Dhizuku;
import com.wtbruh.fakelauncher.receiver.DeviceAdminReceiver;
import com.wtbruh.fakelauncher.service.NotificationListenerService;
import com.wtbruh.fakelauncher.utils.ContentProvider;
import com.wtbruh.fakelauncher.utils.CrashHandler;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;

import java.lang.ref.WeakReference;

public class ApplicationHelper extends Application {

    public static String topActivity;
    private final static String TAG = ApplicationHelper.class.getSimpleName();
    /**
     * 进程内的 Application 单例，供静态方法（{@link #isXposedModuleActivated()}）取 Context 用。
     * Application context 与应用进程同生命周期，静态持有不会泄漏。
     */
    private static volatile ApplicationHelper sInstance;
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

            if (taskId != ContentProvider.NOT_SET && activity != null && !activity.isFinishing()) {
                // 锁定
                try {
                    if (mDpm != null) {
                        ComponentName receiver = switch (mDeviceAdminType) {
                            case PrivilegeProvider.DHIZUKU -> Dhizuku.getOwnerComponent();
                            default -> new ComponentName(ApplicationHelper.this, DeviceAdminReceiver.class);
                        };
                        mDpm.setLockTaskPackages(receiver, new String[]{BuildConfig.APPLICATION_ID});
                    }
                } catch (Throwable e) {
                    // 本方法运行在 ContentResolver.notifyChange 的调用栈里，
                    // 抛出会把写入方（MainActivity / CrashHandler）一起干掉，必须兜住。
                    Log.e(TAG, "setLockTaskPackages failed", e);
                }
                try {
                    activity.startLockTask();
                } catch (Exception e) {
                    Log.e(TAG, "startLockTask failed", e);
                }
            } else if (taskId == ContentProvider.NOT_SET && activity != null && !activity.isFinishing()) {
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
        sInstance = this;
        // 初始化设备管理权限
        initDeviceOwner();

        // 注册 ContentObserver，监听 taskId 变化
        getContentResolver().registerContentObserver(
                ContentProvider.CONTENT_URI, true, mLockTaskObserver);

        // MMKV init
        MMKV.initialize(this);

        // Broadcast a definite state at process start.
        // 进程启动时广播确定态：新进程必然是未锁定，而 Xposed 侧可能还残留上一轮的旧值
        // （应用进程死掉时它收不到任何通知），不纠正就会“应用以为没锁、hook 以为锁着”。
        ContentProvider.resetTaskId(this);

        Log.d(TAG, "Application onCreate. My package name: "+ BuildConfig.APPLICATION_ID);

        // startAllServices();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {

            }

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
            public void onActivityStopped(@NonNull Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {}
        });

        new CrashHandler().init(this);
    }

    // ── 设备管理权限初始化 ──────────────────────────────────────────────────────────────────────────────────

    /**
     * Xposed 模块自检测。
     *
     * <p>数据源是 ContentProvider 的 {@link ContentProvider#KEY_XPOSED_API}：SelfHook 在应用启动时
     * （{@code ApplicationHelper.onCreate} 的 after）把 {@code XposedBridge.getXposedVersion()}
     * 上报进去。这样应用侧既不用调用任何 Xposed API，也不用再靠 hook 改本方法的返回值
     * —— 与 XposedPreference / 权限页天然同源。
     *
     * <p>注意：上报发生在 onCreate 返回之后，所以本方法在 onCreate 内部调用会得到 false，
     * 这是预期行为（那时还没上报）。
     *
     * @return 已上报过有效 API 版本（模块已激活）返回 true
     */
    public static boolean isXposedModuleActivated() {
        ApplicationHelper app = sInstance;
        if (app == null) return false;
        return ContentProvider.getXposedApi(app) > 0;
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
