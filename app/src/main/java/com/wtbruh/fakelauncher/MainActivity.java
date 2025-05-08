package com.wtbruh.fakelauncher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.TextViewCompat;
import androidx.preference.PreferenceManager;

import com.wtbruh.fakelauncher.receiver.PowerConnectionReceiver;
import com.wtbruh.fakelauncher.utils.ContentProvider;
import com.wtbruh.fakelauncher.utils.MyAppCompatActivity;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;
import com.wtbruh.fakelauncher.utils.TelephonyHelper;
import com.wtbruh.fakelauncher.utils.UIHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends MyAppCompatActivity implements PowerConnectionReceiver.getStat {
    private Timer mTimer;
    private int count = 0;
    private final PowerConnectionReceiver mReceiver = new PowerConnectionReceiver();
    private TelephonyHelper mTelHelper;
    private final static String TAG = MainActivity.class.getSimpleName();

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
        init();
    }

    @Override
    protected void onDestroy() {
        if (getLockApp(MainActivity.this) != -1) setLockApp(MainActivity.this, -1);
        // Unregister the receiver on destroy
        // 关掉app时注销掉接收器
        receiverRegister(false);
        // 停止计时任务 Stop timer
        if (mTimer != null) mTimer.cancel();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 检查是否需要退出
        // Log.d(TAG, "checkexit" + getIntent().getBooleanExtra("exit", false));
        if (getIntent().getBooleanExtra("exit", false)) exit();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            // Open menu UI
            // 打开菜单界面
            Log.d(TAG, "Pressed menu key");

        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        counter(keyCode);
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            // Open menu UI
            // 打开菜单界面
            Log.d(TAG, "Pressed menu key");
            UIHelper.intentStarter(MainActivity.this, MenuActivity.class);

        } else if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_POUND) {
            // Simulate the logic of the elders' phone: Pressing the number keys on the main UI will open the dialer
            // 模拟老人机逻辑：主界面按数字键打开拨号盘
            String extra;
            switch (keyCode) {
                case KeyEvent.KEYCODE_POUND:
                    extra = "#";
                    break;
                case KeyEvent.KEYCODE_STAR:
                    extra = "*";
                    break;
                default:
                    // Key 0~9 0到9键
                    extra = String.valueOf(keyCode - KeyEvent.KEYCODE_0);
                    break;
            }
            UIHelper.intentStarter(MainActivity.this, DialerActivity.class, "key", extra);
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Init of MainActivity | MainActivity初始化
     */
    private void init() {
        mTelHelper = new TelephonyHelper(this);
        TextView card1 = findViewById(R.id.card1_provider);
        TextView card2 = findViewById(R.id.card2_provider);
        card1.setText(mTelHelper.getProvidersName(0));
        card2.setText(mTelHelper.getProvidersName(1));
        // 时间字体大小自适应适配
        TextView time = findViewById(R.id.time);
        time.post(() -> {
            // 获取缩放后的字体大小
            float textSize = time.getTextSize(); // 单位：px
            time.setTextSize(textSize);
            TextViewCompat.setAutoSizeTextTypeWithDefaults(time, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
            Log.d(TAG, "now text size: "+textSize);
            time.getLayoutParams().height = (int) (textSize + time.getPaddingTop() + time.getPaddingBottom() + 10);
            time.requestLayout();
        });

        // Start timer 启动计时任务
        updateInfo();
        // Register the receiver 注册接收器
        receiverRegister(true);
        // Manually flash connection status at first 先手动刷新下充电状态
        getBattery(false);
        // Start pin mode 启用屏幕固定
        setLockApp(MainActivity.this, getTaskId());
    }

    /**
     * do necessary codes before calling onDestroy()<br>
     * 调用onDestroy()之前需要执行的代码
     */
    private void exit() {
        // Disable pin mode
        // 关闭屏幕固定
        setLockApp(MainActivity.this, -1);
        // Wait for pin mode disabled, or finishAndRemoveTask() won't work
        // 等待屏幕固定被关闭，不然finishAndRemoveTask()没用
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Log.e(TAG, "An error was occurred while waiting screen pinning to close: "+e);
        }
        // kill myself
        finishAndRemoveTask();
    }

    /**
     * Xposed 模块自检测
     * @return 如果已激活，返回结果会被hook修改为true
     */
    public static boolean isXposedModuleActivated() {
        return false;
    }

    /**
     * Get time info 获取时间信息
     * @param target true为获取时间，false为获取日期
     * @return 返回时间/日期信息
     */
    private String getTime(boolean target){
        long rawTime = System.currentTimeMillis();
        Date d = new Date(rawTime);
        if (target) {
            SimpleDateFormat time_format = new SimpleDateFormat("HH:mm:ss", Locale.CHINA);
            return time_format.format(d);
        } else {
            SimpleDateFormat date_format = new SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA);
            return date_format.format(d);
        }
    }

    /**
     * Automatically update data 定时更新数据
     */
    private void updateInfo() {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                new Handler(Looper.getMainLooper()).post(() -> {
                    TextView time_view = findViewById(R.id.time);
                    TextView date_view = findViewById(R.id.date);
                    TextView battery_view = findViewById(R.id.battery);
                    String time = getTime(true);
                    String date = getTime(false);
                    String battery = getBattery(true);
                    time_view.setText(time);
                    date_view.setText(date);
                    battery_view.setText(battery+"%");
                });
            }
        }, 0, 1000);
    }

    /**
     * Get battery percent 获取电量百分比
     * @param target 操作类型，true为获取电量百分比，false为获取充电状态
     * @return 返回电量百分比，如获取充电状态则为空字符串
     */
    private String getBattery(boolean target) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        if (target) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            float batteryPct = level * 100 / (float) scale;
            return String.valueOf((int) batteryPct);
        } else {
            // 另外加了个获取充电状态的，只会在刚打开时有用
            TextView connection_view = findViewById(R.id.connection);
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;
            if (isCharging) {
                connection_view.setText(R.string.charging);
            } else {
                connection_view.setText(R.string.not_charging);
            }
            return "";
        }
    }

    /**
     * Get data from PowerConnectionReceiver<br>
     * 从PowerConnectionReceiver里获取连接状态
     * @param status PowerConnectionReceiver返回的充电状态
     */
    @Override
    public void getConnectionStatus(String status) {
        TextView connection_view = findViewById(R.id.connection);
        if (status.equals(Intent.ACTION_POWER_CONNECTED)) {
            connection_view.setText(R.string.charging);
        } else if (status.equals(Intent.ACTION_POWER_DISCONNECTED)) {
            connection_view.setText(R.string.not_charging);
        }
    }

    /**
     * <h3>Receiver Register | 接收器动态注册</h3>
     * Implement of dynamically register the PowerConnectionReceiver<br>
     * 为PowerConnectionReceiver实现动态注册
     * @param operation 操作类型，true为注册，false为注销
     */
    private void receiverRegister(boolean operation) {
        IntentFilter ifilter = new IntentFilter();
        ifilter.addAction(Intent.ACTION_POWER_CONNECTED);
        ifilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        if (operation) {
            registerReceiver(mReceiver, ifilter);
            mReceiver.setStat(this);
            Log.d(TAG, "Receiver registered!");
        } else {
            unregisterReceiver(mReceiver);
            Log.d(TAG, "Receiver unregistered!");
        }
    }

    /**
     * <h3>Key counter | 按键计数器</h3>
     * Expected key operation: up, up, down, down, left, right, left, right<br>
     * 预计的按键操作：上上下下左右左右
     *
     * @param keycode 键值
     */
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
                    exit();
                } else count = 0;
                break;
            default:
                count = 0;
        }
        Log.d(TAG,"count="+count);
    }

    /**
     * Set my TaskId to settings database to trigger pin mode<br>
     * 将TaskId存到Settings数据库以启用屏幕固定
     * <h5>Thanks: HChenX/PinningApp</h5>
     * @param activity 应用Activity对象
     * @param id 当前TaskId（-1为关闭屏幕固定）
     */
    public static void setLockApp(Activity activity, int id) {
        // 新方案，用ContentProvider存储taskId，无需操作Settings数据库
        if (isXposedModuleActivated()) {
            Log.d(TAG, "Xposed module enabled, putting taskId into ContentProvider");
            ContentProvider.setTaskId(activity, id);
            return;
        }
        // Check device admin permission
        boolean dhizuku = PreferenceManager.getDefaultSharedPreferences(activity)
                .getBoolean(SettingsFragment.PREF_ENABLE_DHIZUKU, false);
        if (! PrivilegeProvider.checkDeviceAdmin(dhizuku, activity)
                .equals(activity.getResources().getString(R.string.pref_check_privilege_denied))
        ) {
            // todo - Device Admin Support
        }
    }

    /**
     * Read TaskId from settings database to confirm if pin mode is triggered<br>
     * 读取设置数据库中的TaskId以确定屏幕固定状态
     * <h5>Thanks: HChenX/PinningApp</h5>
     */
    public static int getLockApp(Context context) {
        return ContentProvider.getTaskId(context);
    }
}