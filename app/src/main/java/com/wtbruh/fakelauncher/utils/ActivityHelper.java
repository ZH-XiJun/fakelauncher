package com.wtbruh.fakelauncher.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;

public class ActivityHelper extends Activity {
    public boolean isMyLauncherDefault() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
        filter.addCategory(Intent.CATEGORY_HOME);
        List<IntentFilter> filters = new ArrayList<>();
        filters.add(filter);

        List<ComponentName> preferredActivities = new ArrayList<>();
        PackageManager pm = getPackageManager();
        pm.getPreferredActivities(filters, preferredActivities, null);

        return preferredActivities.contains(new ComponentName(this, this.getClass().getName()));
    }
    
}
