package com.example.android.sunshine.app.preference;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;

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
    private Map<String, JSONObject> getAll(String tableName) {
        SharedPreferences preferences = getSharedPreferences(tableName);
        return (Map<String, JSONObject>) preferences.getAll();
    }

    public Cursor query(String tableName, String[] projection, String selection, String[] selectionArgs, Object o) {

        //TODO : Handle Selection , SelectionArgs and SORT
        Map<String, JSONObject> all = getAll(tableName);

        MatrixCursor mc = new MatrixCursor(projection);
        for (JSONObject jsonObject : all.values()) {
            String[] values = new String[projection.length];
            for (int i = 0; i < projection.length; i++) {
                try {
                    values[i] = jsonObject.getString(projection[i]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            mc.addRow(values);
        }
        return mc;
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
