package com.wtbruh.fakelauncher;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.wtbruh.fakelauncher.ui.BaseAppCompatActivity;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;
import com.wtbruh.fakelauncher.utils.UIHelper;

/**
 * 伪关机弹窗（功能机风格）：
 * 伪装界面长按挂机键（电源键）弹出，提供"关机"与"重启"两个选项。
 * 通过 Shizuku / Root 执行 reboot 命令实现真正关机或重启。
 */
public class ShutdownActivity extends BaseAppCompatActivity {

    private static final int OPT_COUNT = 2;
    private int selected = 0;
    private TextView powerView;
    private TextView rebootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏系统状态栏，保持功能机全屏风格
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .hide(WindowInsetsCompat.Type.statusBars());
        setContentView(R.layout.activity_shutdown);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.shutdown_title).getRootView(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        powerView = findViewById(R.id.shutdown_power);
        rebootView = findViewById(R.id.shutdown_reboot);
        // 功能机风格：禁触
        powerView.setOnTouchListener((v, e) -> true);
        rebootView.setOnTouchListener((v, e) -> true);
        refresh();
    }
    private void refresh() {
        powerView.setSelected(selected == 0);
        rebootView.setSelected(selected == 1);
        applyStyle(powerView, selected == 0);
        applyStyle(rebootView, selected == 1);
    }

    private void applyStyle(TextView tv, boolean sel) {
        tv.setTextColor(sel ? Color.BLACK : Color.WHITE);
        tv.setBackgroundColor(sel ? Color.WHITE : Color.TRANSPARENT);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                selected = 0;
                refresh();
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                selected = OPT_COUNT - 1;
                refresh();
                return true;
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                doAction(selected);
                return true;
            case KeyEvent.KEYCODE_BACK:
                // 返回：取消关机
                finish();
                // 通知服务清理底下残留的系统关机菜单
                sendBroadcast(new Intent("com.wtbruh.fakelauncher.CLOSE_SYSTEM_MENU"));
                return true;
            case KeyEvent.KEYCODE_MENU:
                // 左菜单键：取消关机
                finish();
                sendBroadcast(new Intent("com.wtbruh.fakelauncher.CLOSE_SYSTEM_MENU"));
                return true;
            default:
                // 吞掉所有未知键，防止误触发任何系统动作（如误关机）
                return true;
        }
    }

    /** 执行关机（0）或重启（1） */
    private void doAction(int pos) {
        final String cmd = pos == 0 ? "reboot -p" : "reboot";
        new Thread(() -> {
            try {
                int method = PrivilegeProvider.getCurrentPrivilegeProvider(this);
                if (method == PrivilegeProvider.PRIVILEGE_SHIZUKU) {
                    PrivilegeProvider.runCommand(this, method, cmd);
                } else {
                    PrivilegeProvider.runCommand(method, cmd);
                }
            } catch (Exception e) {
                android.util.Log.e("ShutdownActivity", "reboot cmd err", e);
            }
        }).start();
        // 命令已发出，关闭弹窗（系统即将关机/重启）
        finish();
    }
}
