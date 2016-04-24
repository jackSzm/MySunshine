package com.example.android.sunshine.app.preference.sorters;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

public class AscSorter implements Sorter {

    private final String fieldToCompare;

    public AscSorter(String fieldToCompare) {
        this.fieldToCompare = fieldToCompare;
    }

    @Override
    public void sort(List<JSONObject> list) {
        Collections.sort(list, new CustomComparator(fieldToCompare));
    }

    private static class CustomComparator implements Comparator<JSONObject> {
        private final String fieldToCompare;

        public CustomComparator(String fieldToCompare) {
            this.fieldToCompare = fieldToCompare;
        }

        @Override
        public int compare(JSONObject o1, JSONObject o2) {
            try {
                String string1 = o1.getString(fieldToCompare);
                String string2 = o2.getString(fieldToCompare);
                return string1.compareTo(string2);
            } catch (JSONException e) {
                throw new RuntimeException("Comparison Error : " + e.getMessage());
            }
        }
    }

}
