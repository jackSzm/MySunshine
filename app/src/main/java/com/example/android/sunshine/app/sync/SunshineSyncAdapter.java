package com.example.android.sunshine.app.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.format.Time;
import android.util.Log;

import com.example.android.sunshine.app.MainActivity;
import com.example.android.sunshine.app.R;
import com.example.android.sunshine.app.Utility;
import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.data.WeatherContract.LocationEntry;
import com.example.android.sunshine.app.data.WeatherContract.WeatherEntry;

import java.io.IOException;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SunshineSyncAdapter extends AbstractThreadedSyncAdapter {
    public final String LOG_TAG = SunshineSyncAdapter.class.getSimpleName();
    public static final int SYNC_INTERVAL = 60 * 180;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;
    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static final int WEATHER_NOTIFICATION_ID = 3004;

    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[]{
            WeatherEntry.COLUMN_WEATHER_ID,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_MIN_TEMP,
            WeatherEntry.COLUMN_SHORT_DESC
    };

    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;
    private static final int INDEX_SHORT_DESC = 3;

    private final ContentResolver contentResolver;
    private final Context context;

    public SunshineSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);

        this.context = context;
        this.contentResolver = context.getContentResolver();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.d(LOG_TAG, "Starting sync");
        try {
            String locationQuery = Utility.getPreferredLocation(context);
            String forecastJsonStr = getDataFromApi(locationQuery);
            getWeatherDataFromJson(forecastJsonStr, locationQuery);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String getDataFromApi(String locationParameter) {
        String jsonResponse = null;
        try {
            URL url = SunshineUrlBuilder.buildUrl(locationParameter);
            Request request = new Request.Builder().url(url).build();

            OkHttpClient client = new OkHttpClient();
            Response response = client.newCall(request).execute();
            jsonResponse = response.body().string();
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        return jsonResponse;
    }

    private void getWeatherDataFromJson(String forecastJsonStr, String locationName) throws JSONException {

        try {
            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray("list");

            JSONObject cityJson = forecastJson.getJSONObject("city");
            String cityName = cityJson.getString("name");

            JSONObject cityCoord = cityJson.getJSONObject("coord");
            double cityLatitude = cityCoord.getDouble("lat");
            double cityLongitude = cityCoord.getDouble("lon");

            if (!isLocationInDb(locationName)) {
                saveLocation(locationName, cityName, cityLatitude, cityLongitude);
            }

            int weatherDays = weatherArray.length();

            ContentValues[] contentValuesArray = new ContentValues[weatherDays];

            Time dayTime = new Time();
            dayTime.setToNow();
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            dayTime = new Time();

            for (int i = 0; i < weatherDays; i++) {

                JSONObject dayForecast = weatherArray.getJSONObject(i);
                long dateTime = dayTime.setJulianDay(julianStartDay + i);

                double pressure = dayForecast.getDouble("pressure");
                int humidity = dayForecast.getInt("humidity");
                double windSpeed = dayForecast.getDouble("speed");
                double windDirection = dayForecast.getDouble("deg");

                JSONObject weatherObject = dayForecast.getJSONArray("weather").getJSONObject(0);
                String description = weatherObject.getString("main");
                int weatherId = weatherObject.getInt("id");

                JSONObject temperatureObject = dayForecast.getJSONObject("temp");
                double high = temperatureObject.getDouble("max");
                double low = temperatureObject.getDouble("min");

                ContentValues weatherValues = new ContentValues();
                weatherValues.put(WeatherEntry.COLUMN_LOCATION_NAME, locationName);
                weatherValues.put(WeatherEntry.COLUMN_DATE, dateTime);
                weatherValues.put(WeatherEntry.COLUMN_HUMIDITY, humidity);
                weatherValues.put(WeatherEntry.COLUMN_PRESSURE, pressure);
                weatherValues.put(WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
                weatherValues.put(WeatherEntry.COLUMN_DEGREES, windDirection);
                weatherValues.put(WeatherEntry.COLUMN_MAX_TEMP, high);
                weatherValues.put(WeatherEntry.COLUMN_MIN_TEMP, low);
                weatherValues.put(WeatherEntry.COLUMN_SHORT_DESC, description);
                weatherValues.put(WeatherEntry.COLUMN_WEATHER_ID, weatherId);

                contentValuesArray[i] = weatherValues;
            }

            if (weatherDays > 0) {
                deletePreviousData(locationName);
                contentResolver.bulkInsert(WeatherEntry.CONTENT_URI, contentValuesArray);

                notifyWeather();
            }

            Log.d(LOG_TAG, "Sync Complete. " + weatherDays + " Inserted");

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    private void deletePreviousData(String locationName) {
        String[] selectionArgs = {locationName};
        contentResolver.delete(WeatherEntry.CONTENT_URI, WeatherEntry.COLUMN_LOCATION_NAME + " = ?", selectionArgs);
    }

    private void notifyWeather() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key);
        boolean displayNotifications = prefs.getBoolean(
                displayNotificationsKey,
                Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default))
        );

        if (displayNotifications) {

            String locationQuery = Utility.getPreferredLocation(context);
            String dateToQuery = String.valueOf(WeatherContract.normalizeDate(System.currentTimeMillis()));
            String[] filterArgs = {dateToQuery, locationQuery};

            Cursor cursor = contentResolver.query(WeatherEntry.CONTENT_URI, NOTIFY_WEATHER_PROJECTION, WeatherEntry.DATE_AND_LOCATION_FILTER, filterArgs, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int weatherId = cursor.getInt(INDEX_WEATHER_ID);
                    double high = cursor.getDouble(INDEX_MAX_TEMP);
                    double low = cursor.getDouble(INDEX_MIN_TEMP);
                    String desc = cursor.getString(INDEX_SHORT_DESC);

                    int iconId = Utility.getIconResourceForWeatherCondition(weatherId);

                    Resources resources = context.getResources();
                    Bitmap largeIcon = BitmapFactory.decodeResource(resources, Utility.getArtResourceForWeatherCondition(weatherId));
                    String title = context.getString(R.string.app_name);

                    String contentText = buildContentMessage(high, low, desc);

                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(context)
                                    .setColor(resources.getColor(R.color.sunshine_light_blue))
                                    .setSmallIcon(iconId)
                                    .setLargeIcon(largeIcon)
                                    .setContentTitle(title)
                                    .setContentText(contentText);

                    Intent resultIntent = new Intent(context, MainActivity.class);

                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                    stackBuilder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                    mBuilder.setContentIntent(resultPendingIntent);

                    NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(WEATHER_NOTIFICATION_ID, mBuilder.build());

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.apply();
                }
                cursor.close();
            }
        }
    }

    private String buildContentMessage(double high, double low, String desc) {
        String notificationMessage = context.getString(R.string.format_notification);
        String maxTemp = Utility.formatTemperature(context, high);
        String minTemp = Utility.formatTemperature(context, low);
        return String.format(notificationMessage, desc, maxTemp, minTemp);
    }

    private boolean isLocationInDb(String locationName) {
        String[] projection = {LocationEntry._ID};
        String selection = LocationEntry.COLUMN_LOCATION_SETTING + " = ?";
        String[] selectionArgs = {locationName};

        Cursor locationCursor = contentResolver.query(LocationEntry.CONTENT_URI, projection, selection, selectionArgs, null);
        if (locationCursor != null && locationCursor.getCount() != 0) {
            locationCursor.close();
            return true;
        }
        return false;
    }

    private void saveLocation(String locationSetting, String cityName, double lat, double lon) {
        ContentValues locationValues = new ContentValues();
        locationValues.put(LocationEntry.COLUMN_CITY_NAME, cityName);
        locationValues.put(LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
        locationValues.put(LocationEntry.COLUMN_COORD_LAT, lat);
        locationValues.put(LocationEntry.COLUMN_COORD_LONG, lon);
        contentResolver.insert(LocationEntry.CONTENT_URI, locationValues);
    }

    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context), context.getString(R.string.content_authority), bundle);
    }

    public static Account getSyncAccount(Context context) {
        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        Account newAccount = new Account(context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        if (accountManager.getPassword(newAccount) == null) {
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);
        syncImmediately(context);
    }

    private static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            SyncRequest request = new SyncRequest.Builder()
                    .syncPeriodic(syncInterval, flexTime)
                    .setSyncAdapter(account, authority)
                    .setExtras(new Bundle())
                    .build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account, authority, new Bundle(), syncInterval);
        }
    }
}
