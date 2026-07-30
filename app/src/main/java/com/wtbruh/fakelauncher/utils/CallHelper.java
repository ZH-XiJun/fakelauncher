package com.wtbruh.fakelauncher.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.util.Log;

/**
 * Place a real system call. The fake in-call UI is now handled by
 * {@link com.wtbruh.fakelauncher.ui.fragment.phone.FakeInCallFragment} via SubActivity.
 */
public final class CallHelper {
    private static final String TAG = CallHelper.class.getSimpleName();

    private CallHelper() {}

    public static boolean placeCall(Context context, String rawNumber) {
        if (rawNumber == null) return false;
        String dialNumber = rawNumber.replaceAll("[^0-9*#+]", "");
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
}
