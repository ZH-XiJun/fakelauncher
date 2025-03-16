package com.wtbruh.fakelauncher;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Start data updater
        updateInfo();
        // Register the receiver
        receiverRegister(true);
    }

    // Unregister the receiver on destroy
    @Override
    protected void onDestroy() {
        super.onDestroy();
        receiverRegister(false);
    }

    // Get time info
    String getTime(boolean target){
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

    // Automatically update data
    void updateInfo() {
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
                        String battery = getBattery();
                        time_view.setText(time);
                        date_view.setText(date);
                        battery_view.setText(battery+"%");
                    }
                });
            }
        }, 0, 1000);
    }

    // Get battery percent
    String getBattery() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level * 100 / (float) scale;
        return String.format("%.0f", batteryPct);

    }

    // Get data from PowerConnectionReceiver
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
    void receiverRegister(boolean operation) {
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
}