package com.wtbruh.fakelauncher.ui.fragment.phone;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.wtbruh.fakelauncher.ApplicationHelper;
import com.wtbruh.fakelauncher.FakeInCallActivity;
import com.wtbruh.fakelauncher.MainActivity;
import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.SubActivity;
import com.wtbruh.fakelauncher.ui.fragment.BaseFragment;
import com.wtbruh.fakelauncher.utils.ContentProvider;
import com.wtbruh.fakelauncher.utils.UIHelper;

public class FakeInCallFragment extends BaseFragment {
    public static final String ARG_NUMBER = "number";
    public static final String ACTION_END = "com.wtbruh.fakelauncher.action.FAKE_INCALL_END";
    private static final String TAG = FakeInCallActivity.class.getSimpleName();

    private TextView numberView;
    private TextView durationView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long callStartElapsedRealtime;
    private boolean timerStarted;
    private boolean receiverRegistered;
    private boolean finish = false;
    private boolean doBack = false;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if (finish) return;
            updateDuration();
            handler.postDelayed(this, 1000L);
        }
    };

    public static FakeInCallFragment newInstance (Bundle args) {
        FakeInCallFragment fragment = new FakeInCallFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                         Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_fake_incall, container, false);
        init();
        return rootView;
    }
    private void init() {
        /*
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        applySystemBarAppearance();
*/
        numberView = findViewById(R.id.fake_incall_number);
        durationView = findViewById(R.id.fake_incall_duration);
        callStartElapsedRealtime = SystemClock.elapsedRealtime();
        String fakeCallNumber = "";
        if (getArguments() != null) fakeCallNumber = getArguments().getString(ARG_NUMBER);
        applyNumber(fakeCallNumber);
        updateDuration();
        startTimerIfNeeded();

        //IntentFilter endFilter = new IntentFilter(ACTION_END);
        //IntentFilter phoneFilter = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        //registerReceiver(endReceiver, endFilter);
        //registerReceiver(phoneStateReceiver, phoneFilter);
        //receiverRegistered = true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ENDCALL:
                if (doBack) return false;
                Log.d(TAG, "onKeyUp: end call");
                Dialog d = UIHelper.showCustomDialog(requireContext(), R.string.fake_incall_end_call, (dialogInterface, i, keyEvent) -> {
                    if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                        dialogInterface.dismiss();
                    }
                    return true;
                });
                d.setOnDismissListener(dialogInterface -> ((SubActivity) requireActivity()).onBackPressed());
                return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        setFooterBar(L_OPTION, R_END_CALL);
        startTimerIfNeeded();
    }

    private void applyNumber(String number) {
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
        if (timerStarted || finish) return;
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

}
