package com.wtbruh.fakelauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.preference.PreferenceManager;

import com.wtbruh.fakelauncher.constants.SettingsConstants;
import com.wtbruh.fakelauncher.receiver.IncomingCallReceiver;
import com.wtbruh.fakelauncher.ui.BaseAppCompatActivity;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;

/**
 * 来电显示界面（老人机风格）：
 * 电话呼入时由 {@link IncomingCallReceiver} 启动，自动亮屏并全屏显示来电号码。
 */
public class IncomingCallActivity extends BaseAppCompatActivity {

    public static final String EXTRA_NUMBER = "number";
    public static final String ACTION_UPDATE_NUMBER = "com.wtbruh.fakelauncher.action.UPDATE_NUMBER";
    public static final String ACTION_CALL_ACTIVE = "com.wtbruh.fakelauncher.action.CALL_ACTIVE";
    private static final String TAG = IncomingCallActivity.class.getSimpleName();

    private TextView numberView;
    private TextView footerView;
    private String currentNumber = "";
    private boolean isActive = false;   // 已接听（通话中）
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private boolean speakerOn = false; // 免提状态（通话中界面左按钮）
    private long lastKeyHandledAt = 0;  // 按键防抖：down/up 只处理一次
    private TextView durationView;     // 通话计时（接通后显示）
    private boolean timerStarted = false;
    private long callStartElapsedRealtime = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            updateDuration();
            handler.postDelayed(this, 1000L);
        }
    };

    private final BroadcastReceiver closeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (IncomingCallReceiver.ACTION_CLOSE_INCOMING.equals(intent.getAction())) {
                finish();
            } else if (ACTION_CALL_ACTIVE.equals(intent.getAction())) {
                // 通话接通：切换到"通话中"界面
                if (!isActive) onAnswered();
            } else if (ACTION_UPDATE_NUMBER.equals(intent.getAction())) {
                applyNumber(intent.getStringExtra(EXTRA_NUMBER));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 来电时亮屏 + 锁屏之上显示
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_incoming_call);
        numberView = findViewById(R.id.incoming_number);
        footerView = findViewById(R.id.incoming_footer);
        durationView = findViewById(R.id.incoming_duration);
        applyNumber(getIntent().getStringExtra(EXTRA_NUMBER));
        setFooterText(getString(R.string.incoming_ignore_hint));
        // 呼入铃声 + 振动（成为默认通话应用后系统不再自动播放）
        startAlert();
        // 触屏大按钮：接听 / 挂断（老人机风格，实体键之外的第二通道）
        android.view.View btnAnswer = findViewById(R.id.btn_answer);
        android.view.View btnIgnore = findViewById(R.id.btn_ignore);
        if (btnAnswer != null) {
            btnAnswer.setOnClickListener(v -> {
                Log.d(TAG, "touch answer");
                doAnswer();
            });
        }
        if (btnIgnore != null) {
            btnIgnore.setOnClickListener(v -> {
                Log.d(TAG, "touch hangup");
                hangUp();
            });
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(IncomingCallReceiver.ACTION_CLOSE_INCOMING);
        filter.addAction(ACTION_UPDATE_NUMBER);
        filter.addAction(ACTION_CALL_ACTIVE);
        registerReceiver(closeReceiver, filter);

        // 启动自检：若通话已接通（如进程重启/界面重建），直接进入通话中界面
        try {
            android.telecom.Call cur =
                    com.wtbruh.fakelauncher.service.InCallService.sCurrentCall;
            if (cur != null && (cur.getState() == android.telecom.Call.STATE_ACTIVE
                    || cur.getState() == android.telecom.Call.STATE_DIALING
                    || cur.getState() == android.telecom.Call.STATE_CONNECTING)) {
                Log.d(TAG, "onCreate already active state=" + cur.getState());
                onAnswered();
            }
        } catch (Throwable t) {
            Log.e(TAG, "onCreate active check failed", t);
        }

        // 伪装锁定期间来电会触发系统解除 lock task（拨号器不在白名单）。
        // 这里立即重新锁定，让来电界面保持在伪装锁定下，触屏不恢复。
        try {
            if (com.wtbruh.fakelauncher.utils.ContentProvider.getTaskId(this)
                    != com.wtbruh.fakelauncher.utils.ContentProvider.NOT_SET) {
                startLockTask();
                Log.d(TAG, "lock task re-acquired for incoming call");
            }
        } catch (Exception e) {
            Log.e(TAG, "re-lock failed", e);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        applyNumber(intent.getStringExtra(EXTRA_NUMBER));
    }

    private void applyNumber(String number) {
        if (TextUtils.isEmpty(number)) number = getString(R.string.fake_incall_unknown);
        currentNumber = number;
        TextView subView = findViewById(R.id.incoming_sub);
        // 号码匹配通讯录：有联系人显示姓名（大字）+ 号码（小字），无联系人显示号码
        String contactName = lookupContactName(number);
        if (contactName != null && !contactName.isEmpty()) {
            if (numberView != null) {
                numberView.setText(contactName);
                numberView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                numberView.setSingleLine(true);
                numberView.setSelected(true);
            }
            if (subView != null && !getString(R.string.fake_incall_unknown).equals(number)) {
                subView.setText(number);
                subView.setVisibility(android.view.View.VISIBLE);
            }
        } else {
            if (numberView != null) {
                numberView.setText(number);
                numberView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                numberView.setSingleLine(true);
                numberView.setSelected(true);
            }
            if (subView != null) subView.setVisibility(android.view.View.GONE);
        }
        // 若号码仍未知（部分 ROM 广播不带号码），延迟反射读取电话内部状态
        if (getString(R.string.fake_incall_unknown).equals(number)) {
            retryReflectNumber(0);
        }
    }

    /** 延迟反射读取真实来电号码（最多重试 10 次，覆盖 RINGING 建立延迟）。 */
    private void retryReflectNumber(final int attempt) {
        if (attempt > 12) return;
        // 首次延迟 1.5s（等待呼叫进入 RINGING、connection 建立），之后每 900ms 重试
        long delay = attempt == 0 ? 1500 : 900;
        new android.os.Handler(getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                String n = reflectIncomingNumber();
                if (n != null && !n.isEmpty() && !n.contains("?") && n.length() >= 3) {
                    currentNumber = n;
                    if (numberView != null) numberView.setText(n);
                    Log.d(TAG, "reflect got number: " + n);
                } else {
                    retryReflectNumber(attempt + 1);
                }
            }
        }, delay);
    }

    /** 通过通讯录查询号码对应的联系人姓名（无记录返回空串）。 */
    private String lookupContactName(String number) {
        if (number == null || number.isEmpty()
                || getString(R.string.fake_incall_unknown).equals(number)) return "";
        try {
            Uri uri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            Cursor cur = getContentResolver().query(uri,
                    new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
            if (cur != null) {
                try {
                    if (cur.moveToFirst()) {
                        return cur.getString(0);
                    }
                } finally {
                    cur.close();
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "lookupContactName failed: " + e.getMessage());
        }
        return "";
    }

    /**
     * 反射读取内部电话状态：PhoneFactory.getDefaultPhone().getRingingCall()
     * .getEarliestConnection().getAddress()（真实来电号码）。
     * 展锐 ROM 广播不带号码时走此通道；hidden API 被禁则返回空。
     */
    private String reflectIncomingNumber() {
        try {
            Class<?> phoneFactory = Class.forName("com.android.internal.telephony.PhoneFactory");
            java.lang.reflect.Method getDefaultPhone = phoneFactory.getMethod("getDefaultPhone");
            Object phone = getDefaultPhone.invoke(null);
            if (phone == null) return "";

            // 用接口类精确查找方法，避免实现类（PhoneProxy 等）找不到方法
            Class<?> phoneIface = Class.forName("com.android.internal.telephony.Phone");
            java.lang.reflect.Method getRingingCall = phoneIface.getMethod("getRingingCall");
            Object call = getRingingCall.invoke(phone);
            if (call == null) {
                Log.d(TAG, "reflect: ringing call is null");
                return "";
            }
            Class<?> callCls = Class.forName("com.android.internal.telephony.Call");
            java.lang.reflect.Method getEarliest = callCls.getMethod("getEarliestConnection");
            Object conn = getEarliest.invoke(call);
            if (conn == null) {
                Log.d(TAG, "reflect: connection is null");
                return "";
            }
            Class<?> connCls = Class.forName("com.android.internal.telephony.Connection");
            java.lang.reflect.Method getAddress = connCls.getMethod("getAddress");
            Object addr = getAddress.invoke(conn);
            String n = addr != null ? addr.toString() : "";
            Log.d(TAG, "reflect: address=" + n);
            return n;
        } catch (Throwable t) {
            // 解包 InvocationTargetException 看真实原因
            Throwable cause = t;
            if (t instanceof java.lang.reflect.InvocationTargetException
                    && t.getCause() != null) {
                cause = t.getCause();
            }
            Log.d(TAG, "reflect number failed: " + t.getClass().getSimpleName()
                    + " cause=" + cause.getClass().getSimpleName()
                    + ": " + cause.getMessage());
            return "";
        }
    }

    private void setFooterText(String text) {
        if (footerView != null) footerView.setText(text);
    }

    /** 播放呼入铃声（循环直到接听/挂断）+ 周期振动。按情景模式控制。 */
    private void startAlert() {
        // 情景模式：ring_vibrate=响铃并振动 / vibrate_only=仅振动 / silent=静音
        String mode = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(SettingsConstants.PREF_SCENE_MODE, "ring_vibrate");
        boolean ring = !"vibrate_only".equals(mode) && !"silent".equals(mode);
        boolean vibrate = !"silent".equals(mode);
        if (ring) {
            try {
                Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                if (uri != null) {
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setDataSource(this, uri);
                    mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build());
                    mediaPlayer.setLooping(true);
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                }
            } catch (Exception e) {
                Log.e(TAG, "ringtone failed: " + e.getMessage());
            }
        }
        if (vibrate) {
            try {
                vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    vibrator.vibrate(new long[]{0, 800, 500, 800, 500}, 0);
                }
            } catch (Exception e) {
                Log.e(TAG, "vibrate failed: " + e.getMessage());
            }
        }
    }

    /** 停止铃声与振动。 */
    private void stopAlert() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            }
            mediaPlayer = null;
        } catch (Exception ignored) { }
        try {
            if (vibrator != null) {
                vibrator.cancel();
            }
            vibrator = null;
        } catch (Exception ignored) { }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 忽略长按产生的 repeat 事件（否则接听后 repeat 会触发挂断）
        if (event.getRepeatCount() > 0) return true;
        long now = System.currentTimeMillis();
        if (now - lastKeyHandledAt < 400) return true;
        lastKeyHandledAt = now;
        return handleKey(keyCode);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // 所有按键动作只在 keyDown 处理一次；keyUp 直接吞掉，
        // 防止“接听后松开按键”被当成挂断操作
        return true;
    }

    /** 按键统一处理：确认键=接听/挂断，其他键=忽略（挂断）。 */
    private boolean handleKey(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MENU
                || keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_CALL) {
            if (isActive) {
                if (keyCode == KeyEvent.KEYCODE_MENU) {
                    Log.d(TAG, "active call, toggle speaker");
                    toggleSpeaker();
                } else {
                    Log.d(TAG, "active call, key ends call");
                    hangUp();
                }
            } else {
                Log.d(TAG, "key pressed, answering call");
                doAnswer();
            }
            return true;
        }
        if (isActive) {
            if (keyCode == KeyEvent.KEYCODE_SOFT_LEFT) {
                Log.d(TAG, "active call, toggle speaker (soft left)");
                toggleSpeaker();
            } else {
                Log.d(TAG, "active call, other key ends call");
                hangUp();
            }
            return true;
        }
        // 其他任意按键：忽略（真正挂断）
        Log.d(TAG, "key pressed, ignoring call");
        hangUp();
        return true;
    }

    /** 免提 / 听筒切换：三层音频路由方案（与呼出通话界面一致）。 */
    private void toggleSpeaker() {
        try {
            speakerOn = !speakerOn;
            boolean routed = false;
            // ① Call.setAudioRoute（反射；2=扬声器 1=听筒）
            android.telecom.Call call =
                    com.wtbruh.fakelauncher.service.InCallService.sCurrentCall;
            if (call != null) {
                try {
                    java.lang.reflect.Method m =
                            android.telecom.Call.class.getMethod("setAudioRoute", int.class);
                    m.invoke(call, speakerOn ? 2 : 1);
                    routed = true;
                    Log.d(TAG, "toggleSpeaker via setAudioRoute: speakerOn=" + speakerOn);
                } catch (Throwable t) {
                    Log.e(TAG, "setAudioRoute reflect failed", t);
                }
            }
            // ② AudioManager.setCommunicationDevice（API 31+）
            if (!routed) {
                try {
                    AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    if (am != null && Build.VERSION.SDK_INT >= 31) {
                        java.util.List<AudioDeviceInfo> devices = am.getAvailableCommunicationDevices();
                        for (AudioDeviceInfo d : devices) {
                            int type = d.getType();
                            if (speakerOn && type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                                am.setCommunicationDevice(d);
                                routed = true;
                                break;
                            }
                            if (!speakerOn && type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE) {
                                am.setCommunicationDevice(d);
                                routed = true;
                                break;
                            }
                        }
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "setCommunicationDevice failed", t);
                }
            }
            // ③ setMode + setSpeakerphoneOn 兜底
            if (!routed) {
                try {
                    AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    if (am != null) {
                        am.setMode(AudioManager.MODE_IN_COMMUNICATION);
                        am.setSpeakerphoneOn(speakerOn);
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "setSpeakerphoneOn failed", t);
                }
            }
            // 更新左按钮文字：免提 <-> 手机
            TextView ba = findViewById(R.id.btn_answer);
            if (ba != null) {
                ba.setText(getString(speakerOn ? R.string.speaker_off : R.string.speaker_on));
            }
        } catch (Exception e) {
            Log.e(TAG, "toggleSpeaker failed", e);
        }
    }

    private void updateDuration() {
        if (durationView == null || !isActive) return;
        long elapsedMs = Math.max(0L, SystemClock.elapsedRealtime() - callStartElapsedRealtime);
        long elapsedSeconds = elapsedMs / 1000L;
        long minutes = elapsedSeconds / 60L;
        long seconds = elapsedSeconds % 60L;
        durationView.setText(String.format("%02d:%02d", minutes, seconds));
    }

    /** 挂断：优先用 InCallService 的 Call.disconnect()，失败再退回反射 endCall。 */
    private void hangUp() {
        boolean ok = com.wtbruh.fakelauncher.utils.CallHelper.endCallViaService();
        if (!ok) ok = com.wtbruh.fakelauncher.utils.CallHelper.endCall(this);
        Log.d(TAG, "hangUp ok=" + ok);
        finish();
    }

    /** 接听：优先 InCallService 公开 API，失败再反射；最终进入通话中界面。 */
    private void doAnswer() {
        if (answerCall()) {
            onAnswered();
            return;
        }
        // answerCall 失败：可能 ROM 已用系统键（如菜单键=接听）先接通了，
        // 延迟确认通话状态，避免误报"接听失败"且漏停铃声振动。
        Log.d(TAG, "answerCall returned false, delaying confirm");
        confirmAnswered(0);
    }

    /** 延迟确认接听结果：最多尝试两次（600ms / 再 900ms），确认到通话中才切换界面。 */
    private void confirmAnswered(final int attempt) {
        new android.os.Handler(getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                android.telecom.Call c =
                        com.wtbruh.fakelauncher.service.InCallService.sCurrentCall;
                if (c != null && (c.getState() == android.telecom.Call.STATE_ACTIVE
                        || c.getState() == android.telecom.Call.STATE_DIALING
                        || c.getState() == android.telecom.Call.STATE_CONNECTING)) {
                    Log.d(TAG, "confirmed active state=" + c.getState());
                    onAnswered();
                } else if (attempt < 1) {
                    Log.d(TAG, "confirm retry, state=" + (c == null ? "null" : c.getState()));
                    confirmAnswered(attempt + 1);
                } else {
                    Log.d(TAG, "confirm failed, state=" + (c == null ? "null" : c.getState()));
                    setFooterText(getString(R.string.incoming_answer_fail));
                }
            }
        }, attempt == 0 ? 600 : 900);
    }

    /** 进入通话中状态：大字显示"通话中"，号码移到小字，隐藏图标与按钮。 */
    private void onAnswered() {
        isActive = true;
        stopAlert();
        if (numberView != null) {
            numberView.setText(R.string.fake_incall_status); // "通话中"
        }
        TextView subView = findViewById(R.id.incoming_sub);
        if (subView != null) {
            if (!TextUtils.isEmpty(currentNumber)
                    && !getString(R.string.fake_incall_unknown).equals(currentNumber)) {
                subView.setText(currentNumber);
                subView.setVisibility(android.view.View.VISIBLE);
            } else {
                subView.setVisibility(android.view.View.GONE);
            }
        }
        // 通话中界面保留来电图标
        android.widget.ImageView icon = findViewById(R.id.incoming_icon);
        if (icon != null) icon.setVisibility(android.view.View.VISIBLE);
        // 通话计时：接通起走秒（与呼出界面统一）
        if (durationView != null) {
            durationView.setText("00:00");
            durationView.setVisibility(android.view.View.VISIBLE);
        }
        callStartElapsedRealtime = SystemClock.elapsedRealtime();
        timerStarted = false;
        handler.removeCallbacks(tickRunnable);
        handler.post(tickRunnable);
        setFooterText(getString(R.string.incoming_active_hint));
        // 左按钮改为"免提"（点击切换免提/听筒）
        TextView ba = findViewById(R.id.btn_answer);
        if (ba != null) {
            ba.setText(getString(R.string.speaker_on)); // 免提
            ba.setOnClickListener(v -> toggleSpeaker());
        }
        // 右按钮保留"挂断"（点击真正挂断）
        TextView bi = findViewById(R.id.btn_ignore);
        if (bi != null) {
            bi.setText(getString(R.string.incoming_hangup)); // 挂断
            bi.setOnClickListener(v -> hangUp());
        }
    }

    /** 接听来电：InCallService 的 Call.answer()（公开 API）优先，反射兜底。 */
    private boolean answerCall() {
        try {
            android.telecom.Call c =
                    com.wtbruh.fakelauncher.service.InCallService.sCurrentCall;
            if (c != null) {
                int st = c.getState();
                if (st == android.telecom.Call.STATE_ACTIVE
                        || st == android.telecom.Call.STATE_DIALING
                        || st == android.telecom.Call.STATE_CONNECTING) {
                    Log.d(TAG, "call already active, state=" + st);
                    return true;
                }
                if (st == android.telecom.Call.STATE_RINGING) {
                    c.answer(0); // VideoProfile.STATE_AUDIO_ONLY
                    Log.d(TAG, "answered via Call.answer()");
                    return true;
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "Call.answer failed: " + t.getMessage(), t);
        }
        try {
            android.telephony.TelephonyManager tm =
                    (android.telephony.TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            java.lang.reflect.Method m = tm.getClass().getMethod("getITelephony");
            Object it = m.invoke(tm);
            java.lang.reflect.Method a = it.getClass().getMethod("answerRingingCall");
            a.invoke(it);
            Log.d(TAG, "call answered via ITelephony");
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "answerCall reflect failed: " + t.getMessage(), t);
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(tickRunnable);
        stopAlert();
        try {
            unregisterReceiver(closeReceiver);
        } catch (Exception ignored) { }
        super.onDestroy();
    }
}
