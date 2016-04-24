package com.example.android.sunshine.app.preference.filters;

import org.json.JSONObject;

public interface Filter {

    boolean filter(JSONObject object);

    Filter NO_OP = new Filter() {

        @Override
        public boolean filter(JSONObject object) {
            return true;
        }

    };
}
