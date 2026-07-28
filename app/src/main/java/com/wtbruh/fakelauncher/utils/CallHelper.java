package com.wtbruh.fakelauncher.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.util.Log;

import com.wtbruh.fakelauncher.ApplicationHelper;
import com.wtbruh.fakelauncher.FakeInCallActivity;

/**
 * Place a real system call while keeping FakeLauncher (and the fake in-call UI)
 * pinned in the foreground. Do not unlock lock-task / pin mode for the dialer.
 */
public final class CallHelper {
    private static final String TAG = CallHelper.class.getSimpleName();

    private CallHelper() {}

    public static boolean placeCall(Context context, String rawNumber) {
        if (rawNumber == null) return false;
        String dialNumber = rawNumber.replaceAll("[^0-9*#+]", "");
        if (dialNumber.isEmpty()) return false;

        ApplicationHelper.dialing = true;
        ApplicationHelper.fakeCallNumber = dialNumber;

        // Keep current FakeLauncher pin. Releasing pin was the reason system dialer
        // flashed on top and briefly allowed leaving FakeLauncher.
        FakeInCallActivity.start(context, dialNumber);

        Uri uri = Uri.fromParts("tel", dialNumber, null);

        // Prefer TelecomManager: places the call without intentionally launching
        // the system dialer Activity into the foreground.
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

        // Fallback still does not unlock pin. System InCall UI may try to start but
        // should stay blocked by lock-task while FakeInCall covers the session.
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
            restoreAfterFailure(context);
            return false;
        }
    }

    private static void restoreAfterFailure(Context context) {
        ApplicationHelper.dialing = false;
        ApplicationHelper.fakeCallNumber = "";
        FakeInCallActivity.requestEnd(context);
        if (context instanceof Activity) {
            ContentProvider.setTaskId(context, ((Activity) context).getTaskId());
        }
    }
}