package com.wtbruh.fakelauncher;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements PowerConnectionReceiver.getstat {
    int taskId;
    int count = 0;
    String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.Main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Start data updater 自动更新数据
        updateInfo();
        // Register the receiver 注册接收器
        receiverRegister(true);
        // Manually flash connection status at first 先手动刷新下充电状态
        getBattery(false);
        getTaskID();

    }

    // Unregister the receiver on destroy
    // 关掉app时注销掉接收器
    @Override
    protected void onDestroy() {
        super.onDestroy();
        receiverRegister(false);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        counter(keyCode);
        Intent intent = new Intent();
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            intent.setClass(MainActivity.this, MenuActivity.class);
            startActivity(intent);
            // Disable transition anim
            // 去掉过渡动画
            MainActivity.this.overridePendingTransition(0, 0);
        } else if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_POUND) {
            String extra;
            intent.setClass(MainActivity.this, DialerActivity.class);
            switch (keyCode) {
                case KeyEvent.KEYCODE_POUND:
                    extra = "#";
                    break;
                case KeyEvent.KEYCODE_STAR:
                    extra = "*";
                    break;
                default:
                    extra = String.format("%d", keyCode - KeyEvent.KEYCODE_0);
                    break;
            }
            intent.putExtra("key", extra);
            startActivity(intent);
            // Disable transition anim
            // 去掉过渡动画
            MainActivity.this.overridePendingTransition(0, 0);
        }
        return true;
    }

    // Get time info 获取时间信息
    private String getTime(boolean target){
        long rawtime = System.currentTimeMillis();
        Date d = new Date(rawtime);
        if (target) {
            SimpleDateFormat time_format = new SimpleDateFormat("HH:mm:ss", Locale.CHINA);
            String time = time_format.format(d);
            return time;
        } else {
            SimpleDateFormat date_format = new SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA);
            String date = date_format.format(d);
            return date;
        }
    }

    // Automatically update data 定时更新数据
    private void updateInfo() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        TextView time_view = findViewById(R.id.time);
                        TextView date_view = findViewById(R.id.date);
                        TextView battery_view = findViewById(R.id.battery);
                        String time = getTime(true);
                        String date = getTime(false);
                        String battery = getBattery(true);
                        time_view.setText(time);
                        date_view.setText(date);
                        battery_view.setText(battery+"%");
                    }
                });
            }
        }, 0, 1000);
    }

    // Get battery percent 获取电量百分比
    private String getBattery(boolean target) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        if (target) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            float batteryPct = level * 100 / (float) scale;
            return String.format("%.0f", batteryPct);
        } else {
            // 另外加了个获取充电状态的，只会在刚打开时有用
            TextView connection_view = findViewById(R.id.connection);
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;
            if (isCharging) {
                connection_view.setText(R.string.charging);
            } else {
                connection_view.setText(R.string.notcharging);
            }
            return "";
        }
    }

    // Get data from PowerConnectionReceiver
    // 从PowerConnectionReceiver里获取连接状态
    @Override
    public void getConnectionStatus(String status) {
        TextView connection_view = findViewById(R.id.connection);
        if (status.equals(Intent.ACTION_POWER_CONNECTED)) {
            connection_view.setText(R.string.charging);
        } else if (status.equals(Intent.ACTION_POWER_DISCONNECTED)) {
            connection_view.setText(R.string.notcharging);
        }
    }

    // Implement of dynamically register the PowerConnectionReceiver
    // 为PowerConnectionReceiver实现动态注册
    private void receiverRegister(boolean operation) {
        IntentFilter ifilter = new IntentFilter();
        ifilter.addAction(Intent.ACTION_POWER_CONNECTED);
        ifilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        PowerConnectionReceiver receiver = new PowerConnectionReceiver();
        if (operation) {
            registerReceiver(receiver, ifilter);
            receiver.setstat(this);
        } else {
            unregisterReceiver(receiver);
        }
    }
    // Key counter 按键计数器
    // Expected key operation: up, up, down, down, left, right, left, right
    // 预计的按键操作：上上下下左右左右
    private void counter(int keycode) {
        if (count < 0 || count > 7) count = 0;
        switch (keycode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                Log.d(TAG, "Pressed key UP");
                if (count <= 1) count++; else count = 1;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                Log.d(TAG, "Pressed key DOWN");
                if (count == 2 || count == 3) count++; else count = 0;
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                Log.d(TAG, "Pressed key LEFT");
                if (count == 4 || count == 6) count++; else count = 0;
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                Log.d(TAG, "Pressed key RIGHT");
                if (count == 5) count ++; else if (count == 7 ) {
                    count = 0;
                    // do exit app
                } else count = 0;
                break;
            default:
                count = 0;
        }
        Log.d(TAG,"count="+String.format("%d", count));
    }

    private int getTaskID() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {

                }

                @Override
                public void onActivityStarted(@NonNull Activity activity) {

                }

                @Override
                public void onActivityResumed(@NonNull Activity activity) {
                    taskId = activity.getTaskId();
                }

                @Override
                public void onActivityPaused(@NonNull Activity activity) {

                }

                @Override
                public void onActivityStopped(@NonNull Activity activity) {

                }

                @Override
                public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

                }

                @Override
                public void onActivityDestroyed(@NonNull Activity activity) {

                }
            });
        }
    return taskId;
    }
}