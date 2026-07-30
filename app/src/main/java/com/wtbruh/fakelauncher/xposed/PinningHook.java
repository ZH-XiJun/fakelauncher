package com.wtbruh.fakelauncher.xposed;

import android.app.Application;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wtbruh.fakelauncher.utils.ContentProvider;
import com.wtbruh.fakelauncher.utils.HookHelper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class PinningHook extends HookHelper {

    public final static int LOCK_APP = 1;
    public final static int UNLOCK_APP = 2;
    private static int sTaskId = -1;
    private boolean mObserver = false;
    private static boolean sLock = false;
    public static Handler mHandler = new LockAppHandler();

    private final static String TAG = PinningHook.class.getSimpleName();

    @Override
    public void init() {
        // Observe the task id from the content provider and start/stop screen pinning accordingly
        // 观察ContentProvider中的任务ID变化来判断是否开启/关闭屏幕固定模式
        findAndHookMethod("com.android.server.wm.ActivityTaskManagerService", "onSystemReady", new HookAction() {
            @Override
            protected void after(XC_MethodHook.MethodHookParam param) {

                try {
                    Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    if (context == null) {
                        context = findContext(FlAG_ONLY_ANDROID);
                        if (context == null) {
                            logE(tag, "onSystemReady context is null!!");
                            return;
                        }
                    }
                    if (!mObserver) {
                        Context finalContext = context;
                        ContentObserver contentObserver = new ContentObserver(new Handler(finalContext.getMainLooper())) {
                            @Override
                            public void onChange(boolean selfChange, @Nullable Uri uri, int flags) {
                                Cursor cursor = finalContext.getContentResolver().query(uri, null, null, null, null);
                                if (cursor != null && cursor.moveToFirst()) {
                                    sTaskId = cursor.getInt(0);
                                    cursor.close();
                                }
                                logI(TAG, "Got task id: " + sTaskId);
                                sLock = sTaskId != -1;
                                if (sLock) {
                                    callMethod(param.thisObject, "startSystemLockTaskMode", sTaskId);
                                    PhoneWindowManagerHook.setNavBarHidden(true);
                                } else {
                                    callMethod(param.thisObject, "stopSystemLockTaskMode");
                                    PhoneWindowManagerHook.setNavBarHidden(false);
                                }
                            }
                        };
                        context.getContentResolver().registerContentObserver(
                                ContentProvider.CONTENT_URI, false, contentObserver
                        );
                        mObserver = true;
                    }
                } catch (Throwable e) {
                    logE(tag, "E: " + e);
                }
            }
        });
    }


    public static int getLockApp() {
        return sTaskId;
    }

    public static boolean getLockState() {
        return sLock;
    }

    /**
     * Handle message to turn on/off screen pinning<br>
     * 处理消息以开关屏幕固定
     *
     * @noinspection deprecation
     * @author HChenX
     */
    public static class LockAppHandler extends Handler {

        private final static String TAG = LockAppHandler.class.getSimpleName();

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            logI(TAG, "Received message! ");
            Context context = findContext(FlAG_ONLY_ANDROID);
            logI(TAG, "Context package name: " + context.getPackageName());
            if (context == null) {
                logI(TAG, "Context is null!!!");
                mHandler.sendMessageDelayed(mHandler.obtainMessage(msg.what), 500);
                return;
            }
            logI(TAG, "Message content: " + msg.what);
            switch (msg.what) {
                case LOCK_APP:
                    //setLockApp(context, (int) msg.obj);
                    break;
                case UNLOCK_APP:
                    //setLockApp(context, -1);
                    break;
            }
        }
    }

}

