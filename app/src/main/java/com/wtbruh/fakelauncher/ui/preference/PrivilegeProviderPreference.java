package com.wtbruh.fakelauncher.ui.preference;

import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceViewHolder;

import com.wtbruh.fakelauncher.ApplicationHelper;
import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;
import com.wtbruh.fakelauncher.utils.UIHelper;

public class PrivilegeProviderPreference extends ListPreference implements View.OnLongClickListener {

    public int customTitle = -1;

    public int redStateTitle = R.string.pref_check_privilege_denied;
    public int greenStateTitle = R.string.pref_check_privilege_granted;

    private long lastTriggerTime = 0;

    private boolean init;

    public View itemView;

    public PrivilegeProviderPreference(android.content.Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_item_permission);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        itemView = holder.itemView;
        itemView.setOnLongClickListener(this);
        if (!init) {
            init = true;
            init();
        }
        if (UIHelper.debounce(lastTriggerTime, 1000)) return;
        lastTriggerTime = System.currentTimeMillis();
        updateState();
    }

    public void init() {
        setCustomTitle(R.string.pref_privilege_provider);
    }

    public void updateState() {
        if (itemView != null) {
            TextView titleTv = itemView.findViewById(android.R.id.title);
            itemView.post(() -> {
                titleTv.setVisibility(TextView.VISIBLE);
                if (PrivilegeProvider.checkPrivilege(PrivilegeProvider.privilegeToInt(getValue()))) {
                    itemView.setBackgroundResource(R.drawable.bg_ok_green);
                    titleTv.setText(getContext().getString(greenStateTitle, getEntry()));
                } else {
                    itemView.setBackgroundResource(R.drawable.bg_error_red);
                    titleTv.setText(getContext().getString(redStateTitle, getEntry()));
                }
            });
        }
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
    public boolean onLongClick(View view) {
        updateState();
        return true;
    }
}
