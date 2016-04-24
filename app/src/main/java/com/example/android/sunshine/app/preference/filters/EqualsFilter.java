package com.example.android.sunshine.app.preference.filters;

import org.json.JSONException;
import org.json.JSONObject;

public class EqualsFilter implements Filter {

    private final String key;

    private final String expectedValue;

    public EqualsFilter(String key, String expectedValue) {
        this.key = key;
        this.expectedValue = expectedValue;
    }

    @Override
    public boolean filter(JSONObject object) {
        try {
            String objectValue = object.getString(key);
            return objectValue.equals(expectedValue);
        } catch (JSONException e) {
            return false;
        }
    }
}
