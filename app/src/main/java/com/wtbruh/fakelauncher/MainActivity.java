package com.wtbruh.fakelauncher;

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

public class MainActivity extends AppCompatActivity {

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
        updateTime();
    }
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
    void updateTime() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        TextView time_view = findViewById(R.id.time);
                        TextView date_view = findViewById(R.id.date);
                        String time = getTime(true);
                        String date = getTime(false);
                        time_view.setText(time);
                        date_view.setText(date);
                    }
                });
            }
        }, 0, 1000);
    }
}