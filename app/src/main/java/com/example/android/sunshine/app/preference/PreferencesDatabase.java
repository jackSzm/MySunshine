package com.example.android.sunshine.app.preference;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.support.annotation.NonNull;

import com.example.android.sunshine.app.preference.filters.EqualsFilter;
import com.example.android.sunshine.app.preference.filters.Filter;
import com.example.android.sunshine.app.preference.filters.GraterEqualsFilter;
import com.example.android.sunshine.app.preference.sorters.AscSorter;
import com.example.android.sunshine.app.preference.sorters.Sorter;

import java.util.ArrayList;
import java.util.List;
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

    public Cursor query(String tableName, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        Map<String, String> all = getAll(tableName);

        Filter[] filters = buildFilters(selection, selectionArgs);

        MatrixCursor mc = new MatrixCursor(projection);

        List<JSONObject> filteredObjects = new ArrayList<>();
        for (String object : all.values()) {
            try {
                JSONObject jsonObject = new JSONObject(object);
                if (passesFilters(jsonObject, filters)) {
                    filteredObjects.add(jsonObject);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Sorter sorter = getSorterFrom(sortOrder);
        sorter.sort(filteredObjects);

        for (JSONObject filteredObject : filteredObjects) {
            try {
                String[] values = populateArrayWithJson(projection, filteredObject);
                mc.addRow(values);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return mc;
    }

    private Sorter getSorterFrom(String sortOrder) {
        if (sortOrder != null && sortOrder.contains("ASC")) {
            String fieldToSort = sortOrder.replace("ASC", "").trim();
            return new AscSorter(fieldToSort);
        } else {
            return Sorter.NO_OP;
        }
    }

    private Filter[] buildFilters(String selection, String[] selectionArgs) {
        if (selection == null || selectionArgs == null) {
            return new Filter[0];
        }

        String[] rawFilters = selection.split("AND");

        if (rawFilters.length != selectionArgs.length) {
            return new Filter[0];
        }

        Filter[] filters = new Filter[rawFilters.length];

        for (int i = 0; i < rawFilters.length; i++) {
            String rawFilter = rawFilters[i];
            if (rawFilter.contains(">=")) {
                String key = rawFilter.split(">=")[0].trim();
                filters[i] = new GraterEqualsFilter(key, selectionArgs[i]);
            } else if (rawFilter.contains("=")) {
                String key = rawFilter.split("=")[0].trim();
                filters[i] = new EqualsFilter(key, selectionArgs[i]);
            } else {
                filters[i] = Filter.NO_OP;
            }
        }

        return filters;
    }

    private boolean passesFilters(JSONObject jsonObject, Filter[] filters) {
        for (Filter filter : filters) {
            if (!filter.filter(jsonObject)) {
                return false;
            }
        }
        return true;
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
        SharedPreferences sharedPreferences = getSharedPreferences(tableName);
        Map<String, String> all = getAll(tableName);

        int counter = 0;
        Filter[] filters = buildFilters(selection, selectionArgs);

        for (String key : all.keySet()) {
            String object = all.get(key);
            try {
                JSONObject jsonObject = new JSONObject(object);
                if (passesFilters(jsonObject, filters)) {
                    sharedPreferences.edit().remove(key).apply();
                    counter++;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return counter;
    }

    public int update(String tableName, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
