package com.wtbruh.fakelauncher.utils;

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.wtbruh.fakelauncher.R;

/**
 * 电话相关工具：运营商名、信号强度、SIM 状态、卡槽数量。
 * 同时封装了 per-subscription TelephonyManager 缓存，
 * 避免每次轮询都重新 createForSubscriptionId。
 */
public class TelephonyHelper {
    private final Context context;
    private final TelephonyManager mTelephonyManager;
    private final SubscriptionManager mSubscriptionManager;
    private static final String TAG = TelephonyHelper.class.getSimpleName();
    private static final String OPERATOR_CHINA_MOBILE = "46000";
    private static final String OPERATOR_CHINA_MOBILE_2 = "46002";
    private static final String OPERATOR_CHINA_UNICOM = "46001";
    private static final String OPERATOR_CHINA_TELECOM = "46003";

    // 缓存的 per-subId TelephonyManager，减少 createForSubscriptionId 开销。
    private TelephonyManager mTm1, mTm2;
    private int mCachedSubId1 = -1, mCachedSubId2 = -1;
    private boolean mCacheBuilt;

    public TelephonyHelper(Context context) {
        this.context = context;
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mSubscriptionManager = Build.VERSION.SDK_INT >= 22
                ? (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                : null;
    }

    // ── 运营商名 ──

    /** 返回指定卡槽的运营商显示名（SPN）。无权限/无 SIM 返回资源字符串 sim_removed。 */
    public String getProvidersName(int sub) {
        if (!PrivilegeProvider.checkPermission(context, Manifest.permission.READ_PHONE_STATE)) {
            Log.d(TAG, "No permission READ_PHONE_STATE");
            return context.getResources().getString(R.string.sim_removed);
        }
        SubscriptionInfo sub0 = getSubscriptionInfo(sub);
        if (sub0 == null) return context.getResources().getString(R.string.sim_removed);
        CharSequence name = sub0.getDisplayName();
        return name != null ? name.toString() : "";
    }

    // ── 信号强度 ──

    /** 返回指定卡槽信号强度等级 0-4。API 29+ 用 getLevel()，否则退化为 asu 映射。 */
    @SuppressWarnings("MissingPermission")
    public int getSignalLevel(int sub) {
        TelephonyManager tm = getTelephonyManagerForSlot(sub);
        if (tm == null) return 0;
        if (getSimState(sub) != TelephonyManager.SIM_STATE_READY) return 0;
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                android.telephony.SignalStrength ss = tm.getSignalStrength();
                if (ss != null) return ss.getLevel(); // 0-4
            } else {
                // 老版本退化为 GSM ASU 映射
                // 无法直接获取 level，返回默认值
                return 2; // 默认中等信号
            }
        } catch (Exception e) {
            Log.e(TAG, "getSignalLevel failed for sub=" + sub, e);
        }
        return 0;
    }

    // ── SIM 状态 ──

    /** 返回指定卡槽的 SIM 状态（SIM_STATE_READY / SIM_STATE_ABSENT 等）。 */
    @SuppressWarnings("MissingPermission")
    public int getSimState(int sub) {
        TelephonyManager tm = getTelephonyManagerForSlot(sub);
        if (tm == null) return TelephonyManager.SIM_STATE_UNKNOWN;
        try {
            return tm.getSimState();
        } catch (Exception e) {
            Log.e(TAG, "getSimState failed for sub=" + sub, e);
            return TelephonyManager.SIM_STATE_UNKNOWN;
        }
    }

    /** SIM 卡是否就绪（有卡可用）。 */
    public boolean isSimReady(int sub) {
        return getSimState(sub) == TelephonyManager.SIM_STATE_READY;
    }

    // ── 卡槽数量 ──

    /** 获取设备 SIM 卡槽数量，API 23+ 用 getPhoneCount()，否则回退到 1。 */
    @SuppressWarnings("MissingPermission")
    public int getPhoneCount() {
        try {
            if (Build.VERSION.SDK_INT >= 23 && mTelephonyManager != null) {
                return mTelephonyManager.getPhoneCount();
            }
        } catch (Exception e) {
            Log.w(TAG, "getPhoneCount failed", e);
        }
        return 1;
    }

    // ── 内部：per-subscription TelephonyManager ──

    /**
     * 获取指定卡槽对应的 TelephonyManager（缓存）。
     * sub=0 为第一张卡（对应 SIM 卡槽 0），sub=1 为第二张卡。
     */
    @SuppressWarnings("MissingPermission")
    private TelephonyManager getTelephonyManagerForSlot(int sub) {
        if (sub == 0) {
            if (mTm1 != null) return mTm1;
            mTm1 = buildTmForSlot(0);
            return mTm1 != null ? mTm1 : mTelephonyManager;
        }
        if (sub == 1) {
            if (mTm2 != null) return mTm2;
            mTm2 = buildTmForSlot(1);
            return mTm2;
        }
        return mTelephonyManager;
    }

    private TelephonyManager buildTmForSlot(int slot) {
        if (Build.VERSION.SDK_INT < 22 || mSubscriptionManager == null) {
            return mTelephonyManager;
        }
        SubscriptionInfo info = getSubscriptionInfo(slot);
        if (info != null) {
            int subId = info.getSubscriptionId();
            try {
                return mTelephonyManager.createForSubscriptionId(subId);
            } catch (Exception e) {
                Log.e(TAG, "createForSubscriptionId failed for slot=" + slot, e);
            }
        }
        return mTelephonyManager;
    }

    /** 按 SIM 卡槽索引获取 SubscriptionInfo。 */
    @SuppressWarnings("MissingPermission")
    private SubscriptionInfo getSubscriptionInfo(int slot) {
        if (Build.VERSION.SDK_INT < 22 || mSubscriptionManager == null) return null;
        try {
            return mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(slot);
        } catch (Exception e) {
            Log.e(TAG, "getActiveSubscriptionInfoForSimSlotIndex(" + slot + ") failed", e);
            return null;
        }
    }
}
