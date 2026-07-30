package com.wtbruh.fakelauncher;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.TelephonyManager;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import com.rosan.dhizuku.api.Dhizuku;
import com.wtbruh.fakelauncher.receiver.DeviceAdminReceiver;
import com.wtbruh.fakelauncher.receiver.PowerConnectionReceiver;
import com.wtbruh.fakelauncher.ui.fragment.MenuFragment;
import com.wtbruh.fakelauncher.ui.fragment.player.MusicPlayerFragment;
import com.wtbruh.fakelauncher.ui.fragment.phone.DialerFragment;
import com.wtbruh.fakelauncher.constants.SettingsConstants;
import com.wtbruh.fakelauncher.ui.widget.FitTextView;
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

public class MainActivity extends BaseAppCompatActivity implements PowerConnectionReceiver.getStat, ScreenObserver.ScreenStateListener, View.OnTouchListener {

    // extra args 额外参数
    public final static String
            EXTRA_PREVIEW = "preview",
            EXTRA_EXIT = "exit";
    private String mStyle;

    // UI scale
    private float mScale = 1;
    // Custom dialog
    private Dialog dialog;
    // date
    private final static int TIME = 0, DATE = 1, WEEK = 2;

    // battery
    private int mBatteryLevel = 4;
    private final static int[] batteryIcons = {
            R.drawable.ic_battery_1,
            R.drawable.ic_battery_2,
            R.drawable.ic_battery_3,
            R.drawable.ic_battery_4
    };
    private boolean mCharging = false, mShowAccurateBattery = false;
    private Timer mBatteryChargingAnimTimer;

    // 广播接收器 Broadcast receiver
    private final PowerConnectionReceiver mReceiver = new PowerConnectionReceiver();
    private boolean mReceiverRegistered = false;
    private boolean mPhoneWasActive = false;
    /*
     * 通话应用会在电话状态变为 IDLE 后继续处理 END_CALL，并可能再次把系统桌面
     * 置前。因此不能在收到 IDLE 的瞬间立即启动 FakeLauncher，需等待电话应用收尾。
     */
    private final Handler mPhoneReturnHandler = new Handler(Looper.getMainLooper());
    private final Runnable mReturnToFakeLauncher = () -> {
        // Always re-show MainActivity after hangup; dialing may already be cleared
        // by FakeInCallActivity.finishCallUi().
        Intent launcherIntent = new Intent(MainActivity.this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(launcherIntent);
        // Restore pin to MainActivity task so UI accepts input again.
        if (!ApplicationHelper.dialing) {
            UIHelper.setLockApp(MainActivity.this, getTaskId());
        }
    };
    private final BroadcastReceiver mPhoneStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) return;
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)
                    || TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                mPhoneWasActive = true;
                mPhoneReturnHandler.removeCallbacks(mReturnToFakeLauncher);
            } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)
                    && (ApplicationHelper.dialing || mPhoneWasActive)) {
                mPhoneWasActive = false;
                mPhoneReturnHandler.removeCallbacks(mReturnToFakeLauncher);
                // Let FakeInCallActivity close itself and clear dialing; then bring
                // MainActivity back after system phone UI finishes cleanup.
                FakeInCallActivity.requestEnd(MainActivity.this);
                mPhoneReturnHandler.postDelayed(mReturnToFakeLauncher, 900);
            }
        }
    };
    private boolean mPhoneStateRegistered = false;

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

    // key detection 按键检测相关
    private int mKeyCount = 0;
    private int[] mKeyAction = {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT
    };

    // Device owner
    private int mDeviceAdminType = PrivilegeProvider.DEACTIVATED;
    private DevicePolicyManager mDpm;

    private final static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Switch UI style
        mStyle = UIHelper.getCurrentUIType(this);
        if (mStyle.equals(UIHelper.STYLE_PLAYER)) {
            setContentView(R.layout.activity_main_player);
        } else {
            setContentView(R.layout.activity_main_phone);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.Main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // If expected preview, then only initialize UI
        // 如果只需要预览，就只做UI初始化
        if (getIntent().getBooleanExtra(EXTRA_PREVIEW, false)) {
            mPreviewMode = true;
            initUI();
        } else init();
    }

    /**
     * Init of MainActivity | MainActivity初始化
     */
    private void init() {
        Log.d(TAG, "Now start init, UI style: " + mStyle);
        // Common init 通用初始化代码
        // UI init
        initUI();
        // Manually get battery 手动获取电池电量
        setBattery();
        // Register the receiver 注册接收器
        receiverRegister(true);
        // Manually get connection status 手动获取连接状态
        getConnectionStatus();
        if (mStyle.equals(UIHelper.STYLE_PLAYER)) {
            findViewById(R.id.Main).setOnTouchListener(this);
        } else { // Default/Fallback: feature phone UI
            ScreenObserver screenObserver = new ScreenObserver(this);
            screenObserver.startScreenObserver(this);
            // Device owner init
            initDeviceOwner();
            // Start pin mode 启用屏幕固定
            UIHelper.setLockApp(MainActivity.this, getTaskId());

            // key action init
            if (UIHelper.checkExitMethod(this, UIHelper.EXIT_METHOD_DPAD)) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
                String keyActionStr = sp.getString(SettingsConstants.PREF_EXIT_FAKEUI_CONFIG_KEY, "");
                if (!keyActionStr.isEmpty()) {
                    String[] array = keyActionStr.split(",");
                    mKeyAction = new int[array.length];
                    int index = 0;
                    for (String keyCode : array) {
                        mKeyAction[index] = Integer.parseInt(keyCode);
                        index++;
                    }
                }
            }

        }
        // Start timer 启动计时任务
        updateInfo();
    }

    /**
     * Init of MainActivity user interface | MainActivity 用户界面初始化
     */
    private void initUI() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        mScale = (float) sp.getInt(SettingsConstants.PREF_MAIN_UI_HEIGHT_SCALE, 10) / 10;
        batteryAccurate();
        if (mStyle.equals(UIHelper.STYLE_PLAYER)) {
            // todo: mp3 ui init
        } else { // Default/Fallback: feature phone UI
            int[] simpleResizableView = {
                    R.id.date,
                    R.id.lunarDate,
                    R.id.simCard,
                    R.id.main_ActionBar
            };
            if (mScale > 0 && mScale != 1.0) {
                for (int resId : simpleResizableView) {
                    View view = findViewById(resId);
                    UIHelper.resizeView(mScale, view);
                }
            }

            TelephonyHelper mTelHelper = new TelephonyHelper(this);
            FitTextView
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

            UIHelper.resizeView(mScale,
                    findViewById(R.id.statusBar),
                    findViewById(R.id.connection),
                    findViewById(R.id.battery));
        }
        // Common init 通用初始化代码
        // 时间字体大小自适应适配
        FitTextView time = findViewById(R.id.time);
        time.post(() -> {
            boolean pref = sp.getBoolean(SettingsConstants.PREF_TIME_SHOW_SECOND, false);
            if (mPreviewMode) time.setText(pref? "11:45:14" : "19:19" );
            time.getLayoutParams().height = (int) (time.getHeight() * mScale);
            time.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    time.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    time.fitHeight();
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
        UIHelper.setLockApp(this, -1);
        // Wait for pin mode disabled, or finishAffinity() won't work
        // 等待屏幕固定被关闭，不然finishAffinity()没用
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Log.e(TAG, "An error was occurred while waiting screen pinning to close: ", e);
        }
        // kill myself
        finishAffinity();
    }

    private void setDialog(Dialog dialog) {
        if (this.dialog != null && this.dialog.isShowing()) this.dialog.dismiss();
        this.dialog = dialog;
    }

    @Override
    protected void onDestroy() {
        if (UIHelper.getLockApp(MainActivity.this) != -1) UIHelper.setLockApp(MainActivity.this, -1);
        // Unregister the receiver on destroy
        // 关掉app时注销掉接收器
        receiverRegister(false);
        // 停止计时任务 Stop timer
        if (mTimer != null) mTimer.cancel();
        super.onDestroy();
        // Enable touch screen
        UIHelper.setTouchscreenState(true, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        batteryAccurate();
        // Only re-cover the call UI when a fake-call session is still active.
        if (ApplicationHelper.dialing
                && ApplicationHelper.fakeCallNumber != null
                && !ApplicationHelper.fakeCallNumber.isEmpty()) {
            Intent cover = new Intent(this, FakeInCallActivity.class)
                    .putExtra(FakeInCallActivity.EXTRA_NUMBER, ApplicationHelper.fakeCallNumber)
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(cover);
            overridePendingTransition(0, 0);
            return;
        }
        // Hangup / residual state cleanup.
        if (ApplicationHelper.dialing) {
            ApplicationHelper.dialing = false;
            ApplicationHelper.fakeCallNumber = "";
        }
        // After call session ends, keep pin on MainActivity so keys work again.
        UIHelper.setLockApp(this, getTaskId());
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ApplicationHelper.dialing) return true;
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (ApplicationHelper.dialing) return true;
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d(TAG,"onTouch");
        if (mStyle.equals(UIHelper.STYLE_PHONE)) return false;

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (!UIHelper.intentStarterDebounce(SubActivity.class)) {
                Intent intent = new Intent().setClass(MainActivity.this, SubActivity.class)
                        .putExtra(SubActivity.HIDE_ACTION_BAR, true);
                if (false) {// Detect if playing music
                    intent.putExtra(SubActivity.TARGET_FRAGMENT, MusicPlayerFragment.class.getName());

                } else {
                    intent.putExtra(SubActivity.TARGET_FRAGMENT, MenuFragment.class.getName());
                }
                startActivity(intent);
                // Disable transition anim
                // 去掉过渡动画
                overridePendingTransition(0, 0);
            }
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mStyle.equals(UIHelper.STYLE_PHONE)) {
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
        if (mStyle.equals(UIHelper.STYLE_PHONE)) {
            if (mLocked) { // 需要解锁的情况 Need star key unlock
                if (keyCode == KeyEvent.KEYCODE_MENU) {
                    setDialog(UIHelper.showCustomDialog(this, R.string.dialog_press_star_unlock, (dialogInterface, keyCode1, keyEvent) -> {
                        if (keyCode1 == KeyEvent.KEYCODE_STAR) {
                            onUnlocked();
                        }
                        return true;
                    }));
                } else {
                    if (!mKeyLongPressed)
                        setDialog(UIHelper.showCustomDialog(this, R.string.dialog_long_press_star_unlock, (dialogInterface, keyCode1, keyEvent) -> {
                            if (keyCode1 != KeyEvent.KEYCODE_BACK) {
                                if (keyEvent.getRepeatCount() >= 2) {
                                    mKeyLongPressed = true;
                                    onUnlocked();
                                }
                            }
                            return true;
                        }));
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
                            .getBoolean(SettingsConstants.PREF_DPAD_CENTER_OPEN_MENU, false)
                    ) return super.onKeyUp(keyCode, event);
                }
                // Open menu UI
                // 打开菜单界面
                UIHelper.startIntent(MainActivity.this, SubActivity.class);

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
                Bundle extra = new Bundle();
                extra.putString(DialerFragment.ARG_INPUT, key);
                if (!UIHelper.intentStarterDebounce(SubActivity.class)) {
                    startActivity(
                            new Intent().setClass(MainActivity.this, SubActivity.class)
                                    .putExtra(SubActivity.TARGET_FRAGMENT, DialerFragment.class.getName())
                                    .putExtra(SubActivity.FRAGMENT_ARGS, extra)
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
        mKeyLongPressed = false;
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
        /*
        // New dialog should be opened before closing the old one.
        Dialog dialog2;
        dialog2 = UIHelper.showCustomDialog(MainActivity.this, R.string.dialog_unlocked, null);
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = dialog2;
        setFooterBar(R.string.main_leftButton);
        mLocked = false;
         */
        setDialog(UIHelper.showCustomDialog(MainActivity.this, R.string.dialog_unlocked, null));
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
                boolean showSecond = defaultPref.getBoolean(SettingsConstants.PREF_TIME_SHOW_SECOND, false);
                String pattern = showSecond ? "HH:mm:ss" : "HH:mm";
                format = new SimpleDateFormat(pattern, Locale.getDefault());
                break;
            }
            case DATE: {
                String pattern = UIHelper.STYLE_PLAYER.equals(mStyle) ? "yyyy-MM-dd" : getResources().getString(R.string.date_format);
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
                            weekView = UIHelper.STYLE_PLAYER.equals(mStyle) ? findViewById(R.id.week) : null;
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
        if (mStyle.equals(UIHelper.STYLE_PLAYER)) {
            TextView battery_view = findViewById(R.id.battery);
            battery_view.setText(battery+"%");
        }
        else if (mStyle.equals(UIHelper.STYLE_PHONE)) {
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
        if (mStyle.equals(UIHelper.STYLE_PLAYER)) {
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
                    mBatteryChargingAnimTimer = new Timer();
                    mBatteryChargingAnimTimer.schedule(new TimerTask() {
                        int i = mBatteryLevel;
                        @Override
                        public void run() {
                            if (i < batteryIcons.length - 1) {
                                i += 1;
                            } else {
                                i = mBatteryLevel;
                            }
                            setBatteryIcons(i);
                        }
                    },0,1000);
                }

            } else {
                if (mShowAccurateBattery) connection_view.setText(R.string.not_charging);
                else {
                    if (mBatteryChargingAnimTimer != null) mBatteryChargingAnimTimer.cancel();
                    mBatteryChargingAnimTimer = null;
                    setBatteryIcons(mBatteryLevel);
                }
            }
        }
    }

    private void setBatteryIcons(int level) {
        View main = findViewById(R.id.Main);
        main.post( () -> {
            Drawable overlay;
            try {
                overlay = ContextCompat.getDrawable(this, batteryIcons[level]);
            } catch (IndexOutOfBoundsException e) {
                overlay = null;
            }
            if (overlay != null) {
                int screenWidth = main.getWidth(), margin = 10, scale = (int) (4 / mScale);
                overlay.setBounds(screenWidth - margin - overlay.getIntrinsicWidth() / scale, margin, screenWidth - margin, margin + overlay.getIntrinsicHeight() / scale);
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
                    case UIHelper.STYLE_PLAYER -> findViewById(R.id.week);
                    default -> findViewById(R.id.lunarDate); // case UIHelper.STYLE_PHONE
                };
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mShowAccurateBattery = sharedPrefs.getBoolean(SettingsConstants.PREF_SHOW_ACCURATE_BATTERY, false);
        statusBarView.setVisibility(mShowAccurateBattery? View.VISIBLE : View.INVISIBLE);
        if (mStyle.equals(UIHelper.STYLE_PHONE)) dateView.setVisibility(mShowAccurateBattery? View.INVISIBLE : View.VISIBLE);

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
            } else Log.w(TAG, "Receiver has already registered!");
            if (!mPhoneStateRegistered) {
                IntentFilter phoneFilter = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
                registerReceiver(mPhoneStateReceiver, phoneFilter);
                mPhoneStateRegistered = true;
            }
        } else {
            if (mReceiverRegistered) {
                unregisterReceiver(mReceiver);
                mReceiverRegistered = false;
                Log.d(TAG, "Receiver unregistered!");
            } else Log.w(TAG, "Receiver was not registered yet!");
            if (mPhoneStateRegistered) {
                unregisterReceiver(mPhoneStateReceiver);
                mPhoneStateRegistered = false;
            }
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
        if (! UIHelper.checkExitMethod(this, UIHelper.EXIT_METHOD_DPAD)) return;
        if (mKeyCount < 0 || mKeyCount > mKeyAction.length - 1) mKeyCount = 0;

        if (keycode != mKeyAction[mKeyCount]) mKeyCount = 0;
        else if (mKeyCount < mKeyAction.length - 1) mKeyCount++;
        else if (mKeyCount == mKeyAction.length - 1) {
            mKeyCount = 0;
            exit();
        }
        Log.d(TAG,"counter(): validKeyCount="+ mKeyCount);
    }

}