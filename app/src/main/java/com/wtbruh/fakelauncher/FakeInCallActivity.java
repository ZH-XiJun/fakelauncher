package com.wtbruh.fakelauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.wtbruh.fakelauncher.ui.BaseAppCompatActivity;
import com.wtbruh.fakelauncher.utils.ContentProvider;

/**
 * Locked fake in-call screen. Real telephony may still handle the call in the
 * background; this Activity stays on top and cannot be dismissed by normal keys.
 */
public class FakeInCallActivity extends BaseAppCompatActivity {
    public static final String EXTRA_NUMBER = "extra_number";
    public static final String ACTION_END = "com.wtbruh.fakelauncher.action.FAKE_INCALL_END";
    private static final String TAG = FakeInCallActivity.class.getSimpleName();

    private TextView numberView;
    private TextView durationView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long callStartElapsedRealtime;
    private boolean timerStarted;
    private boolean receiverRegistered;
    private boolean finishingForIdle;
    private boolean seenActiveCall;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if (finishingForIdle || isFinishing()) return;
            updateDuration();
            handler.postDelayed(this, 1000L);
        }
    };

    private final Runnable bringFrontRunnable = this::bringToFrontIfNeeded;

    private final BroadcastReceiver endReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_END.equals(intent.getAction())) {
                finishCallUi("broadcast_end");
            }
        }
    };

    private final BroadcastReceiver phoneStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) return;
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)
                    || TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                seenActiveCall = true;
                bringToFrontIfNeeded();
            } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                // End as soon as the call returns to IDLE while our session is active.
                if (ApplicationHelper.dialing || seenActiveCall) {
                    finishCallUi("phone_idle");
                }
            }
        }
    };

    public static void start(Context context, String number) {
        ApplicationHelper.dialing = true;
        ApplicationHelper.fakeCallNumber = number == null ? "" : number;
        Intent intent = new Intent(context, FakeInCallActivity.class)
                .putExtra(EXTRA_NUMBER, ApplicationHelper.fakeCallNumber)
                // Stay in FakeLauncher task when started from an Activity so lock-task
                // never needs to be released for system dialer / other apps.
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        if (!(context instanceof android.app.Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    public static void requestEnd(Context context) {
        if (context == null) return;
        context.sendBroadcast(new Intent(ACTION_END).setPackage(context.getPackageName()));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Match FakeLauncher desktop window treatment: edge-to-edge black bars.
        EdgeToEdge.enable(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        applySystemBarAppearance();

        setContentView(R.layout.activity_fake_incall);

        View root = findViewById(R.id.fake_incall_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        numberView = findViewById(R.id.fake_incall_number);
        durationView = findViewById(R.id.fake_incall_duration);
        callStartElapsedRealtime = SystemClock.elapsedRealtime();
        applyNumber(getIntent());
        updateDuration();
        startTimerIfNeeded();

        IntentFilter endFilter = new IntentFilter(ACTION_END);
        IntentFilter phoneFilter = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        registerReceiver(endReceiver, endFilter);
        registerReceiver(phoneStateReceiver, phoneFilter);
        receiverRegistered = true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyNumber(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (finishingForIdle) return;
        ApplicationHelper.dialing = true;
        numberView.setSelected(true);
        startTimerIfNeeded();
        updateDuration();
        applySystemBarAppearance();
        // Re-assert pin on the current FakeLauncher task (same task as Main/Sub).
        // Never leave pin unlocked during an active call session.
        try {
            ContentProvider.setTaskId(this, getTaskId());
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && !finishingForIdle) {
            applySystemBarAppearance();
        }
    }

    @Override
    protected void onPause() {
        if (!finishingForIdle && ApplicationHelper.dialing) {
            handler.removeCallbacks(bringFrontRunnable);
            handler.postDelayed(bringFrontRunnable, 250L);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        finishingForIdle = true;
        handler.removeCallbacksAndMessages(null);
        if (receiverRegistered) {
            try {
                unregisterReceiver(endReceiver);
            } catch (Exception ignored) {
            }
            try {
                unregisterReceiver(phoneStateReceiver);
            } catch (Exception ignored) {
            }
            receiverRegistered = false;
        }
        super.onDestroy();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public void onBackPressed() {
        // Block exit while the fake call UI is active.
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return true;
    }

    private void applySystemBarAppearance() {
        // Align with SubActivity/MainActivity black status treatment.
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(false);
            controller.setAppearanceLightNavigationBars(false);
            // Hide system bars like a locked feature-phone UI while call is active.
            controller.hide(WindowInsetsCompat.Type.statusBars()
                    | WindowInsetsCompat.Type.navigationBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    private void applyNumber(Intent intent) {
        String number = intent != null ? intent.getStringExtra(EXTRA_NUMBER) : null;
        if (TextUtils.isEmpty(number)) number = ApplicationHelper.fakeCallNumber;
        if (TextUtils.isEmpty(number)) number = getString(R.string.fake_incall_unknown);
        numberView.setText(number);
        numberView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        numberView.setSingleLine(true);
        numberView.setSelected(true);
        if (!getString(R.string.fake_incall_unknown).equals(number)) {
            ApplicationHelper.fakeCallNumber = number;
        }
    }

    private void startTimerIfNeeded() {
        if (timerStarted || finishingForIdle) return;
        if (callStartElapsedRealtime <= 0L) {
            callStartElapsedRealtime = SystemClock.elapsedRealtime();
        }
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

    private void bringToFrontIfNeeded() {
        if (finishingForIdle || isFinishing() || !ApplicationHelper.dialing) return;
        try {
            // Same-task reorder only; avoid NEW_TASK so pin stays on FakeLauncher.
            Intent intent = new Intent(this, FakeInCallActivity.class)
                    .putExtra(EXTRA_NUMBER, ApplicationHelper.fakeCallNumber)
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
        } catch (Exception e) {
            Log.w(TAG, "bringToFront failed", e);
        }
    }

    private void finishCallUi(String reason) {
        if (finishingForIdle || isFinishing()) return;
        finishingForIdle = true;
        timerStarted = false;
        Log.d(TAG, "finishCallUi reason=" + reason);

        handler.removeCallbacksAndMessages(null);

        ApplicationHelper.dialing = false;
        ApplicationHelper.fakeCallNumber = "";
        seenActiveCall = false;

        // Do not unlock pin here; MainActivity will re-pin to its own task on resume.
        // Unlocking would briefly allow system dialer / launcher to surface.

        Intent home = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        try {
            startActivity(home);
            overridePendingTransition(0, 0);
        } catch (Exception e) {
            Log.w(TAG, "return MainActivity failed", e);
        }
        finish();
        overridePendingTransition(0, 0);
    }
}