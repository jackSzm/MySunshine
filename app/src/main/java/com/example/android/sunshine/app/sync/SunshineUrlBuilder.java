package com.example.android.sunshine.app.sync;

import android.net.Uri;

import com.example.android.sunshine.app.BuildConfig;

import java.net.MalformedURLException;
import java.net.URL;

class SunshineUrlBuilder {

    static URL buildUrl(String locationQueryParameter) {
        URL url;
        try {
            final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String QUERY_PARAM = "q";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";
            final String APPID_PARAM = "APPID";

            String format = "json";
            String units = "metric";
            int numDays = 14;

            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, locationQueryParameter)
                    .appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                    .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                    .build();

            url = new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            throw new RuntimeException("MalformedURLException : " + e.getMessage());
        }
        return url;
    }
}
