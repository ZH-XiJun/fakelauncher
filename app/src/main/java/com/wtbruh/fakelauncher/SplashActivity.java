package com.wtbruh.fakelauncher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import com.tencent.mmkv.MMKV;
import com.wtbruh.fakelauncher.constants.SettingsConstants;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;
import com.wtbruh.fakelauncher.utils.UIHelper;

import java.io.FileInputStream;
import java.io.OutputStream;
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
     * Take a silent screenshot via root / Shizuku and save to the SAF-authorized
     * gallery directory (PREF_GALLERY_ACCESS). Two-step process:
     *   1. screencap → cache dir (app-private, writable by root/shizuku)
     *   2. Copy via DocumentFile into the SAF directory
     */
    private void takeScreenshot() {
        MMKV kv = MMKV.defaultMMKV();
        String uriStr = kv.decodeString(SettingsConstants.PREF_GALLERY_ACCESS, "");
        if (uriStr == null || uriStr.isEmpty()) {
            Log.w(TAG, "No gallery SAF URI configured, skipping screenshot");
            return;
        }

        int privilege = PrivilegeProvider.getCurrentPrivilegeProvider(this);
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String tmpPath = getCacheDir().getAbsolutePath() + "/screenshot_" + ts + ".png";
        String filename = "screenshot_" + ts + ".png";
        String cmd = "screencap -p " + tmpPath;

        if (privilege == PrivilegeProvider.PRIVILEGE_ROOT) {
            // Root: runCommand is synchronous — screencap completes before we copy
            new Thread(() -> {
                PrivilegeProvider.runCommand(this,
                        PrivilegeProvider.PRIVILEGE_ROOT, cmd);
                copyToSafDir(uriStr, tmpPath, filename);
                Log.d(TAG, "Screenshot saved (root) to SAF dir");
            }, "screenshot-root").start();

        } else if (privilege == PrivilegeProvider.PRIVILEGE_SHIZUKU) {
            // Shizuku: runCommand is async via UserService; delay copy to wait for screencap
            PrivilegeProvider.runCommand(this,
                    PrivilegeProvider.PRIVILEGE_SHIZUKU, cmd);
            new Thread(() -> {
                try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                copyToSafDir(uriStr, tmpPath, filename);
                Log.d(TAG, "Screenshot saved (shizuku) to SAF dir");
            }, "screenshot-shizuku").start();

        } else {
            Log.d(TAG, "No privilege for silent screenshot, skipping");
        }
    }

    /**
     * Copy a local file into the SAF-authorized directory using DocumentFile.
     */
    private void copyToSafDir(String safUriStr, String srcPath, String destName) {
        try {
            Uri treeUri = Uri.parse(safUriStr);
            DocumentFile folder = DocumentFile.fromTreeUri(this, treeUri);
            if (folder == null) {
                Log.e(TAG, "DocumentFile.fromTreeUri returned null for: " + safUriStr);
                return;
            }

            DocumentFile dest = folder.createFile("image/png", destName);
            if (dest == null) {
                Log.e(TAG, "Failed to create file in SAF dir: " + destName);
                return;
            }

            try (FileInputStream in = new FileInputStream(srcPath);
                 OutputStream out = getContentResolver().openOutputStream(dest.getUri())) {
                if (out == null) {
                    Log.e(TAG, "Failed to open output stream for SAF file");
                    return;
                }
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "copyToSafDir failed", e);
        }
    }
}
