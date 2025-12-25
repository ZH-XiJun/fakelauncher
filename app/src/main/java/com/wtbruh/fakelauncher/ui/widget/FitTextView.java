package com.wtbruh.fakelauncher.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextPaint;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.TextViewCompat;

import com.wtbruh.fakelauncher.R;

public class FitTextView extends androidx.appcompat.widget.AppCompatTextView {

    private final static int[] FIT_ATTRS = new int[]{R.attr.fitWidth, R.attr.fitHeight};

    public FitTextView(@NonNull Context context) {
        super(context);
    }

    public FitTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FitTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray tr = context.obtainStyledAttributes(attrs, FIT_ATTRS, defStyleAttr, 0);
        if (tr.getBoolean(0, false)) fitWidth();
        if (tr.getBoolean(1, false)) fitHeight();
        tr.recycle();

    }

    public void fitWidth() {
        TextPaint paint = getLayout().getPaint();
        getLayoutParams().width = (int) (paint.measureText((String) getText()) + getPaddingStart() + getPaddingEnd());
        requestLayout();
    }

    public void fitHeight() {
        TextViewCompat.setAutoSizeTextTypeWithDefaults(this, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
        getLayoutParams().height = (int) (getTextSize() + getPaddingTop() + getPaddingBottom() + 10);
        requestLayout();
    }
}
