package com.wtbruh.fakelauncher;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class CrashLogActivity extends AppCompatActivity {

    private static final String TAG = "CrashLogActivity";
    public static final String EXTRA_CRASH_LOG = "crash_log";
    private String mCrashLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_crash_log);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.crashLog), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 读取崩溃日志
        mCrashLog = getIntent().getStringExtra(EXTRA_CRASH_LOG);
        if (mCrashLog == null || mCrashLog.isEmpty()) {
            mCrashLog = getString(R.string.crash_no_log);
        }

        // 显示日志
        TextView logText = findViewById(R.id.crash_log_text);
        logText.setText(mCrashLog);

        // 重启按钮
        Button btnRestart = findViewById(R.id.btn_restart);
        btnRestart.setOnClickListener(v -> restartApp());

        // 复制按钮
        Button btnCopy = findViewById(R.id.btn_copy);
        btnCopy.setOnClickListener(v -> copyLogToClipboard());

        // 退出按钮
        Button btnExit = findViewById(R.id.btn_exit);
        btnExit.setOnClickListener(v -> exitApp());
    }

    /**
     * 重启应用 —— 拉起启动 Activity 后杀进程，让系统冷启动。
     */
    private void restartApp() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        // 必须杀进程，尤其是主线崩溃时 Looper.loop() 不会自己结束
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    /**
     * 将崩溃日志复制到系统剪贴板。
     */
    private void copyLogToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("crash_log", mCrashLog);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, R.string.crash_copied, Toast.LENGTH_SHORT).show();
    }

    /**
     * 直接退出应用。
     */
    private void exitApp() {
        finishAffinity();
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }
}
