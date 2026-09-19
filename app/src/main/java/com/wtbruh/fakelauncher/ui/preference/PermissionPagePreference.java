package com.wtbruh.fakelauncher.ui.preference;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceViewHolder;

import com.wtbruh.fakelauncher.ApplicationHelper;
import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;
import com.wtbruh.fakelauncher.utils.UIHelper;

public class PermissionPagePreference extends CommonPreference {

    public int customTitle = -1;

    public int redStateTitle = R.string.pref_permissions_danger;
    public int redStateSummary = R.string.pref_permissions_danger_summary;
    public int yellowStateTitle = R.string.pref_xposed_not_activated;
    public int yellowStateSummary = R.string.pref_xposed_warn_summary;
    public int greenStateTitle = R.string.pref_permissions_ok;
    public int greenStateSummary = R.string.pref_permissions_ok_summary;

    private long lastTriggerTime = 0;

    private boolean init;

    public View itemView;

    public PermissionPagePreference(android.content.Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_item_permission);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        itemView = holder.itemView;

        if (!init) {
            init = true;
            init();
        }

        if (UIHelper.debounce(lastTriggerTime, 1000)) return;
        lastTriggerTime = System.currentTimeMillis();
        updateState();
    }

    public void setCustomTitle(int titleResId) {
        customTitle = titleResId;
        if (itemView != null) {
            TextView customTitleTv = itemView.findViewById(R.id.title);
            customTitleTv.setVisibility(View.VISIBLE);
            customTitleTv.setText(customTitle);
        }
    }

    @Override
    public void updateState() {
        if (itemView == null) return;
        itemView.post(() -> {
            if (!PrivilegeProvider.checkAllPermissions(getContext())) {
                titleTv.setText(redStateTitle);
                summaryTv.setText(redStateSummary);
                itemView.setBackgroundResource(R.drawable.bg_error_red);
            } else if (!ApplicationHelper.isXposedModuleActivated()) {
                titleTv.setText(yellowStateTitle);
                summaryTv.setText(yellowStateSummary);
                itemView.setBackgroundResource(R.drawable.bg_warn_yellow);
            } else {
                titleTv.setText(greenStateTitle);
                summaryTv.setText(greenStateSummary);
                itemView.setBackgroundResource(R.drawable.bg_ok_green);
            }
        });
        super.updateState();
    }
}
