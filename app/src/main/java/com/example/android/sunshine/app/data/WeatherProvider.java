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
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.example.android.sunshine.app.data.WeatherContract.LocationEntry;
import com.example.android.sunshine.app.data.WeatherContract.WeatherEntry;

@SuppressWarnings("ConstantConditions")
public class WeatherProvider extends ContentProvider {

    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private WeatherDbHelper mOpenHelper;

    static final int WEATHER = 100;
    static final int LOCATION = 300;

    private ContentResolver contentResolver;

    static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = WeatherContract.CONTENT_AUTHORITY;
        matcher.addURI(authority, WeatherContract.PATH_WEATHER, WEATHER);
        matcher.addURI(authority, WeatherContract.PATH_LOCATION, LOCATION);
        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new WeatherDbHelper(getContext());
        contentResolver = getContext().getContentResolver();
        return true;
    }

    private SQLiteDatabase getReadableDatabase() {
        return mOpenHelper.getReadableDatabase();
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
        normalizeDate(values);
        long id = db.insert(tableName, null, values);

        if (id > 0) {
            returnUri = ContentUris.withAppendedId(uri, id);
        } else {
            throw new SQLException("Failed to insert row into " + uri);
        }

        contentResolver.notifyChange(uri, null);
        return returnUri;
    }

    private SQLiteDatabase getWritableDatabase() {
        return mOpenHelper.getWritableDatabase();
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        if (null == selection) {
            selection = "1";
        }
        switch (match) {
            case WEATHER:
                rowsDeleted = db.delete(WeatherEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case LOCATION:
                rowsDeleted = db.delete(LocationEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsDeleted != 0) {
            contentResolver.notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    private void normalizeDate(ContentValues values) {
        if (values.containsKey(WeatherEntry.COLUMN_DATE)) {
            long dateValue = values.getAsLong(WeatherEntry.COLUMN_DATE);
            values.put(WeatherEntry.COLUMN_DATE, WeatherContract.normalizeDate(dateValue));
        }
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case WEATHER:
                normalizeDate(values);
                rowsUpdated = db.update(WeatherEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case LOCATION:
                rowsUpdated = db.update(LocationEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            contentResolver.notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        final SQLiteDatabase db = getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case WEATHER:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        normalizeDate(value);
                        long _id = db.insert(WeatherEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                contentResolver.notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }

    @Override
    @TargetApi(11)
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
    }
}
