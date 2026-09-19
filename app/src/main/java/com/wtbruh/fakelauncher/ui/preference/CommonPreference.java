package com.wtbruh.fakelauncher.ui.preference;

import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.wtbruh.fakelauncher.ApplicationHelper;
import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;
import com.wtbruh.fakelauncher.utils.UIHelper;

public class CommonPreference extends Preference {

    public int customTitle = -1;

    public long lastTriggerTime = 0;

    public boolean init;

    public View itemView;
    public TextView titleTv;
    public TextView summaryTv;

    public CommonPreference(android.content.Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_item_permission);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        itemView = holder.itemView;

        titleTv = itemView.findViewById(android.R.id.title);
        summaryTv = itemView.findViewById(android.R.id.summary);

        if (!init) {
            init = true;
            init();
        }

        if (UIHelper.debounce(lastTriggerTime, 500)) return;
        lastTriggerTime = System.currentTimeMillis();
        updateState();
    }

    /**
     * Override this method to perform any additional initialization.<br>
     * 重载此方法以添加额外的初始化操作。
     */
    public void init() {}

    public void setCustomTitle(int titleResId) {
        customTitle = titleResId;
        if (itemView != null) {
            TextView customTitleTv = itemView.findViewById(R.id.title);
            customTitleTv.setVisibility(View.VISIBLE);
            customTitleTv.setText(customTitle);
        }
    }

    public void updateState() {
        if (itemView != null) {
            itemView.post(() -> {
                itemView.findViewById(android.R.id.title).setVisibility(View.VISIBLE);
                itemView.findViewById(android.R.id.summary).setVisibility(View.VISIBLE);
            });
        }
    }
}
