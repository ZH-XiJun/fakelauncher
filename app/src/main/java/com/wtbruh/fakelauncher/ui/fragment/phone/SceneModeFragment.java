package com.wtbruh.fakelauncher.ui.fragment.phone;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
 * 情景模式：响铃并振动 / 仅振动 / 静音。
 * 针对接听电话时是否响铃与振动（由 IncomingCallActivity 读取生效）。
 */
public class SceneModeFragment extends BaseFragment {

    private static final String[] MODES = {"ring_vibrate", "vibrate_only", "silent"};

    private RecyclerView menuView;
    private SingleTextviewAdapter adapter;
    private final List<String> itemNames = new ArrayList<>();

    public SceneModeFragment() { }

    public static SceneModeFragment newInstance() {
        return new SceneModeFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_scene_mode, container, false);
        init();
        return rootView;
    }

    private void init() {
        itemNames.clear();
        itemNames.add(getString(R.string.scene_ring_vibrate));
        itemNames.add(getString(R.string.scene_vibrate_only));
        itemNames.add(getString(R.string.scene_silent));

        menuView = rootView.findViewById(R.id.scene_mode_list);
        menuView.setLayoutManager(new LinearLayoutManager(requireContext()));
        menuView.setFocusable(false);
        menuView.setItemAnimator(null);
        // 功能机风格：禁触
        menuView.setOnTouchListener((v, e) -> true);
        adapter = new SingleTextviewAdapter(itemNames);
        menuView.setAdapter(adapter);

        // 高亮当前情景模式
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String mode = sp.getString(SettingsConstants.PREF_SCENE_MODE, MODES[0]);
        for (int i = 0; i < MODES.length; i++) {
            if (MODES[i].equals(mode)) {
                menuView.scrollToPosition(i);
                adapter.setSelectedPosition(i);
                break;
            }
        }
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
                if (position >= 0 && position < MODES.length) {
                    PreferenceManager.getDefaultSharedPreferences(requireContext())
                            .edit().putString(SettingsConstants.PREF_SCENE_MODE, MODES[position]).apply();
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
