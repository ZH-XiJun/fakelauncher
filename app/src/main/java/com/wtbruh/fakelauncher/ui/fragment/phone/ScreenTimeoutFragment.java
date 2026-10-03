package com.wtbruh.fakelauncher.ui.fragment.phone;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.constants.SettingsConstants;
import com.wtbruh.fakelauncher.ui.fragment.BaseFragment;
import com.wtbruh.fakelauncher.ui.view.SingleTextviewAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * 熄屏设置：30秒 / 1分钟 / 3分钟 / 5分钟 / 10分钟 / 常亮。
 * 保存到偏好后由 MainActivity 在伪装内应用为系统屏幕超时。
 */
public class ScreenTimeoutFragment extends BaseFragment {

    // 各档位对应的秒数（常亮 = 24 小时）
    private static final int[] OPT_SECS = {30, 60, 180, 300, 600, 86400};

    private RecyclerView menuView;
    private SingleTextviewAdapter adapter;
    private final List<String> itemNames = new ArrayList<>();

    public ScreenTimeoutFragment() { }

    public static ScreenTimeoutFragment newInstance() {
        return new ScreenTimeoutFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_screen_timeout, container, false);
        init();
        return rootView;
    }

    private void init() {
        itemNames.clear();
        itemNames.add(getString(R.string.screen_timeout_30s));
        itemNames.add(getString(R.string.screen_timeout_1m));
        itemNames.add(getString(R.string.screen_timeout_3m));
        itemNames.add(getString(R.string.screen_timeout_5m));
        itemNames.add(getString(R.string.screen_timeout_10m));
        itemNames.add(getString(R.string.screen_timeout_always));

        menuView = rootView.findViewById(R.id.screen_timeout_list);
        menuView.setLayoutManager(new LinearLayoutManager(requireContext()));
        menuView.setFocusable(false);
        menuView.setItemAnimator(null);
        // 功能机风格：禁触
        menuView.setOnTouchListener((v, e) -> true);
        adapter = new SingleTextviewAdapter(itemNames);
        menuView.setAdapter(adapter);

        // 高亮当前档位
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(requireContext());
        int cur = sp.getInt(SettingsConstants.PREF_FAKEUI_SCREEN_TIMEOUT, 60);
        int idx = 1;
        for (int i = 0; i < OPT_SECS.length; i++) {
            if (OPT_SECS[i] == cur) { idx = i; break; }
        }
        menuView.scrollToPosition(idx);
        adapter.setSelectedPosition(idx);
        setFooterBar(L_SAVE, R_DEFAULT);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (adapter == null || adapter.getItemCount() == 0) {
            return keyCode == KeyEvent.KEYCODE_BACK;
        }
        int position = adapter.getSelectedPosition();
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                if (position > 0) {
                    menuView.scrollToPosition(position - 1);
                    adapter.setSelectedPosition(position - 1);
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (position < adapter.getItemCount() - 1) {
                    menuView.scrollToPosition(position + 1);
                    adapter.setSelectedPosition(position + 1);
                }
                return true;
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (position >= 0 && position < OPT_SECS.length) {
                    int sec = OPT_SECS[position];
                    PreferenceManager.getDefaultSharedPreferences(requireContext())
                            .edit().putInt(SettingsConstants.PREF_FAKEUI_SCREEN_TIMEOUT, sec).apply();
                    // 立即应用到系统屏幕超时
                    try {
                        if (android.provider.Settings.System.canWrite(requireContext())) {
                            android.provider.Settings.System.putInt(requireContext().getContentResolver(),
                                    android.provider.Settings.System.SCREEN_OFF_TIMEOUT, sec * 1000);
                        }
                    } catch (Exception ignore) { }
                    requireActivity().onBackPressed();
                }
                return true;
            case KeyEvent.KEYCODE_BACK:
                return false;
            default:
                return false;
        }
    }
}
