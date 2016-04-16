/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.sunshine.app.data;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;

@SuppressWarnings("ConstantConditions")
public class WeatherProvider extends ContentProvider {

    private WeatherDbHelper mOpenHelper;
    private ContentResolver contentResolver;

    @Override
    public boolean onCreate() {
        mOpenHelper = new WeatherDbHelper(getContext());
        contentResolver = getContext().getContentResolver();
        return true;
    }

    private SQLiteDatabase getReadableDatabase() {
        return mOpenHelper.getReadableDatabase();
    }

    private SQLiteDatabase getWritableDatabase() {
        return mOpenHelper.getWritableDatabase();
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase database = getReadableDatabase();
        String tableName = getTableName(uri);
        Cursor retCursor = database.query(tableName, projection, selection, selectionArgs, null, null, sortOrder);
        retCursor.setNotificationUri(contentResolver, uri);
        return retCursor;
    }

    private String getTableName(@NonNull Uri uri) {
        return uri.getLastPathSegment();
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        SQLiteDatabase db = getWritableDatabase();
        String tableName = getTableName(uri);
        Uri returnUri;
        long id = db.insert(tableName, null, values);
        if (id != -1) {
            returnUri = ContentUris.withAppendedId(uri, id);
        } else {
            throw new SQLException("Failed to insert row into " + uri);
        }

        contentResolver.notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = getWritableDatabase();
        String tableName = getTableName(uri);

        if (null == selection) {
            selection = "1";
        }

        int rowsDeleted = db.delete(tableName, selection, selectionArgs);
        if (rowsDeleted != 0) {
            contentResolver.notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        String tableName = getTableName(uri);

        int rowsUpdated = getWritableDatabase().update(tableName, values, selection, selectionArgs);
        if (rowsUpdated != 0) {
            contentResolver.notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        SQLiteDatabase db = getWritableDatabase();
        String tableName = getTableName(uri);

        db.beginTransaction();
        int returnCount = 0;

        try {
            for (ContentValues value : values) {
                long id = db.insert(tableName, null, value);
                if (id != -1) {
                    returnCount++;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        contentResolver.notifyChange(uri, null);
        return returnCount;
    }

    @Override
    @TargetApi(11)
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
    }
}
