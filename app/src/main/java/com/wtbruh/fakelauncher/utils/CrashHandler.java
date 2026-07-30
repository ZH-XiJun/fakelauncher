package com.wtbruh.fakelauncher.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.wtbruh.fakelauncher.CrashLogActivity;
import com.wtbruh.fakelauncher.SplashActivity;
import com.wtbruh.fakelauncher.constants.SettingsConstants;

import java.io.PrintWriter;
import java.io.StringWriter;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private Context mContext;
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    private final static String TAG = CrashHandler.class.getSimpleName();

    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
        // 这里只处理后台线程崩溃（主线已经被 LooperGuard 兜住了）
        Log.d(TAG, "Uncaught exception in background thread " + thread.getName() + ": " + throwable.getMessage(), throwable);
        handleException(thread, throwable);
    }

    // ── 初始化 ──────────────────────────────────────────────────────────────────────

    /**
     * 初始化全局异常捕获。在 Application.onCreate() 中尽早调用。
     */
    public void init(Context context) {
        mContext = context.getApplicationContext();
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        new Handler(Looper.getMainLooper()).post(() -> {
            while (true) {
                try {
                    Looper.loop();
                } catch (Throwable e) {
                    handleException(Thread.currentThread(), e);
                }
            }
        });
    }

    // ── 异常处理 ────────────────────────────────────────────────────────────────────

    private void handleException(@NonNull Thread thread, @NonNull Throwable throwable) {
        if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(SettingsConstants.PREF_CRASH_SHOW_LOG, false)) {
            showCrashLog(thread, throwable);
        } else {
            restartSilently();
        }
    }

    private void showCrashLog(@NonNull Thread thread, @NonNull Throwable throwable) {
        UIHelper.setLockApp(mContext, -1);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Log.e(TAG, "An error was occurred while waiting screen pinning to close: ", e);
        }
        String crashInfo = buildCrashInfo(thread, throwable);

        Intent intent = new Intent(mContext, CrashLogActivity.class);
        intent.putExtra(CrashLogActivity.EXTRA_CRASH_LOG, crashInfo);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mContext.startActivity(intent);
        // 主 Looper 还活着（被 Guard 保护着），Activity 会正常启动，不需要额外操作
    }

    private void restartSilently() {
        Intent intent = new Intent(mContext, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);

        try {
            Thread.sleep(300);
        } catch (InterruptedException ignored) {
        }

        Process.killProcess(Process.myPid());
        System.exit(10);
    }

    // ── 工具 ────────────────────────────────────────────────────────────────────────

    private String buildCrashInfo(Thread thread, Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append("Thread: ").append(thread.getName()).append("\n");
        sb.append("Time: ")
                .append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                        java.util.Locale.getDefault()).format(new java.util.Date()))
                .append("\n\n");
        sb.append(getStackTraceString(throwable));

        Throwable cause = throwable.getCause();
        while (cause != null) {
            sb.append("\n\nCaused by: ");
            sb.append(getStackTraceString(cause));
            cause = cause.getCause();
        }
        return sb.toString();
    }

    private String getStackTraceString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }
}
