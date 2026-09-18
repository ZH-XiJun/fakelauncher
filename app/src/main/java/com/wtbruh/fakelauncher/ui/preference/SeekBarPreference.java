package com.wtbruh.fakelauncher.ui.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceViewHolder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class SeekBarPreference extends androidx.preference.SeekBarPreference implements SeekBar.OnSeekBarChangeListener {

    private TextView mSeekBarValueTextView;
    private SeekBar mSeekBar;
    Class<?> superClass;

    public SeekBarPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        superClass = SeekBarPreference.class.getSuperclass();
        try {
            assert superClass != null;
            Field f = superClass.getDeclaredField("mSeekBarValueTextView");
            Field f2 = superClass.getDeclaredField("mSeekBar");
            f.setAccessible(true);
            f2.setAccessible(true);
            mSeekBarValueTextView = (TextView) f.get(this);
            mSeekBar = (SeekBar) f2.get(this);
        } catch (Exception ignore) {
        }
        if (mSeekBar != null) {
            mSeekBar.setOnSeekBarChangeListener(this);
        }
        updateLabelValue(getSeekBarValue());
    }

    private int getSeekBarValue() {
        try {
            Field f = superClass.getDeclaredField("mSeekBarValue");
            f.setAccessible(true);
            return f.getInt(this);
        } catch (Exception e) {
            return -1;
        }
    }

    void updateLabelValue(int value) {
        float finalValue = (float) value / 10;
        if (mSeekBarValueTextView != null) {
            mSeekBarValueTextView.setText(String.valueOf(finalValue));
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        try {
            Method syncValueInternal = superClass.getDeclaredMethod("syncValueInternal", SeekBar.class);
            Field f = superClass.getDeclaredField("mUpdatesContinuously");
            Field f2 = superClass.getDeclaredField("mTrackingTouch");
            syncValueInternal.setAccessible(true);
            f.setAccessible(true);
            f2.setAccessible(true);
            boolean mUpdatesContinuously = f.getBoolean(this);
            boolean mTrackingTouch = f2.getBoolean(this);
            if (fromUser && (mUpdatesContinuously || !mTrackingTouch)) {
                syncValueInternal.invoke(this, seekBar);
            }
            updateLabelValue(progress + getMin());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}
}
