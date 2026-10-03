package com.wtbruh.fakelauncher.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.lang.reflect.Method;

/**
 * Place a real system call. The fake in-call UI is now handled by
 * {@link com.wtbruh.fakelauncher.ui.fragment.phone.FakeInCallFragment} via SubActivity.
 */
public final class CallHelper {
    private static final String TAG = CallHelper.class.getSimpleName();

    private CallHelper() {}

    public static boolean placeCall(Context context, String rawNumber) {
        if (rawNumber == null) return false;
        // 只保留数字和 +；* 与 #（MMI 码/USSD）在功能机伪装中不发起呼叫，避免系统 MMI 弹窗
        String dialNumber = rawNumber.replaceAll("[^0-9+]", "");
        if (dialNumber.isEmpty()) return false;

        Uri uri = Uri.fromParts("tel", dialNumber, null);

        try {
            TelecomManager telecomManager =
                    (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
            if (telecomManager != null) {
                Bundle extras = new Bundle();
                telecomManager.placeCall(uri, extras);
                Log.d(TAG, "placeCall via TelecomManager: " + dialNumber);
                return true;
            }
        } catch (SecurityException se) {
            Log.e(TAG, "TelecomManager.placeCall denied, fallback to ACTION_CALL", se);
        } catch (Exception e) {
            Log.e(TAG, "TelecomManager.placeCall failed, fallback to ACTION_CALL", e);
        }

        try {
            Intent call = new Intent(Intent.ACTION_CALL, uri);
            call.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(call);
            Log.d(TAG, "placeCall via ACTION_CALL: " + dialNumber);
            return true;
        } catch (SecurityException se) {
            Log.e(TAG, "CALL_PHONE denied, falling back to ACTION_DIAL", se);
            return fallbackDial(context, uri);
        } catch (Exception e) {
            Log.e(TAG, "ACTION_CALL failed, falling back to ACTION_DIAL", e);
            return fallbackDial(context, uri);
        }
    }

    private static boolean fallbackDial(Context context, Uri uri) {
        try {
            Intent dial = new Intent(Intent.ACTION_DIAL, uri);
            dial.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(dial);
            return true;
        } catch (Exception ex) {
            Log.e(TAG, "ACTION_DIAL failed", ex);
            return false;
        }
    }

    /**
     * Hang up the current call (real hang-up, cross-version reflection).
     * Try TelephonyManager.getITelephony().endCall() first, then fall back to
     * TelephonyManager.endCall() (hidden API, available on some devices).
     *
     * @param context Context
     * @return true if hung up (or no active call), false if cannot hang up automatically
     */
    /** 通过 InCallService 当前通话的 Call.disconnect() 挂断（对任意状态可靠）。 */
    public static boolean endCallViaService() {
        try {
            android.telecom.Call call = com.wtbruh.fakelauncher.service.InCallService.sCurrentCall;
            if (call != null) {
                call.disconnect();
                Log.i("CallHelper", "endCallViaService: disconnected via Call");
                return true;
            }
            Log.w("CallHelper", "endCallViaService: no current call");
        } catch (Throwable t) {
            Log.e("CallHelper", "endCallViaService failed: " + t.getMessage(), t);
        }
        return false;
    }

    public static boolean endCall(Context context) {
        try {
            TelephonyManager tm =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm == null) return false;

            // 豁免隐藏 API 限制（Android 13 getMethod 找不到 getITelephony）
            try {
                HiddenApiBypass.addHiddenApiExemptions(
                        "Landroid/telephony/TelephonyManager;",
                        "Lcom/android/internal/telephony/ITelephony;");
            } catch (Throwable t) {
                Log.w(TAG, "addHiddenApiExemptions failed", t);
            }

            // Method 1: getITelephony().endCall()（HiddenApiBypass 反射）
            try {
                Method getITelephony = HiddenApiBypass.getDeclaredMethod(
                        TelephonyManager.class, "getITelephony");
                getITelephony.setAccessible(true);
                Object iTelephony = getITelephony.invoke(tm);
                if (iTelephony != null) {
                    Method end = HiddenApiBypass.getDeclaredMethod(
                            iTelephony.getClass(), "endCall");
                    end.setAccessible(true);
                    Object result = end.invoke(iTelephony);
                    if (result instanceof Boolean && (Boolean) result) {
                        Log.d(TAG, "endCall via ITelephony: success");
                        return true;
                    }
                }
            } catch (Throwable e) {
                Log.e(TAG, "ITelephony.endCall failed", e);
            }

            // Method 2: TelephonyManager.endCall()（隐藏 API）
            try {
                Method end = HiddenApiBypass.getDeclaredMethod(
                        TelephonyManager.class, "endCall");
                Object result = end.invoke(tm);
                if (result instanceof Boolean && (Boolean) result) {
                    Log.d(TAG, "endCall via TelephonyManager: success");
                    return true;
                }
            } catch (Throwable e) {
                Log.e(TAG, "TelephonyManager.endCall failed", e);
            }
        } catch (Throwable e) {
            Log.e(TAG, "endCall failed", e);
        }
        return false;
    }
}
