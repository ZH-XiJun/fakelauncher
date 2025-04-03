package com.wtbruh.fakelauncher.utils;

import android.util.Log;

import java.io.IOException;

public class PrivilegeProvider {
    public final static String TAG = "PrivilegeProvider";
    // Fail message
    public final static String FAIL_ROOT = "FAIL_ROOT";
    public final static String FAIL_SHIZUKU = "FAIL_SHIZUKU";
    public final static String FAIL_RUN = "FAIL_RUN";
    // Run method to int
    public final static int METHOD_NORMAL = 0;
    public final static int METHOD_ROOT = 1;
    public final static int METHOD_SHIZUKU = 2;
    // Command define
    public final static String CMD_SU = "su";
    public final static String CMD_BUSYBOX = "busybox";
    public final static String CMD_SH = "sh";
    public final static String CMD_RISH= "rish";

    private static String prefix(int method) {
        switch (method) {
            case METHOD_NORMAL:
                return "";
            case METHOD_ROOT:
                return CMD_SU+"-c ";
            case METHOD_SHIZUKU:
                return CMD_RISH+" ";
            default:
                return "";
        }
    }
    public static void get_root() {
        try {
            Runtime.getRuntime().exec(CMD_SU);
        } catch (IOException e) {
            Log.e(TAG, "Get root failed!");
            throw new RuntimeException(FAIL_ROOT);
        }
    }

    // Single cmd running
    public static void runCmd(String cmd, int method) {
        try {
            Runtime.getRuntime().exec(prefix(method)+cmd);
        } catch (IOException e) {
            throw new RuntimeException(FAIL_RUN);
        }
    }

}
