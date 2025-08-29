package com.wtbruh.fakelauncher.ui.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.ui.fragment.settings.SubSettingsFragment;

public class StrokeTextView extends FitTextView implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final int[] STROKE_ATTRS = new int[]{R.attr.strokeTextColor, R.attr.strokeTextWidth};
    private @ColorInt int strokeColor = 0;
    private int strokeWidth = 3;

    public StrokeTextView(Context context) {
        this(context, null);
    }

    public StrokeTextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public StrokeTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.registerOnSharedPreferenceChangeListener(this);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, STROKE_ATTRS, defStyleAttr, 0);
        strokeColor = typedArray.getColor(0, 0);
        strokeWidth = sp.getInt(SubSettingsFragment.PREF_TEXT_STROKE_WIDTH, typedArray.getInt(1, 3));

        typedArray.recycle();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, @Nullable String key) {
        if (key != null && key.equals(SubSettingsFragment.PREF_TEXT_STROKE_WIDTH)) {
            setStrokeWidth(sp.getInt(key, strokeWidth));
        }
    }

    public void setStrokeColor(@ColorInt int color) {
        strokeColor = color;
        invalidate();
    }

    public void setStrokeWidth(int width) {
        strokeWidth = width;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        try {
            if (strokeWidth > 0) {
                TextPaint wkPaint = getLayout().getPaint();
                int preColor = wkPaint.getColor();
                Paint.Style prePaintStyle = wkPaint.getStyle();
                // apply stroke paint
                wkPaint.setColor(strokeColor);
                wkPaint.setStrokeWidth(strokeWidth);
                wkPaint.setStyle(Paint.Style.STROKE);
                // draw text outline
                getLayout().draw(canvas);

                // restore paint
                wkPaint.setColor(preColor);
                wkPaint.setStrokeWidth(0);
                wkPaint.setStyle(prePaintStyle);
            }
        } catch (Exception ignore) {}
        super.onDraw(canvas);
    }
}
