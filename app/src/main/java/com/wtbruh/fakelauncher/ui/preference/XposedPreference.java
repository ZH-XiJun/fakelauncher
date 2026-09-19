package com.wtbruh.fakelauncher.ui.preference;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceViewHolder;

import com.wtbruh.fakelauncher.ApplicationHelper;
import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.utils.ContentProvider;

public class XposedPreference extends CommonPreference {

    public int redStateTitle = R.string.not_activated;
    public int redStateSummary = R.string.pref_xposed_warn_summary;
    public int greenStateTitle = R.string.activated;
    
    public XposedPreference(android.content.Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        updateState();
    }

    @Override
    public void init() {
        setCustomTitle(R.string.pref_check_xposed);
        if (itemView != null) itemView.findViewById(R.id.arrow).setVisibility(View.INVISIBLE);
    }

    @Override
    public void updateState() {
        if (itemView != null) {
            itemView.post(() -> {
                if (ApplicationHelper.isXposedModuleActivated()) {
                    titleTv.setText(greenStateTitle);
                    summaryTv.setText(getContext().getString(R.string.pref_xposed_activated_summary, getFrameworkVersion()));
                    itemView.setBackgroundResource(R.drawable.bg_ok_green);
                } else {
                    titleTv.setText(redStateTitle);
                    summaryTv.setText(redStateSummary);
                    itemView.setBackgroundResource(R.drawable.bg_error_red);
                }
            });
            super.updateState();
        }
    }

    /**
     * 读取Xposed Framework API 版本
     */
    private int getFrameworkVersion() {
        return ContentProvider.getXposedApi(getContext());
    }
}
