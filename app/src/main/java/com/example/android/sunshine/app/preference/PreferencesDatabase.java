package com.example.android.sunshine.app.preference;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.support.annotation.NonNull;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class PreferencesDatabase {

    private final Context context;
    private final PreferencesDatabaseHelper helper;

    public static PreferencesDatabase newInstance(Context context) {
        PreferencesDatabaseHelper preferencesDatabaseHelper = new PreferencesDatabaseHelper(context);
        return new PreferencesDatabase(context, preferencesDatabaseHelper);
    }

    public PreferencesDatabase(Context context, PreferencesDatabaseHelper helper) {
        this.context = context;
        this.helper = helper;
    }

    private SharedPreferences getSharedPreferences(String tableName) {
        return context.getSharedPreferences(tableName, Context.MODE_PRIVATE);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getAll(String tableName) {
        SharedPreferences preferences = getSharedPreferences(tableName);
        return (Map<String, String>) preferences.getAll();
    }

    public Cursor query(String tableName, String[] projection, String selection, String[] selectionArgs, Object o) {

        //TODO : Handle Selection , SelectionArgs and SORT
        Map<String, String> all = getAll(tableName);

        MatrixCursor mc = new MatrixCursor(projection);
        for (String object : all.values()) {
            try {
                JSONObject jsonObject = new JSONObject(object);
                String[] values = populateArrayWithJson(projection, jsonObject);
                mc.addRow(values);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return mc;
    }

    @NonNull
    private String[] populateArrayWithJson(String[] projection, JSONObject jsonObject) throws JSONException {
        String[] values = new String[projection.length];
        for (int i = 0; i < projection.length; i++) {
            values[i] = jsonObject.getString(projection[i]);
        }
        return values;
    }

    public long insert(String tableName, Object o, ContentValues values) {
        SharedPreferences sharedPreferences = getSharedPreferences(tableName);

        int id = helper.getNextIdFor(tableName);
        String json = helper.parseToJson(values, id);

        sharedPreferences.edit().putString(String.valueOf(id), json).apply();
        return id;
    }

    public int delete(String tableName, String selection, String[] selectionArgs) {
        return 0;
    }

    public int update(String tableName, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
