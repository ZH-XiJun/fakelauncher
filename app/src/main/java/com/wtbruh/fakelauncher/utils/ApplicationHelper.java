package com.wtbruh.fakelauncher.utils;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;


public class ApplicationHelper extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public boolean isMyLauncherDefault(Context context) {

        PackageManager pm = context.getPackageManager();

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);

        ResolveInfo homeActivities = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (homeActivities == null) return false;
        return context.getPackageName().equals(homeActivities.activityInfo.packageName);
    }

}
