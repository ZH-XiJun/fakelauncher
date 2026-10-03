package com.wtbruh.fakelauncher.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.constants.SettingsConstants;
import com.wtbruh.fakelauncher.utils.UIHelper;

/**
 * 音量调节（与亮度调节同风格）：0~100 无百分号，可输入数字控制。
 * 数字键输入（不预览），左菜单键删除末位，右菜单键/确定键保存并返回；
 * 音量键与方向键即时调整（±1 系统步进）。
 */
public class VolumeActivity extends BaseAppCompatActivity {
    private AudioManager mAudioManager;
    private ProgressBar bar;
    private ImageView icon;
    private TextView text;
    private final int AUDIO_STREAM_TYPE = AudioManager.STREAM_MUSIC;
    private int mMaxVol = 15;
    private int percent = 50;                    // 0~100 百分比（显示与控制值）
    private final StringBuilder input = new StringBuilder(); // 编辑输入串
    private int shownOverride = -1;              // 无输入删除时仅改显示（-1=未启用），不影响实际音量

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_volume);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.Volume), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        init();
        // Android 13 返回预测可能不走 onKeyUp：注册返回回调，保证右菜单键必定保存并返回
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                applyInput();
                finish();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // singleTop 复用：音量键再次按下时刷新当前系统音量
        if (mAudioManager != null) {
            shownOverride = -1;
            getVolume();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return switch (keyCode) {
            // 音量键/方向键即时调整；BACK 拦截（由 onKeyUp 保存返回）
            case KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN,
                 KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_RIGHT,
                 KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT -> {
                adjustVolume(keyCode);
                yield true;
            }
            case KeyEvent.KEYCODE_BACK -> {
                applyInput();
                finish();
                yield true;
            }
            default -> super.onKeyDown(keyCode, event);
        };
    }

    /**
     * Init of volume data 初始化音量相关
     */
    private void init() {
        // 自动调整大小
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        float scale = (float) sp.getInt(SettingsConstants.PREF_MAIN_UI_HEIGHT_SCALE, 10) / 10;
        if (scale > 0 && scale != 1.0) {
            View view = findViewById(R.id.volume_ActionBar);
            UIHelper.resizeView(scale, view);
        }
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        bar = findViewById(R.id.volumeBar);
        icon = findViewById(R.id.volume_logo);
        text = findViewById(R.id.volumeLevel);
        icon.setTag("unmute");
        mMaxVol = mAudioManager.getStreamMaxVolume(AUDIO_STREAM_TYPE);
        if (mMaxVol <= 0) mMaxVol = 15;
        bar.setMax(100);
        getVolume();
    }

    /**
     * Adjust volume and show current volume 调整音量（音量键/方向键，即时生效）
     */
    private void adjustVolume(int keyCode) {
        if (mAudioManager == null) return;
        int operation;
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                operation = AudioManager.ADJUST_RAISE;
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
                operation = AudioManager.ADJUST_LOWER;
                break;
            default:
                return;
        }
        mAudioManager.adjustStreamVolume(AUDIO_STREAM_TYPE, operation, AudioManager.FLAG_PLAY_SOUND);
        input.setLength(0);
        shownOverride = -1;
        getVolume();
    }

    private void getVolume() {
        // Current volume 当前音量 → 0~100
        int volume = mAudioManager.getStreamVolume(AUDIO_STREAM_TYPE);
        percent = Math.round(volume * 100f / mMaxVol);
        refresh();
    }

    /** 刷新进度条与数字：编辑中显示输入串（0~100 无%），否则显示当前值 */
    private void refresh() {
        int shown;
        if (input.length() > 0) {
            shown = Math.min(100, Integer.parseInt(input.toString()));
        } else if (shownOverride >= 0) {
            shown = shownOverride;
        } else {
            shown = percent;
        }
        bar.setProgress(shown);
        text.setText(String.valueOf(shown));
        if (shown == 0 && "unmute".equals(icon.getTag())) {
            icon.setImageResource(R.drawable.ic_volume_speaker_off);
            icon.setTag("mute");
        } else if (shown != 0 && "mute".equals(icon.getTag())) {
            icon.setImageResource(R.drawable.ic_volume_speaker_on);
            icon.setTag("unmute");
        }
    }

    /** 应用输入值（0~100）到媒体音量（保存时才调用，不预览） */
    private void applyInput() {
        if (input.length() > 0) {
            percent = Math.min(100, Integer.parseInt(input.toString()));
            input.setLength(0);
        }
        shownOverride = -1;
        int sysVol = Math.round(percent * mMaxVol / 100f);
        mAudioManager.setStreamVolume(AUDIO_STREAM_TYPE, sysVol, 0);
        getVolume();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // 数字键：编辑输入新值（不实时预览）
        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            int digit = keyCode - KeyEvent.KEYCODE_0;
            if (input.length() < 3) {
                input.append(digit);
                if (Integer.parseInt(input.toString()) > 100) {
                    input.deleteCharAt(input.length() - 1);
                }
            }
            refresh();
            return true;
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                // 左菜单键：删除末位数字——有输入删输入；无输入仅改显示值，不影响实际音量
                if (input.length() > 0) {
                    input.deleteCharAt(input.length() - 1);
                } else {
                    shownOverride = percent / 10;
                }
                refresh();
                return true;
            case KeyEvent.KEYCODE_BACK:
                // 右菜单键：保存并返回
                applyInput();
                finish();
                return true;
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                // 方向键中间确定：保存并返回
                applyInput();
                finish();
                return true;
            default:
                return false;
        }
    }
}
