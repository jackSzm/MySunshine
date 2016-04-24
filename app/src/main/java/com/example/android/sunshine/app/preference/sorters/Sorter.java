package com.example.android.sunshine.app.preference.sorters;

import java.util.List;

import org.json.JSONObject;

public interface Sorter {

    void sort(List<JSONObject> list);

    Sorter NO_OP = new Sorter() {

        @Override
        public void sort(List<JSONObject> list) {
            // no-op
        }
    };
}
