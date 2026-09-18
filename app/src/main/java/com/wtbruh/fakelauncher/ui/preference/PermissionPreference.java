package com.wtbruh.fakelauncher.ui.preference;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.wtbruh.fakelauncher.ApplicationHelper;
import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;

public class PermissionPreference extends Preference {
    public PermissionPreference(android.content.Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_item_permission);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View itemView = holder.itemView;
        itemView.post(() -> {
            if (!PrivilegeProvider.checkAllPermissions(getContext())) {
                setTitle(R.string.pref_permissions_danger);
                setSummary(R.string.pref_permissions_danger_summary);
                itemView.setBackgroundResource(R.drawable.bg_error_red);
            } else if (!ApplicationHelper.isXposedModuleActivated()) {
                setTitle(R.string.pref_permissions_warn);
                setSummary(R.string.pref_permissions_warn_summary);
                itemView.setBackgroundResource(R.drawable.bg_warn_yellow);
            } else {
                setTitle(R.string.pref_permissions_ok);
                setSummary(R.string.pref_permissions_ok_summary);
                itemView.setBackgroundResource(R.drawable.bg_ok_green);
            }
        });

    }
}
