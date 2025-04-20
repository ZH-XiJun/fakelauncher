package com.wtbruh.fakelauncher.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.wtbruh.fakelauncher.R;

public class TelephonyHelper {
    private Context context;
    private final TelephonyManager mTelephonyManager;
    private final SubscriptionManager mSubscriptionManager;
    private final static String TAG = TelephonyHelper.class.getSimpleName();
    private final static String OPERATOR_CHINA_MOBILE = "46000";
    private final static String OPERATOR_CHINA_MOBILE_2 = "46002";
    private final static String OPERATOR_CHINA_UNICOM = "46001";
    private final static String OPERATOR_CHINA_TELECOM = "46003";

    public TelephonyHelper(Context context) {
        this.context = context;
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mSubscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
    }

    public String getProvidersName() {
        if (! PrivilegeProvider.CheckPermission(context,Manifest.permission.READ_PHONE_STATE)) {
            Log.d(TAG, "No permission READ_PHONE_STATE!!!");
            return "";
        }
        SubscriptionInfo sub0 = mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(0);
        if (sub0 != null) Log.d(TAG, "Card 1!");
        return "";
    }
}
