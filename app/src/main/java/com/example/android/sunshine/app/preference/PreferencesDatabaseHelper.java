package com.example.android.sunshine.app.preference;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;
import java.util.Set;

public class PreferencesDatabaseHelper {

    private final Context context;

    private static final String PROVIDER_PREFERENCES = "weather_preferences_provider";

    public PreferencesDatabaseHelper(Context context) {
        this.context = context;
    }

    public int getNextIdFor(String preferenceName) {
        SharedPreferences preferences = context.getSharedPreferences(PROVIDER_PREFERENCES, Context.MODE_PRIVATE);
        int id = preferences.getInt(preferenceName, 1);
        preferences.edit().putInt(preferenceName, id + 1).apply();
        return id;
    }

    public String parseToJson(ContentValues values, int id) {
        Set<Map.Entry<String, Object>> entries = values.valueSet();

        StringBuilder stringBuilder = new StringBuilder("{");

        for (Map.Entry<String, Object> entry : entries) {
            appendKeyValue(stringBuilder, entry.getKey(), String.valueOf(entry.getValue()));
            stringBuilder.append("\",");
        }

        appendKeyValue(stringBuilder, "_id", String.valueOf(id));
        stringBuilder.append("\"}");

        return stringBuilder.toString();
    }

    private void appendKeyValue(StringBuilder stringBuilder, String key, String value) {
        stringBuilder.append("\"");
        stringBuilder.append(key);
        stringBuilder.append("\":\"");
        stringBuilder.append(value);
    }
}
