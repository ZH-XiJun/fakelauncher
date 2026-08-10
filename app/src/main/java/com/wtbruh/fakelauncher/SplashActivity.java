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

import java.io.DataOutputStream;
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
     *   1. screencap → /data/local/tmp (filesystem path, writable by root/shizuku)
     *   2. Copy via DocumentFile into the SAF directory
     *   3. Delete temp file
     */
    private void takeScreenshot() {
        MMKV kv = MMKV.defaultMMKV();
        String uriStr = kv.decodeString(SettingsConstants.PREF_GALLERY_ACCESS, "");
        if (uriStr.isEmpty()) {
            Log.w(TAG, "No gallery SAF URI configured, skipping screenshot");
            return;
        }

        int privilege = PrivilegeProvider.getCurrentPrivilegeProvider(this);
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String tmpPath = "/data/local/tmp/screenshot_" + ts + ".png";
        String filename = "screenshot_" + ts + ".png";
        String cmd = "screencap -p " + tmpPath;

        switch (privilege) {
            case PrivilegeProvider.PRIVILEGE_ROOT:
                new Thread(() -> {
                    try {
                        execRoot(cmd);
                        copyToSafDir(uriStr, tmpPath, filename);
                        execRoot("rm " + tmpPath);
                        Log.d(TAG, "Screenshot saved (root) to SAF dir");
                    } catch (Exception e) {
                        Log.e(TAG, "Screenshot (root) failed", e);
                    }
                }, "screenshot-root").start();
                break;

            case PrivilegeProvider.PRIVILEGE_SHIZUKU:
                // Shizuku is async via UserService, fire-and-forget.
                // screencap runs on the service process; we delay the copy
                // to give it time to finish, then copy via DocumentFile.
                PrivilegeProvider.runCommand(this,
                        PrivilegeProvider.PRIVILEGE_SHIZUKU, cmd);
                new Thread(() -> {
                    try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                    try {
                        copyToSafDir(uriStr, tmpPath, filename);
                        Log.d(TAG, "Screenshot saved (shizuku) to SAF dir");
                    } catch (Exception e) {
                        Log.e(TAG, "Screenshot (shizuku) copy failed", e);
                    }
                }, "screenshot-shizuku").start();
                break;

            default:
                Log.d(TAG, "No privilege for silent screenshot, skipping");
                break;
        }
    }

    /** Run a command as root synchronously. */
    private static void execRoot(String cmd) throws Exception {
        Process p = Runtime.getRuntime().exec("su");
        DataOutputStream os = new DataOutputStream(p.getOutputStream());
        os.writeBytes(cmd + "\n");
        os.writeBytes("exit\n");
        os.flush();
        os.close();
        p.waitFor();
    }

    /**
     * Copy a local file into the SAF-authorized directory using DocumentFile.
     *
     * @param safUriStr SAF tree URI string (from PREF_GALLERY_ACCESS)
     * @param srcPath   absolute filesystem path of the source file
     * @param destName  destination filename
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
