package com.wtbruh.fakelauncher;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wtbruh.fakelauncher.service.NotificationListenerService;

public class ApplicationHelper extends Application {

    public static String topActivity;
    private final static String TAG = ApplicationHelper.class.getSimpleName();
    public final static String PACKAGE_NAME = "com.wtbruh.fakelauncher"; // Static reference

    @Override
    public void onCreate() {
        super.onCreate();
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
            public void onActivityDestroyed(@NonNull Activity activity) {

            }
        });
    }

    /**
     * 启动所有服务。 Start all services.
     */
    public void startAllServices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //8.0以上系统启动为前台服务, 否则在后台, 测试中发现过几分钟后MediaController监听不到音乐信息
            startForegroundService(new Intent(this, NotificationListenerService.class));
        } else {
            startService(new Intent(this, NotificationListenerService.class));
        }
    }

    public ComponentName getComponentName(Class<?> cls) {
        return new ComponentName(getPackageName(), cls.toString());
    }

    public String getAppPackageName() {
        return getPackageName();
    }
}
