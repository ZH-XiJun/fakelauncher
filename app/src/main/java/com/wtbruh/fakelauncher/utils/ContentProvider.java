package com.wtbruh.fakelauncher.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 进程内的「状态黑板」，通过 ContentResolver 把两个标量在应用与 Xposed 侧之间传递：
 * <ul>
 *   <li>{@link #KEY_TASKID} —— 当前要锁屏固定的 taskId，{@link #NOT_SET} 表示未锁定</li>
 *   <li>{@link #KEY_XPOSED_API} —— SelfHook 上报的 Xposed 框架 API 版本号，&lt;=0 表示未激活/未知</li>
 * </ul>
 *
 * <p>写入会触发 {@code notifyChange}，观察者（应用侧 {@code ApplicationHelper}，
 * Xposed 侧 {@code PinningHook}）据此执行 startLockTask / stopLockTask。
 *
 * <p><b>这两个值都是会话级状态，不持久化</b> —— 应用进程一结束即归零。因此：
 * <ul>
 *   <li>进程启动时广播一次确定态（{@link #resetTaskId}），纠正 hook 侧可能残留的旧值；</li>
 *   <li>所有退出/崩溃重启路径也必须广播确定态，否则 hook 侧会一直以为还锁着。</li>
 * </ul>
 *
 * @author ZH-XiJun
 */
public class ContentProvider extends android.content.ContentProvider {

    private final static String TAG = ContentProvider.class.getSimpleName();
    private final static String AUTHORITY = "com.wtbruh.fakelauncher";

    public final static Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY);
    public final static String KEY_TASKID = "taskId";
    public final static String KEY_XPOSED_API = "xposedApi";
    /** 未锁定 / 未上报的哨兵值 */
    public final static int NOT_SET = -1;

    private final static String[] COLUMNS = new String[]{KEY_TASKID, KEY_XPOSED_API};
    private final static AtomicInteger currentTaskId = new AtomicInteger(NOT_SET);
    private final static AtomicInteger xposedApi = new AtomicInteger(NOT_SET);

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        MatrixCursor cursor = new MatrixCursor(COLUMNS);
        cursor.addRow(new Object[]{currentTaskId.get(), xposedApi.get()});
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (values == null) return null;
        if (applyState(values)) notifyChangeSafely(uri);
        return uri;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        return applyState(values) ? 1 : 0;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        // 重置为未锁定/未上报，并强制通知：用于进程启动、退出、崩溃重启等需要广播确定态的场景
        currentTaskId.set(NOT_SET);
        xposedApi.set(NOT_SET);
        notifyChangeSafely(uri);
        return 1;
    }

    /**
     * 把 ContentValues 里的已知键写入状态，只有值真的变了才返回 true。
     * 调用方据此决定是否通知 —— 避免「值没变也通知」导致重复的锁屏转换
     * （例如 MainActivity.onResume 每次都会写一次相同的 taskId）。
     *
     * <p>注意 {@code getAsInteger} 可能返回 null（值不是整数），必须判空，
     * 否则拆箱会 NPE —— 而本 provider 与应用同进程，崩的就是应用自己。
     */
    private boolean applyState(@Nullable ContentValues values) {
        if (values == null) return false;
        boolean changed = false;
        Integer taskId = values.getAsInteger(KEY_TASKID);
        if (taskId != null && currentTaskId.getAndSet(taskId) != taskId) changed = true;
        Integer api = values.getAsInteger(KEY_XPOSED_API);
        if (api != null && xposedApi.getAndSet(api) != api) changed = true;
        return changed;
    }

    /**
     * 通知失败不应让写入失败：观察者抛出的异常会顺着 notifyChange 冒泡回调用方，
     * 在最坏情况下会把应用主线程干掉，所以这里整体兜住。
     */
    private void notifyChangeSafely(@NonNull Uri uri) {
        try {
            Context context = getContext();
            if (context != null) context.getContentResolver().notifyChange(uri, null);
            else Log.w(TAG, "notifyChange skipped: context is null");
        } catch (Throwable e) {
            Log.e(TAG, "notifyChange failed", e);
        }
    }

    // ── 供应用与 hook 使用的静态 API ──────────────────────────────────────────────

    /**
     * 写入 taskId（值未变化时不发通知）。
     * @param taskId {@link #NOT_SET} 表示解锁
     */
    public static void setTaskId(Context context, int taskId) {
        write(context, KEY_TASKID, taskId);
    }

    /**
     * 重置为未锁定并<b>强制</b>广播，用于进程启动/退出/崩溃重启时把确定态同步给 hook 侧。
     * （hook 侧的状态不会随应用进程消失，必须显式清）
     */
    public static void resetTaskId(Context context) {
        if (context == null) return;
        try {
            context.getContentResolver().delete(CONTENT_URI, null, null);
        } catch (Throwable e) {
            Log.e(TAG, "resetTaskId failed", e);
        }
    }

    /**
     * 由 SelfHook 上报 Xposed 框架 API 版本号；&lt;=0 视为未激活/未知。
     */
    public static void setXposedApi(Context context, int apiVersion) {
        write(context, KEY_XPOSED_API, apiVersion);
    }

    /**
     * @return 当前 taskId，读不到返回 {@link #NOT_SET}
     */
    public static int getTaskId(Context context) {
        return readInt(context, KEY_TASKID, NOT_SET);
    }

    /**
     * @return SelfHook 上报的 Xposed API 版本号，读不到/未上报返回 {@link #NOT_SET}
     */
    public static int getXposedApi(Context context) {
        return readInt(context, KEY_XPOSED_API, NOT_SET);
    }

    private static void write(Context context, String key, int value) {
        if (context == null) return;
        try {
            ContentValues values = new ContentValues(1);
            values.put(key, value);
            context.getContentResolver().insert(CONTENT_URI, values);
        } catch (Throwable e) {
            Log.e(TAG, "write " + key + " failed", e);
        }
    }

    /** 按列名读取；查询失败、列不存在、值为 null 时返回默认值，不向上抛 */
    private static int readInt(Context context, String key, int defaultValue) {
        if (context == null) return defaultValue;
        try (Cursor cursor = context.getContentResolver().query(CONTENT_URI, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(key);
                if (index >= 0 && !cursor.isNull(index)) return cursor.getInt(index);
            }
        } catch (Throwable e) {
            Log.e(TAG, "read " + key + " failed", e);
        }
        return defaultValue;
    }
}
