package com.wtbruh.fakelauncher.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import com.wtbruh.fakelauncher.R;

public class StrokeTextView extends androidx.appcompat.widget.AppCompatTextView {
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
        TypedArray typedArray = context.obtainStyledAttributes(attrs, STROKE_ATTRS, defStyleAttr, 0);
        strokeColor = typedArray.getColor(0, 0);
        strokeWidth = typedArray.getInt(1, 3);

        //todo: Read text stroke configuration from SharedPreferences
        //SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        //strokeWidth = pref.getInt(SubSettingsFragment.PREF_TEXT_STROKE_WIDTH, 3);

        typedArray.recycle();
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
        super.onDraw(canvas);
    }


}
