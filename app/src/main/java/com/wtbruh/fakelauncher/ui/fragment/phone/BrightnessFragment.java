package com.wtbruh.fakelauncher.ui.fragment.phone;

import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.ui.fragment.BaseFragment;

/**
 * 亮度调节（音量界面风格）：0~100 无百分号。
 * 上下/左右键调进度条并实时预览，数字键直接输入新值实时预览，
 * 确定（右菜单键/确定键）保存并返回，左菜单键删除末位数字。
 */
public class BrightnessFragment extends BaseFragment {

    private ProgressBar bar;
    private TextView levelText;
    private int brightness = 50;                 // 当前亮度 0~100
    private final StringBuilder input = new StringBuilder(); // 编辑输入串（0~100）

    public BrightnessFragment() { }

    public static BrightnessFragment newInstance() {
        return new BrightnessFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_brightness, container, false);
        init();
        return rootView;
    }

    private void init() {
        bar = rootView.findViewById(R.id.brightness_bar);
        levelText = rootView.findViewById(R.id.brightness_level);
        bar.setMax(100);
        // 关闭自动亮度并读取当前亮度（0~255 → 0~100）
        try {
            Settings.System.putInt(requireContext().getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            int sys = Settings.System.getInt(requireContext().getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, 128);
            brightness = Math.round(sys * 100f / 255f);
        } catch (Exception e) {
            brightness = 50;
        }
        refresh();
        // 底部按钮：左=删除，右=保存
        setFooterBar(new String[]{BaseFragment.LEFT_BTN, String.valueOf(R.string.edittext_rightButton)},
                new String[]{BaseFragment.RIGHT_BTN, String.valueOf(R.string.save_leftButton)});
        // Android 13 返回预测可能不走 onKeyUp：注册返回回调，保证右菜单键必定保存并返回
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        saveAndBack();
                    }
                });
    }

    /** 保存当前亮度值并返回上级界面（右菜单键/确定键统一入口） */
    private void saveAndBack() {
        if (input.length() > 0) {
            brightness = Math.min(100, Integer.parseInt(input.toString()));
            input.setLength(0);
        }
        apply(brightness);
        // 返回上一 Fragment（设置菜单），而不是关闭整个 Activity
        try {
            requireActivity().onBackPressed();
        } catch (Exception e) {
            requireActivity().finish();
        }
    }

    /** 刷新进度条与数字显示：编辑中显示输入串，否则显示当前值（无%） */
    private void refresh() {
        int shown;
        if (input.length() > 0) {
            shown = Math.min(100, Integer.parseInt(input.toString()));
        } else {
            shown = brightness;
        }
        if (bar != null) bar.setProgress(shown);
        if (levelText != null) levelText.setText(String.valueOf(shown));
    }

    /** 应用 0~100 值到系统亮度（映射 0~255），实时生效 */
    private void apply(int value) {
        int sys = Math.max(0, Math.min(255, Math.round(value * 255f / 100f)));
        try {
            Settings.System.putInt(requireContext().getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, sys);
        } catch (Exception e) {
            // 无 WRITE_SETTINGS 权限时静默失败
        }
    }

    /** 编辑中解析输入值并实时预览；无输入则用当前值 */
    private void applyEditing() {
        if (input.length() > 0) {
            apply(Math.min(100, Integer.parseInt(input.toString())));
        } else {
            apply(brightness);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // 数字键：编辑输入新值并实时预览
        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            int digit = keyCode - KeyEvent.KEYCODE_0;
            if (input.length() < 3) {
                input.append(digit);
                if (Integer.parseInt(input.toString()) > 100) {
                    input.deleteCharAt(input.length() - 1);
                }
            }
            applyEditing();
            refresh();
            return true;
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                input.setLength(0);
                brightness = Math.min(100, brightness + 5);
                apply(brightness);
                refresh();
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
                input.setLength(0);
                brightness = Math.max(0, brightness - 5);
                apply(brightness);
                refresh();
                return true;
            case KeyEvent.KEYCODE_MENU:
                // 左菜单键：删除末位数字，并实时预览
                if (input.length() > 0) {
                    input.deleteCharAt(input.length() - 1);
                    applyEditing();
                    refresh();
                }
                return true;
            case KeyEvent.KEYCODE_BACK:
                // 右菜单键：确定保存并返回上级界面
                saveAndBack();
                return true;
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                // 方向键中间确定：保存并返回
                saveAndBack();
                return true;
            default:
                return false;
        }
    }
}
