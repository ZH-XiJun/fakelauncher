package com.wtbruh.fakelauncher.xposed;

import android.content.Context;

import com.wtbruh.fakelauncher.utils.ContentProvider;
import com.wtbruh.fakelauncher.utils.HookHelper;

import de.robv.android.xposed.XposedBridge;

public class SelfHook extends HookHelper {
    private final static String TAG = SelfHook.class.getSimpleName();

    @Override
    public void init() {
        // Hook myself if I'm activated
        findAndHookMethod("com.wtbruh.fakelauncher.ApplicationHelper", "isXposedModuleActivated", new HookAction() {
            @Override
            protected void before(MethodHookParam param) {
                super.before(param);
                param.setResult(true);
            }
        });

        // Report the framework API version to the ContentProvider once the app starts,
        // so that both the app and other hooks can tell whether this module is active
        // -- and which API version it runs on -- without calling any Xposed API.
        // 应用启动时把框架 API 版本上报到 ContentProvider（KEY_XPOSED_API）：
        // 这样应用侧与其它 hook 不调用 Xposed API 也能知道模块是否激活、运行在哪个 API 版本。
        findAndHookMethod("com.wtbruh.fakelauncher.ApplicationHelper", "onCreate", new HookAction() {
            @Override
            protected void after(MethodHookParam param) {
                super.after(param);
                try {
                    Context context = (Context) param.thisObject;
                    if (context == null) {
                        logE(tag, "Failed to report Xposed version: context is null");
                        return;
                    }
                    int version = getFrameworkVersion();
                    ContentProvider.setXposedApi(context, version);
                    logI(tag, "Reported Xposed API version: " + version);
                } catch (Throwable e) {
                    logE(tag, "Failed to report Xposed API version: " + e);
                }
            }
        });
    }

    /**
     * Xposed 框架的 API 版本号。
     *
     * <p>只有在模块已激活的进程里才能调用（否则 XposedBridge 不存在）。
     */
    public static int getFrameworkVersion() {
        return XposedBridge.getXposedVersion();
    }
}
