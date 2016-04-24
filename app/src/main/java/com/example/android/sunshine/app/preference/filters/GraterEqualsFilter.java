package com.example.android.sunshine.app.preference.filters;

import org.json.JSONException;
import org.json.JSONObject;

public class GraterEqualsFilter implements Filter {
    private final String key;

    private final String expectedValue;

    public GraterEqualsFilter(String key, String expectedValue) {
        this.key = key;
        this.expectedValue = expectedValue;
    }

    @Override
    public boolean filter(JSONObject object) {
        try {
            String objectValue = object.getString(key);
            int comparisonResult = objectValue.compareTo(expectedValue);
            return comparisonResult >= 0;
        } catch (JSONException e) {
            return false;
        }
    }
}
