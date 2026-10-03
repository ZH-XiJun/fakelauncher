package com.wtbruh.fakelauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.wtbruh.fakelauncher.service.InCallService;
import com.wtbruh.fakelauncher.ui.BaseAppCompatActivity;
import com.wtbruh.fakelauncher.utils.CallHelper;

/**
 * 呼出通话界面（独立 Activity，避免拨号时 lock task 解除导致 SubActivity 被关、界面消失）。
 * 布局完全复用呼入来电界面（activity_incoming_call.xml），保证呼出通话中界面与呼入通话中界面完全一致。
 * 拨号中：大字"拨号中"+号码+图标+左下"免提"+右下"挂断"；
 * 通话中：大字"通话中"+号码+图标+计时+提示"通话中 按任意键挂断"+左下"免提"+右下"挂断"。
 * 监听 InCallService 的 CALL_STATE 广播：ACTIVE=通话中，DISCONNECTED=对方挂断（关闭界面）。
 */
public class OutgoingCallActivity extends BaseAppCompatActivity {
    public static final String EXTRA_NUMBER = "number";
    private static final String TAG = OutgoingCallActivity.class.getSimpleName();

    private TextView numberView;     // 大字：拨号中 / 通话中
    private TextView subView;        // 小字：号码
    private TextView durationView;   // 计时
    private TextView footerView;     // 提示
    private TextView btnAnswer;      // 左下：免提
    private TextView btnIgnore;      // 右下：挂断
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean connected = false;
    private boolean timerStarted = false;
    private boolean speakerOn = false;
    private long callStartElapsedRealtime = 0;
    private long lastKeyHandledAt = 0;
    private String currentNumber = "";
    private BroadcastReceiver stateReceiver;
    private boolean receiverRegistered = false;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            updateDuration();
            handler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_incoming_call);

        numberView = findViewById(R.id.incoming_number);
        subView = findViewById(R.id.incoming_sub);
        durationView = findViewById(R.id.incoming_duration);
        footerView = findViewById(R.id.incoming_footer);
        btnAnswer = findViewById(R.id.btn_answer);
        btnIgnore = findViewById(R.id.btn_ignore);

        // 初始：拨号中
        currentNumber = getIntent() == null ? "" : getIntent().getStringExtra(EXTRA_NUMBER);
        if (TextUtils.isEmpty(currentNumber)) currentNumber = getString(R.string.fake_incall_unknown);
        numberView.setText(R.string.fake_incall_dialing); // 拨号中
        if (subView != null) {
            subView.setText(currentNumber);
            subView.setVisibility(View.VISIBLE);
        }
        durationView.setVisibility(View.GONE);
        footerView.setVisibility(View.GONE);
        btnAnswer.setText(R.string.speaker_on); // 免提
        btnAnswer.setOnClickListener(v -> toggleSpeaker());
        btnAnswer.setVisibility(View.VISIBLE);
        btnIgnore.setText(R.string.incoming_hangup); // 挂断
        btnIgnore.setOnClickListener(v -> hangUp());

        registerStateReceiver();
        // 兜底自检：呼出失败（系统无通话）时关闭界面
        handler.postDelayed(checkCallStateRunnable, 1500L);
        handler.postDelayed(checkCallStateRunnable, 4000L);

        // 伪装锁定期间呼出会触发系统解除 lock task；立即重新锁定，触屏不恢复
        try {
            if (com.wtbruh.fakelauncher.utils.ContentProvider.getTaskId(this)
                    != com.wtbruh.fakelauncher.utils.ContentProvider.NOT_SET) {
                startLockTask();
                Log.d(TAG, "lock task re-acquired for outgoing call");
            }
        } catch (Exception e) {
            Log.e(TAG, "re-lock failed", e);
        }
    }

    /** 兜底：延迟检查系统真实通话状态，呼出失败/已结束则关闭界面。 */
    private final Runnable checkCallStateRunnable = new Runnable() {
        @Override
        public void run() {
            if (connected) return;
            try {
                TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                int cs = tm != null ? tm.getCallState() : TelephonyManager.CALL_STATE_IDLE;
                Log.d(TAG, "checkCallState: sys=" + cs);
                // IDLE=无通话（呼出失败/已挂断）；RINGING/OFFHOOK=通话进行中，保持界面
                if (cs == TelephonyManager.CALL_STATE_IDLE) {
                    finish();
                }
            } catch (Throwable t) {
                Log.e(TAG, "checkCallState failed", t);
            }
        }
    };

    private void registerStateReceiver() {
        if (receiverRegistered) return;
        stateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!InCallService.ACTION_CALL_STATE.equals(intent.getAction())) return;
                int state = intent.getIntExtra(InCallService.EXTRA_CALL_STATE, -1);
                Log.d(TAG, "state bcast: " + state);
                if (state == android.telecom.Call.STATE_ACTIVE) {
                    onCallActive();
                } else if (state == android.telecom.Call.STATE_DISCONNECTED) {
                    finish();
                }
            }
        };
        IntentFilter filter = new IntentFilter(InCallService.ACTION_CALL_STATE);
        try {
            registerReceiver(stateReceiver, filter);
            receiverRegistered = true;
        } catch (Throwable t) {
            Log.e(TAG, "register state receiver failed", t);
        }
    }

    /** 接通：切到通话中（与呼入通话中界面完全一致）。 */
    private void onCallActive() {
        if (connected) return;
        connected = true;
        numberView.setText(R.string.fake_incall_status); // 通话中
        if (subView != null) {
            if (!TextUtils.isEmpty(currentNumber)
                    && !getString(R.string.fake_incall_unknown).equals(currentNumber)) {
                subView.setText(currentNumber);
                subView.setVisibility(View.VISIBLE);
            } else {
                subView.setVisibility(View.GONE);
            }
        }
        durationView.setText("00:00");
        durationView.setVisibility(View.VISIBLE);
        footerView.setText(getString(R.string.incoming_active_hint)); // 通话中 按任意键挂断
        footerView.setVisibility(View.VISIBLE);
        callStartElapsedRealtime = SystemClock.elapsedRealtime();
        startTimerIfNeeded();
    }

    private void startTimerIfNeeded() {
        if (timerStarted) return;
        if (callStartElapsedRealtime <= 0L) callStartElapsedRealtime = SystemClock.elapsedRealtime();
        timerStarted = true;
        handler.removeCallbacks(tickRunnable);
        handler.post(tickRunnable);
    }

    private void updateDuration() {
        if (durationView == null) return;
        long elapsedMs = Math.max(0L, SystemClock.elapsedRealtime() - callStartElapsedRealtime);
        long elapsedSeconds = elapsedMs / 1000L;
        long minutes = elapsedSeconds / 60L;
        long seconds = elapsedSeconds % 60L;
        durationView.setText(String.format("%02d:%02d", minutes, seconds));
    }

    /** 挂断：真实挂断并关闭界面。 */
    private void hangUp() {
        Log.d(TAG, "hangUp");
        boolean ended = CallHelper.endCallViaService() || CallHelper.endCall(this);
        handler.removeCallbacks(tickRunnable);
        if (!ended) {
            Log.w(TAG, "hangUp: endCall failed, closing anyway");
        }
        finish();
    }

    /** 免提 / 手机切换（三层音频路由，与呼入一致）。 */
    private void toggleSpeaker() {
        try {
            speakerOn = !speakerOn;
            boolean routed = false;
            android.telecom.Call call = InCallService.sCurrentCall;
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
            btnAnswer.setText(speakerOn ? R.string.speaker_off : R.string.speaker_on);
        } catch (Exception e) {
            Log.e(TAG, "toggleSpeaker failed", e);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 忽略长按产生的 repeat 事件
        if (event.getRepeatCount() > 0) return true;
        long now = System.currentTimeMillis();
        if (now - lastKeyHandledAt < 400) return true;
        lastKeyHandledAt = now;
        return handleKey(keyCode);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // 按键动作只在 keyDown 处理一次
        return true;
    }

    /** 按键统一处理：左菜单=免提；拨号中=右菜单/返回/挂机键挂断；通话中=任意键挂断（除左菜单）。 */
    private boolean handleKey(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SOFT_LEFT) {
            Log.d(TAG, "toggle speaker");
            toggleSpeaker();
            return true;
        }
        if (connected) {
            Log.d(TAG, "active call, key ends call");
            hangUp();
            return true;
        }
        // 拨号中：右菜单键 / 返回键 / 挂机键 = 挂断，其他键忽略
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ENDCALL
                || keyCode == KeyEvent.KEYCODE_SOFT_RIGHT) {
            hangUp();
            return true;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        hangUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(tickRunnable);
        if (receiverRegistered) {
            try {
                unregisterReceiver(stateReceiver);
            } catch (Exception ignored) { }
            receiverRegistered = false;
        }
    }
}
