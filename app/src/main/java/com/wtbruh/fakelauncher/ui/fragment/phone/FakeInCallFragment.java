package com.wtbruh.fakelauncher.ui.fragment.phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.SubActivity;
import com.wtbruh.fakelauncher.service.InCallService;
import com.wtbruh.fakelauncher.ui.fragment.BaseFragment;
import com.wtbruh.fakelauncher.utils.CallHelper;
import com.wtbruh.fakelauncher.utils.UIHelper;

/**
 * 呼出通话界面（统一风格）：
 * 拨号中（大字"拨号中"+号码+图标+右挂断）→ 接通后"通话中"（+计时+左免提/右挂断）。
 * 监听 InCallService 的 CALL_STATE 广播：ACTIVE=通话中，DISCONNECTED=对方挂断（关闭界面）。
 */
public class FakeInCallFragment extends BaseFragment {
    public static final String ARG_NUMBER = "number";
    private static final String TAG = FakeInCallFragment.class.getSimpleName();

    private TextView statusView;    // 大字：拨号中 / 通话中
    private TextView numberView;    // 小字：号码
    private TextView durationView;  // 计时
    private ImageView iconView;     // 图标
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean connected = false;   // 已接通
    private boolean timerStarted = false;
    private boolean finish = false;
    private boolean speakerOn = false;
    private long callStartElapsedRealtime = 0;
    private BroadcastReceiver stateReceiver;
    private boolean receiverRegistered = false;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if (finish) return;
            updateDuration();
            handler.postDelayed(this, 1000L);
        }
    };

    public static FakeInCallFragment newInstance(Bundle args) {
        FakeInCallFragment fragment = new FakeInCallFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_fake_incall, container, false);
        init();
        return rootView;
    }

    private void init() {
        // 通话界面保持屏幕常亮
        try {
            requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } catch (Throwable t) {
            Log.e(TAG, "KEEP_SCREEN_ON failed", t);
        }
        statusView = findViewById(R.id.fake_incall_status);
        numberView = findViewById(R.id.fake_incall_number);
        durationView = findViewById(R.id.fake_incall_duration);
        iconView = findViewById(R.id.fake_incall_icon);

        // 初始：拨号中
        statusView.setText(R.string.fake_incall_dialing);
        String num = "";
        if (getArguments() != null) num = getArguments().getString(ARG_NUMBER);
        if (TextUtils.isEmpty(num)) num = getString(R.string.fake_incall_unknown);
        numberView.setText(num);
        numberView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        numberView.setSingleLine(true);
        numberView.setSelected(true);
        durationView.setVisibility(View.GONE);

        // 监听通话状态：接通/挂断
        registerStateReceiver();

        // 兜底自检：呼出失败（未进入 InCallService 通话流程）时关闭界面
        handler.postDelayed(checkCallStateRunnable, 1500L);
        handler.postDelayed(checkCallStateRunnable, 4000L);

        // Android 13 返回键：真实挂断 + 关闭界面
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        boolean ended = CallHelper.endCallViaService() || CallHelper.endCall(requireContext());
                        if (ended) {
                            setEnabled(false);
                            requireActivity().onBackPressed();
                        } else {
                            UIHelper.showCustomDialog(requireContext(), R.string.fake_incall_end_fail, null);
                        }
                    }
                });
    }

    /** 兜底：延迟检查系统真实通话状态，呼出失败/已结束则关闭界面。 */
    private final Runnable checkCallStateRunnable = new Runnable() {
        @Override
        public void run() {
            if (finish || connected) return;
            try {
                TelephonyManager tm = (TelephonyManager) requireContext().getSystemService(Context.TELEPHONY_SERVICE);
                int cs = tm != null ? tm.getCallState() : TelephonyManager.CALL_STATE_IDLE;
                Log.d(TAG, "checkCallState: sys=" + cs);
                // IDLE=无通话（呼出失败/已挂断）；RINGING/OFFHOOK=通话进行中，保持界面
                if (cs == TelephonyManager.CALL_STATE_IDLE) {
                    onCallEnd();
                }
            } catch (Throwable t) {
                Log.e(TAG, "checkCallState failed", t);
            }
        }
    };

    /** 注册通话状态广播。 */
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
                    onCallEnd();
                }
            }
        };
        IntentFilter filter = new IntentFilter(InCallService.ACTION_CALL_STATE);
        try {
            requireContext().registerReceiver(stateReceiver, filter);
            receiverRegistered = true;
        } catch (Throwable t) {
            Log.e(TAG, "register state receiver failed", t);
        }
    }

    /** 接通：切到通话中。 */
    private void onCallActive() {
        if (finish || connected) return;
        connected = true;
        statusView.setText(R.string.fake_incall_status); // 通话中
        callStartElapsedRealtime = SystemClock.elapsedRealtime();
        durationView.setText("00:00");
        durationView.setVisibility(View.VISIBLE);
        startTimerIfNeeded();
        updateFooter();
    }

    /** 对方挂断/未接通结束：确保通话终止并关闭界面。 */
    private void onCallEnd() {
        if (finish) return;
        finish = true;
        handler.removeCallbacks(tickRunnable);
        // 兜底终止通话（防系统进入"呼叫转接"流程）
        boolean ended = CallHelper.endCallViaService() || CallHelper.endCall(requireContext());
        Log.d(TAG, "call ended remotely, cleanup ended=" + ended);
        try {
            ((SubActivity) requireActivity()).onBackPressed();
        } catch (Throwable t) {
            Log.e(TAG, "close failed", t);
        }
    }

    private void startTimerIfNeeded() {
        if (timerStarted || finish) return;
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

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_SOFT_LEFT:
                if (connected) {
                    toggleSpeaker();
                }
                return true;
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ENDCALL:
                if (finish) return false;
                Log.d(TAG, "onKeyUp: end call");
                boolean ended = CallHelper.endCallViaService() || CallHelper.endCall(requireContext());
                if (ended) {
                    finish = true;
                    handler.removeCallbacks(tickRunnable);
                    ((SubActivity) requireActivity()).onBackPressed();
                } else {
                    UIHelper.showCustomDialog(requireContext(), R.string.fake_incall_end_fail, null);
                }
                return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateFooter();
        if (connected) startTimerIfNeeded();
    }

    /** 底部按钮：拨号中=右挂断；通话中=左免提/右挂断。 */
    private void updateFooter() {
        if (connected) {
            setFooterBar(new String[]{BaseFragment.LEFT_BTN,
                    String.valueOf(speakerOn ? R.string.speaker_off : R.string.speaker_on)}, R_END_CALL);
        } else {
            setFooterBar(new String[]{BaseFragment.LEFT_BTN, ""}, R_END_CALL);
        }
    }

    /** 免提 / 手机切换（三层音频路由）。 */
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
                    AudioManager am = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);
                    if (am != null && android.os.Build.VERSION.SDK_INT >= 31) {
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
                    AudioManager am = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);
                    if (am != null) {
                        am.setMode(AudioManager.MODE_IN_COMMUNICATION);
                        am.setSpeakerphoneOn(speakerOn);
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "setSpeakerphoneOn failed", t);
                }
            }
            updateFooter();
        } catch (Exception e) {
            Log.e(TAG, "toggleSpeaker failed", e);
        }
    }

    @Override
    public void onDestroyView() {
        finish = true;
        handler.removeCallbacks(tickRunnable);
        if (receiverRegistered) {
            try {
                requireContext().unregisterReceiver(stateReceiver);
            } catch (Exception ignored) { }
            receiverRegistered = false;
        }
        super.onDestroyView();
    }
}
