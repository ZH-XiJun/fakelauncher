package com.wtbruh.fakelauncher.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

public class ContentProvider extends android.content.ContentProvider {

    private final static String AUTHORITY = "com.wtbruh.fakelauncher";
    private final static AtomicInteger currentTaskId = new AtomicInteger(-1);
    public final static Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY);
    public final static String KEY_TASKID = "taskId";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] strings, @Nullable String s, @Nullable String[] strings1, @Nullable String s1) {
        MatrixCursor cursor = new MatrixCursor(new String[]{KEY_TASKID});
        cursor.addRow(new Integer[]{currentTaskId.get()});
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return "";
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        if (contentValues != null && contentValues.containsKey(KEY_TASKID)) {
            currentTaskId.set(contentValues.getAsInteger(KEY_TASKID));
            getContext().getContentResolver().notifyChange(uri, null);
            return uri;
        }
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
        currentTaskId.set(-1);
        getContext().getContentResolver().notifyChange(uri, null);
        return 1;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        if (contentValues != null && contentValues.containsKey(KEY_TASKID)) {
            currentTaskId.set(contentValues.getAsInteger(KEY_TASKID));
            getContext().getContentResolver().notifyChange(uri, null);
            return 1;
        }
        return 0;
    }

    public static void setTaskId(Context context, int taskId) {
        ContentValues values = new ContentValues();
        values.put(KEY_TASKID, taskId);
        context.getContentResolver().insert(CONTENT_URI, values);
    }

    public static int getTaskId(Context context) {
        try (Cursor cursor = context.getContentResolver().query(
                CONTENT_URI, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }
        return -1;
    }
}
