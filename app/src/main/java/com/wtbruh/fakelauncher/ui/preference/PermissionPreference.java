package com.wtbruh.fakelauncher.ui.preference;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.TextView;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;

public class PermissionPreference extends CommonPreference {

    private boolean clicked = false;

    public int redStateTitle = R.string.pref_permissions_danger;
    public int redStateSummary = R.string.pref_grant_all_permissions;
    public int greenStateTitle = R.string.pref_permissions_granted;
    public int greenStateSummary = R.string.pref_permissions_ok_summary;

    public PermissionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void init() {
        setCustomTitle(R.string.pref_category_permissions);
    }

    @Override
    public void updateState() {
        if (clicked) return;
        if (itemView != null) {
            itemView.post(() -> {
                if (PrivilegeProvider.checkAllPermissions(getContext())) {
                    setTitle(greenStateTitle);
                    setSummary(greenStateSummary);
                    itemView.setBackgroundResource(R.drawable.bg_ok_green);
                } else {
                    setTitle(redStateTitle);
                    setSummary(redStateSummary);
                    itemView.setBackgroundResource(R.drawable.bg_error_red);
                }
            });
            super.updateState();
        }
    }

    @Override
    public void onClick() {
        clicked = true;
        TextView summaryTv = itemView.findViewById(android.R.id.summary);
        String previousText = summaryTv.getText().toString();
        summaryTv.setText(R.string.pref_operation_completed);
        clicked = false;
        new Handler().postDelayed(() -> {
            if (summaryTv.getText().toString().equals(getContext().getString(R.string.pref_operation_completed))) summaryTv.setText(previousText);
            }, 2000);
    }
}
