package com.wtbruh.fakelauncher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.wtbruh.fakelauncher.constants.SettingsConstants;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;
import com.wtbruh.fakelauncher.utils.UIHelper;

import java.io.DataOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        // Disable touch screen
        // 禁用触摸
        UIHelper.setTouchscreenState(false, this);

        // Take screenshot before launching fake UI (if enabled)
        // 启动伪装界面前截图（如果开启了）
        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(SettingsConstants.PREF_SCREENSHOT_BEFORE_FAKEUI, false)) {
            takeScreenshot();
        }

        // Launch fake ui with flags
        // 带flag启动伪装界面
        startActivity(new Intent()
                .setClass(SplashActivity.this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
        // Vibrate after fake ui being launched
        // 让手机振一下
        if (PreferenceManager.getDefaultSharedPreferences(SplashActivity.this)
                .getBoolean(SettingsConstants.PREF_VIBRATE_ON_START, true)) {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(200);
        }
        finish();
    }

    /**
     * Take a silent screenshot via root / Shizuku and save to gallery-accessible path.
     * Fire-and-forget: does not block the activity launch.
     */
    private void takeScreenshot() {
        int privilege = PrivilegeProvider.getCurrentPrivilegeProvider(this);
        String dir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES) + "/fakelauncher/";
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String path = dir + "screenshot_" + ts + ".png";

        String mkdir = "mkdir -p " + dir;
        String cmd = "screencap -p " + path;

        switch (privilege) {
            case PrivilegeProvider.PRIVILEGE_ROOT:
                // Root: exec su synchronously on a background thread
                new Thread(() -> {
                    try {
                        Process p = Runtime.getRuntime().exec("su");
                        DataOutputStream os = new DataOutputStream(p.getOutputStream());
                        os.writeBytes(mkdir + "\n");
                        os.writeBytes(cmd + "\n");
                        os.writeBytes("exit\n");
                        os.flush();
                        os.close();
                        p.waitFor();
                        Log.d(TAG, "Screenshot saved (root): " + path);
                        scanFile(path);
                    } catch (Exception e) {
                        Log.e(TAG, "Screenshot (root) failed", e);
                    }
                }, "screenshot-root").start();
                break;

            case PrivilegeProvider.PRIVILEGE_SHIZUKU:
                // Shizuku: fire-and-forget via runCommand
                // (The UserService callback will handle it asynchronously;
                //  we don't need the result here.)
                PrivilegeProvider.runCommand(this,
                        PrivilegeProvider.PRIVILEGE_SHIZUKU, mkdir, cmd);
                // Schedule a delayed media scan (Shizuku is async, give it time)
                new Thread(() -> {
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                    scanFile(path);
                }, "screenshot-shizuku-scan").start();
                break;

            default:
                Log.d(TAG, "No privilege for silent screenshot, skipping");
                break;
        }
    }

    /** Trigger MediaStore rescan so the screenshot appears in gallery apps. */
    private void scanFile(String path) {
        MediaScannerConnection.scanFile(this,
                new String[]{path}, null,
                (scannedPath, uri) -> {
                    if (uri != null) {
                        Log.d(TAG, "MediaScan complete: " + scannedPath);
                    } else {
                        Log.w(TAG, "MediaScan failed for: " + scannedPath);
                    }
                });
    }
}
