package com.wtbruh.fakelauncher.ui.widget;

import android.content.Context;
import android.text.TextPaint;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.TextViewCompat;

public class FitTextView extends androidx.appcompat.widget.AppCompatTextView {

    public FitTextView(@NonNull Context context) {
        super(context);
    }

    public FitTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FitTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
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
