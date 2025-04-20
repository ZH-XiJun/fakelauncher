package com.wtbruh.fakelauncher;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.wtbruh.fakelauncher.utils.MyAppCompatActivity;

public class VolumeActivity extends MyAppCompatActivity {
    private AudioManager mAudioManager;
    private ProgressBar bar;
    private ImageView icon;
    private TextView text;
    private final int AUDIO_STREAM_TYPE = AudioManager.STREAM_MUSIC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_volume);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        init();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode){
            // Volume key interruption
            // 音量键拦截
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                adjustVolume(keyCode);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Init of volume data 初始化音量相关
     */
    private void init() {
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        bar = findViewById(R.id.volumeBar);
        icon = findViewById(R.id.volume_logo);
        text = findViewById(R.id.volumeLevel);
        icon.setTag("unmute");
        // max volume defined in system 系统定义的最大音量
        int mMaxVol = mAudioManager.getStreamMaxVolume(AUDIO_STREAM_TYPE);
        bar.setMax(mMaxVol);
        getVolume();
    }

    /**
     * Adjust volume and show current volume
     * 调整音量，并同步显示
     * @param keyCode 键值
     */
    private void adjustVolume(int keyCode) {
        if (mAudioManager == null) return;
        int operation;
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                operation = AudioManager.ADJUST_RAISE;
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                operation = AudioManager.ADJUST_LOWER;
                break;
            default:
                return;
        }
        mAudioManager.adjustStreamVolume(AUDIO_STREAM_TYPE, operation, AudioManager.FLAG_PLAY_SOUND);
        getVolume();
    }

    private void getVolume() {
        // Current volume 当前音量大小
        int volume = mAudioManager.getStreamVolume(AUDIO_STREAM_TYPE);
        // Volume bar 音量条
        bar.setProgress(volume);
        // Volume text view 音量文字显示
        /*
        text.setText(String.valueOf(
                // The volume range is not 0~100 in common, convert it by an expression
                // x: result, y: max vol from system, z: current vol
                // Expression: 100/y=x/z, x=100z/y
                // Android 音量范围普遍不是0~100，通过公式转换成0~100
                // x：最终结果，y：系统最大音量, z：当前音量
                // 公式：100/y=x/z，即x=100z/y
                volume*100/mMaxVol
        ));
         */
        text.setText(String.valueOf(volume)); // Originally display volume 直接按照系统提供的范围显示音量，不转换为0~100形式，避免bug
        if (volume == 0 && "unmute".equals(icon.getTag())) {
            icon.setImageResource(R.drawable.volume_speaker_off);
            icon.setTag("mute");
        } else if (volume != 0 && "mute".equals(icon.getTag())) {
            icon.setImageResource(R.drawable.volume_speaker_on);
            icon.setTag("unmute");
        }
    }
}