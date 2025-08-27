package com.wtbruh.fakelauncher;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.TextViewCompat;
import androidx.preference.PreferenceManager;

import com.rosan.dhizuku.api.Dhizuku;
import com.wtbruh.fakelauncher.receiver.DeviceAdminReceiver;
import com.wtbruh.fakelauncher.receiver.PowerConnectionReceiver;
import com.wtbruh.fakelauncher.ui.fragment.phone.DialerFragment;
import com.wtbruh.fakelauncher.ui.fragment.settings.SubSettingsFragment;
import com.wtbruh.fakelauncher.ui.widget.StrokeTextView;
import com.wtbruh.fakelauncher.utils.ApplicationHelper;
import com.wtbruh.fakelauncher.utils.ContentProvider;
import com.wtbruh.fakelauncher.utils.LunarCalender;
import com.wtbruh.fakelauncher.ui.BaseAppCompatActivity;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;
import com.wtbruh.fakelauncher.utils.ScreenObserver;
import com.wtbruh.fakelauncher.utils.TelephonyHelper;
import com.wtbruh.fakelauncher.utils.UIHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends BaseAppCompatActivity implements PowerConnectionReceiver.getStat, ScreenObserver.ScreenStateListener {

    // extra args 额外参数
    public final static String
            EXTRA_PREVIEW = "preview",
            EXTRA_EXIT = "exit";

    // UI style
    public final static String
            STYLE_PHONE = "phone",
            STYLE_PLAYER = "player";
    private String mStyle;

    private Dialog dialog;
    // date
    private final static int TIME = 0, DATE = 1, WEEK = 2;

    // battery
    private int mBatteryLevel;
    private final static int[] batteryIcons = {
            R.drawable.ic_battery_1,
            R.drawable.ic_battery_2,
            R.drawable.ic_battery_3,
            R.drawable.ic_battery_4
    };
    private boolean mCharging = false, mShowAccurateBattery = false;

    // 广播接收器 Broadcast receiver
    private final PowerConnectionReceiver mReceiver = new PowerConnectionReceiver();
    private boolean mReceiverRegistered = false;

    // is screen off 是否熄屏
    private boolean mLocked = false;

    // key long press check 按键长按检查
    private boolean mKeyLongPressed = false;

    // preview mode 预览模式
    private boolean mPreviewMode = false;

    // Regularly refresh data
    // 计时任务
    private String mPreviousDate, previousTime;
    private int mPreviousBattery;
    private Timer mTimer;

    // key count 按键计数
    private int mKeyCount = 0;

    // Device owner
    private int mDeviceAdminType = PrivilegeProvider.DEACTIVATED;
    private DevicePolicyManager mDpm;

    private final static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Switch UI style
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        String mDefaultStyle = getResources().getString(R.string.pref_style_default);
        mStyle = pref.getString(SubSettingsFragment.PREF_STYLE, mDefaultStyle);
        if (mStyle.equals(STYLE_PLAYER)) {
            setContentView(R.layout.activity_main_player);
        } else {
            setContentView(R.layout.activity_main_phone);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.Main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // If expected preview, only initialize UI
        // 如果只需要预览，就只做UI初始化
        if (getIntent().getBooleanExtra(EXTRA_PREVIEW, false)) {
            mPreviewMode = true;
            initUI();
        }
        else init();
    }

    /**
     * Init of MainActivity | MainActivity初始化
     */
    private void init() {
        Log.d(TAG, "Now start init, UI style: " + mStyle);
        // Common init 通用初始化代码
        // Manually get battery 手动获取电池电量
        setBattery();
        // Register the receiver 注册接收器
        receiverRegister(true);
        // Manually get connection status 手动获取连接状态
        getConnectionStatus();
        // Start timer 启动计时任务
        updateInfo();
        if (mStyle.equals(STYLE_PLAYER)) {
            // todo: mp3 ui init
        } else { // Default/Fallback: feature phone UI
            ScreenObserver screenObserver = new ScreenObserver(this);
            screenObserver.startScreenObserver(this);

            initDeviceOwner();
            // Start pin mode 启用屏幕固定
            setLockApp(MainActivity.this, getTaskId());
        }
        // UI init
        initUI();
        // Disable touch screen
        UIHelper.touchscreenController(false, this);
    }

    /**
     * Init of MainActivity user interface | MainActivity 用户界面初始化
     */
    private void initUI() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        float scale = (float) sp.getInt(SubSettingsFragment.PREF_MAIN_UI_HEIGHT_SCALE, 10) / 10;
        batteryAccurate();
        if (mStyle.equals(STYLE_PLAYER)) {
            // todo: mp3 ui init
        } else { // Default/Fallback: feature phone UI
            int[] simpleResizableView = {
                    R.id.date,
                    R.id.lunarDate,
                    R.id.simCard,
            };
            if (scale > 0 && scale != 1.0) {
                for (int resId : simpleResizableView) {
                    View view = findViewById(resId);
                    UIHelper.resizeView(scale, view);
                }
            }

            TelephonyHelper mTelHelper = new TelephonyHelper(this);
            StrokeTextView
                    card1 = findViewById(R.id.card1_provider),
                    card2 = findViewById(R.id.card2_provider);
            View cardProvider = findViewById(R.id.cardProvider);
            card1.setText(mTelHelper.getProvidersName(0));
            card2.setText(mTelHelper.getProvidersName(1));

            cardProvider.post(() -> {
                cardProvider.getLayoutParams().width = WRAP_CONTENT;
                cardProvider.getViewTreeObserver().addOnGlobalLayoutListener(UIHelper.getFitWidthViewsListener(cardProvider, card1, card2));
                cardProvider.requestLayout();
            });

            UIHelper.resizeView(scale,
                    findViewById(R.id.statusBar),
                    findViewById(R.id.connection),
                    findViewById(R.id.battery));
        }
        // Common init 通用初始化代码
        // 时间字体大小自适应适配
        TextView time = findViewById(R.id.time);
        time.post(() -> {
            boolean pref = sp.getBoolean(SubSettingsFragment.PREF_TIME_SHOW_SECOND, false);
            time.setText(pref? "11:45:14" : "19:19" );
            time.getLayoutParams().height = (int) (time.getHeight() * scale);
            time.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    TextViewCompat.setAutoSizeTextTypeWithDefaults(time, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
                    // 获取缩放后的字体大小
                    float textSize = time.getTextSize(); // 单位：px
                    Log.d(TAG, "now text size: " + textSize);
                    time.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    time.getLayoutParams().height = (int) (textSize + time.getPaddingTop() + time.getPaddingBottom() + 10);
                    time.requestLayout();
                }
            });
            time.requestLayout();
        });
    }

    /**
     * 当想要退出App时，重复启动MainActivity可触发退出
     */
    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        boolean isExit = intent.getBooleanExtra(EXTRA_EXIT, false);
        if (isExit) exit();
    }

    /**
     * do necessary codes before calling onDestroy()<br>
     * 调用onDestroy()之前需要执行的代码
     */
    private void exit() {
        if (mTimer != null) mTimer.cancel();
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

    @Override
    protected void onDestroy() {
        if (getLockApp(MainActivity.this) != -1) setLockApp(MainActivity.this, -1);
        // Unregister the receiver on destroy
        // 关掉app时注销掉接收器
        receiverRegister(false);
        // 停止计时任务 Stop timer
        if (mTimer != null) mTimer.cancel();
        // Enable touch screen
        UIHelper.touchscreenController(true, this);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        batteryAccurate();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mStyle.equals(STYLE_PHONE)) {
            // Star key long press detection
            // 长按星键检测
            if (mLocked && keyCode == KeyEvent.KEYCODE_STAR && event.getRepeatCount() >= 2) {
                mKeyLongPressed = true;
                onUnlocked();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mPreviewMode) {
            return super.onKeyUp(keyCode, event);
        }
        if (mStyle.equals(STYLE_PHONE)) {
            if (mLocked) { // 需要解锁的情况 Need star key unlock
                if (keyCode == KeyEvent.KEYCODE_MENU) {
                    dialog = UIHelper.showCustomDialog(this, R.string.dialog_press_star_unlock, (dialogInterface, keyCode1, keyEvent) -> {
                        if (keyCode1 == KeyEvent.KEYCODE_STAR) {
                            onUnlocked();
                        }
                        return false;
                    });
                } else {
                    if (!mKeyLongPressed)
                        dialog = UIHelper.showCustomDialog(this, R.string.dialog_long_press_star_unlock, (dialogInterface, keyCode1, keyEvent) -> {
                            if (keyCode1 != KeyEvent.KEYCODE_BACK) {
                                if (keyEvent.getRepeatCount() >= 2) {
                                    mKeyLongPressed = true;
                                    onUnlocked();
                                }
                            }
                            return false;
                        });
                }
                // 展示提示弹窗后不会执行下面的代码
                // The codes below will not be executed. Only show dialog
                return super.onKeyUp(keyCode, event);
            }
            if (mKeyLongPressed) {
                mKeyLongPressed = false;
                return true;
            }
            // Unlocked 已解锁后
            counter(keyCode);
            if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                    if (!PreferenceManager.getDefaultSharedPreferences(this)
                            .getBoolean(SubSettingsFragment.PREF_DPAD_CENTER_OPEN_MENU, false)
                    ) return super.onKeyUp(keyCode, event);
                }
                // Open menu UI
                // 打开菜单界面
                Log.d(TAG, "Pressed menu key");
                UIHelper.intentStarter(MainActivity.this, SubActivity.class);

            } else if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_POUND) {
                // Simulate the logic of the elders' phone: Pressing the number keys on the main UI will open the dialer
                // 模拟老人机逻辑：主界面按数字键打开拨号盘
                String key = switch (keyCode) {
                    case KeyEvent.KEYCODE_POUND -> "#";
                    case KeyEvent.KEYCODE_STAR -> "*";
                    default ->
                        // Key 0~9 0到9键
                            String.valueOf(keyCode - KeyEvent.KEYCODE_0);
                };
                String[] extra = {DialerFragment.class.getName(), key};
                if (!UIHelper.intentStarterDebounce(SubActivity.class)) {
                    startActivity(
                            new Intent().setClass(MainActivity.this, SubActivity.class)
                                    .putExtra("args", extra)
                    );
                    // Disable transition anim
                    // 去掉过渡动画
                    overridePendingTransition(0, 0);
                }
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onScreenOn() {
        onLocked();
    }
    @Override
    public void onScreenOff() {
        // Clear dialog on screen off
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = null;
    }
    @Override
    public void onUserPresent() {}

    /**
     * init of DeviceOwner | DeviceOwner 初始化
     */
    private void initDeviceOwner() {
        // Check privilege level
        // 检查权限等级
        mDeviceAdminType = PrivilegeProvider.checkDeviceAdmin(this);
        mDpm = switch (mDeviceAdminType) {
            // Dhizuku已激活
            case PrivilegeProvider.DHIZUKU -> PrivilegeProvider.binderWrapperDevicePolicyManager(this);
            // Device Owner已激活
            case PrivilegeProvider.DEVICE_OWNER -> ContextCompat.getSystemService(this, DevicePolicyManager.class);
            default -> null;
        };
    }

    /**
     * Xposed 模块自检测
     * @return 如果已激活，返回结果会被hook修改为true
     */
    public static boolean isXposedModuleActivated() {
        return false;
    }

    /**
     * Footer customization<br>
     * 界面底部自定义
     * @param resId 文字的资源id
     */
    private void setFooterBar(int resId) {
        TextView leftButtonTv = findViewById(R.id.main_leftButton);
        leftButtonTv.setText(resId);
    }

    private void onLocked() {
        String topActivity = ApplicationHelper.topActivity;
        if (topActivity != null) if (!topActivity.contains(MainActivity.class.getSimpleName())) return;
        mLocked = true;
        setFooterBar(R.string.unlock_leftButton);
    }
    private void onUnlocked() {
        // New dialog should be opened before closing the old one.
        Dialog dialog2;
        dialog2 = UIHelper.showCustomDialog(MainActivity.this, R.string.dialog_unlocked, null);
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = dialog2;
        setFooterBar(R.string.main_leftButton);
        mLocked = false;
    }

    /**
     * Get time info 获取时间信息
     * @param target true为获取时间，false为获取日期
     * @return 返回时间/日期信息
     */
    private String getTime(int target){
        long rawTime = System.currentTimeMillis();
        Date d = new Date(rawTime);
        SimpleDateFormat format = null;
        SharedPreferences defaultPref = PreferenceManager.getDefaultSharedPreferences(this);
        switch (target) {
            case TIME: {
                // Check if showing seconds
                boolean pref = defaultPref.getBoolean(SubSettingsFragment.PREF_TIME_SHOW_SECOND, false);
                String pattern = pref ? "HH:mm:ss" : "HH:mm";
                format = new SimpleDateFormat(pattern, Locale.getDefault());
                break;
            }
            case DATE: {
                String pattern = STYLE_PLAYER.equals(mStyle) ? "yyyy-MM-dd" : getResources().getString(R.string.date_format);
                format = new SimpleDateFormat(pattern, Locale.getDefault());
                break;
            }
            case WEEK: {
                format = new SimpleDateFormat("E", Locale.getDefault());
                break;
            }
        }
        return format == null ? "" : format.format(d) ;
    }

    /**
     * Automatically update data 定时更新数据
     */
    private void updateInfo() {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                new Handler(Looper.getMainLooper()).post(() -> {
                    TextView timeView = findViewById(R.id.time), 
                            dateView = findViewById(R.id.date),
                            lunarView = findViewById(R.id.lunarDate),
                            weekView = STYLE_PLAYER.equals(mStyle) ? findViewById(R.id.week) : null;
                    String time = getTime(TIME),
                            date = getTime(DATE);
                    int battery = getBattery();
                    if (!time.equals(previousTime)) {
                        timeView.setText(time);
                        previousTime = time;
                    }
                    if (!date.equals(mPreviousDate)) {
                        if (weekView != null) weekView.setText(getTime(WEEK));
                        if (lunarView != null) lunarView.setText(LunarCalender.getLunarString(LunarCalender.getDateArray()));
                        dateView.setText(date);
                        mPreviousDate = date;
                    }
                    if (battery != mPreviousBattery) {
                        setBattery(battery);
                    }
                });
            }
        }, 0, 1000);
    }
    @SuppressLint("SetTextI18n")
    private void setBattery(int battery) {
        if (mStyle.equals(STYLE_PLAYER)) {
            TextView battery_view = findViewById(R.id.battery);
            battery_view.setText(battery+"%");
        }
        else if (mStyle.equals(STYLE_PHONE)) {
            if (mShowAccurateBattery) {
                TextView battery_view = findViewById(R.id.battery);
                battery_view.setText(battery+"%");
            } else {
                if (battery >= 75) mBatteryLevel = 3;
                else if (battery >= 50) mBatteryLevel = 2;
                else if (battery >= 25) mBatteryLevel = 1;
                else mBatteryLevel = 0;

                setBatteryIcons(mBatteryLevel);
            }
        }
        mPreviousBattery = battery;
    }

    private void setBattery() {
        setBattery(getBattery());
    }

    /**
     * Get battery percent 获取电量百分比
     * @return 返回电量百分比
     */
    private int getBattery() {

        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, filter);

        int defaultValue = -1, level = defaultValue, scale = defaultValue;

        if (batteryStatus != null) {
            level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, defaultValue);
            scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, defaultValue);
        }

        float batteryPct = level * 100 / (float) scale;
        return (int) batteryPct;
    }

    /**
     * Get connection status manually<br>
     * 手动获取连接状态
     */
    private void getConnectionStatus() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, filter);
        int defaultValue = -1, status = defaultValue;

        if (batteryStatus != null) {
            status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, defaultValue);
        }
        mCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
        setConnectionStatus();
    }

    /**
     * Get connection status from PowerConnectionReceiver<br>
     * 从PowerConnectionReceiver里获取连接状态
     * @param status PowerConnectionReceiver返回的充电状态
     */
    @Override
    public void getConnectionStatus(String status) {
        if (status.equals(Intent.ACTION_POWER_CONNECTED)) {
            mCharging = true;
        } else if (status.equals(Intent.ACTION_POWER_DISCONNECTED)) {
            mCharging = false;
        }
        setConnectionStatus();
    }

    private void setConnectionStatus() {

        if (mStyle.equals(STYLE_PLAYER)) {
            View connection_view = findViewById(R.id.connection);
            if (mCharging) {
                connection_view.setVisibility(View.VISIBLE);
            } else {
                connection_view.setVisibility(View.GONE);
            }

        } else {
            TextView connection_view = findViewById(R.id.connection);
            if (mCharging) {
                if (mShowAccurateBattery) connection_view.setText(R.string.charging);
                else {
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        int z = mBatteryLevel;
                        @Override
                        public void run() {
                            if (!mCharging) cancel();
                            if (z <= batteryIcons.length - 1) {
                                setBatteryIcons(z);
                                z += 1;
                            } else z = mBatteryLevel;
                        }
                    },0,1000);
                }

            } else {
                if (mShowAccurateBattery) connection_view.setText(R.string.not_charging);
                else setBatteryIcons(mBatteryLevel);
            }
        }
    }

    private void setBatteryIcons(int level) {
        View main = findViewById(R.id.Main);
        main.post( () -> {
            Drawable overlay = ContextCompat.getDrawable(this, batteryIcons[level]);
            if (overlay != null) {
                int screenWidth = main.getWidth(), margin = 10, scale = 4;
                overlay.setBounds(screenWidth - margin - overlay.getIntrinsicWidth() / scale, margin, screenWidth - margin, margin + overlay.getIntrinsicHeight() / scale );
                main.getOverlay().clear();
                main.getOverlay().add(overlay);
            }
        });
    }

    /**
     * Check if user needs to show accurate battery.<br>
     * If so, lunar calendar should be hidden<br>
     * 检查用户是否需要显示精确电量，<br>
     * 如果是，则需隐藏农历显示
     */
    private void batteryAccurate() {
        View statusBarView = findViewById(R.id.statusBar),
                dateView = switch (mStyle) {
                    case STYLE_PLAYER -> findViewById(R.id.week);
                    default -> findViewById(R.id.lunarDate); // case STYLE_PHONE
                };
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mShowAccurateBattery = sharedPrefs.getBoolean(SubSettingsFragment.PREF_SHOW_ACCURATE_BATTERY, false);
        statusBarView.setVisibility(mShowAccurateBattery? View.VISIBLE : View.INVISIBLE);
        if (mStyle.equals(STYLE_PHONE)) dateView.setVisibility(mShowAccurateBattery? View.INVISIBLE : View.VISIBLE);

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
            if (! mReceiverRegistered) {
                registerReceiver(mReceiver, ifilter);
                mReceiver.setStat(this);
                mReceiverRegistered = true;
                Log.d(TAG, "Receiver registered!");
            } else Log.w(TAG, "Receiver already registered!");
        } else {
            if (mReceiverRegistered) {
                unregisterReceiver(mReceiver);
                mReceiverRegistered = false;
                Log.d(TAG, "Receiver unregistered!");
            } else Log.w(TAG, "Receiver not registered!");
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
        if (! UIHelper.checkExitMethod(this, 0)) return;
        if (mKeyCount < 0 || mKeyCount > 7) mKeyCount = 0;
        switch (keycode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                Log.d(TAG, "Pressed key UP");
                if (mKeyCount <= 1) mKeyCount++; else mKeyCount = 1;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                Log.d(TAG, "Pressed key DOWN");
                if (mKeyCount == 2 || mKeyCount == 3) mKeyCount++; else mKeyCount = 0;
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                Log.d(TAG, "Pressed key LEFT");
                if (mKeyCount == 4 || mKeyCount == 6) mKeyCount++; else mKeyCount = 0;
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                Log.d(TAG, "Pressed key RIGHT");
                if (mKeyCount == 5) mKeyCount++; else if (mKeyCount == 7 ) {
                    mKeyCount = 0;
                    exit();
                } else mKeyCount = 0;
                break;
            default:
                mKeyCount = 0;
        }
        Log.d(TAG,"count="+ mKeyCount);
    }

    /**
     * Trigger screen pinning<br>
     * 启用屏幕固定
     * @param activity 应用Activity对象
     * @param id 当前TaskId（-1为关闭屏幕固定）
     */
    public void setLockApp(Activity activity, int id) {
        // 新方案，用ContentProvider存储taskId，无需操作Settings数据库
        if (isXposedModuleActivated()) {
            Log.d(TAG, "Xposed module enabled, putting taskId into ContentProvider");
            ContentProvider.setTaskId(activity, id);
            return;
        }
        new Thread(() -> {
            ComponentName receiver = switch (mDeviceAdminType) {
                case PrivilegeProvider.DHIZUKU -> Dhizuku.getOwnerComponent();
                default -> new ComponentName(this, DeviceAdminReceiver.class);
            };
            // Check device admin permission
            if (mDpm != null) mDpm.setLockTaskPackages(receiver, new String[]{getPackageName()});
            if (id != -1) {
                activity.startLockTask();
            } else {
                activity.stopLockTask();
            }
        }).start();
    }

    /**
     * Read TaskId from settings database to confirm if pin mode is triggered<br>
     * 读取设置数据库中的TaskId以确定屏幕固定状态
     */
    public static int getLockApp(Context context) {
        return ContentProvider.getTaskId(context);
    }

}