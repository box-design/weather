package com.skyweather.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class WidgetPrefs {
    private static final String PREFS_NAME = "skyweather_widget_prefs";
    private static final String KEY_PREFIX_CITY = "widget_city_";
    private static final String KEY_PREFIX_LAT = "widget_lat_";
    private static final String KEY_PREFIX_LON = "widget_lon_";
    private static final String KEY_PREFIX_TZ = "widget_tz_";
    private static final String KEY_CACHED_DATA = "cached_widget_data";
    private static final String KEY_SAVED_CITIES = "skyweather_saved_cities";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void setWidgetCity(Context context, int appWidgetId, String name, double lat, double lon, String tz) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString(KEY_PREFIX_CITY + appWidgetId, name);
        editor.putFloat(KEY_PREFIX_LAT + appWidgetId, (float) lat);
        editor.putFloat(KEY_PREFIX_LON + appWidgetId, (float) lon);
        editor.putString(KEY_PREFIX_TZ + appWidgetId, tz != null ? tz : "auto");
        editor.apply();
    }

    public static String getWidgetCityName(Context context, int appWidgetId) {
        return getPrefs(context).getString(KEY_PREFIX_CITY + appWidgetId, null);
    }

    public static double getWidgetLat(Context context, int appWidgetId) {
        return getPrefs(context).getFloat(KEY_PREFIX_LAT + appWidgetId, 0f);
    }

    public static double getWidgetLon(Context context, int appWidgetId) {
        return getPrefs(context).getFloat(KEY_PREFIX_LON + appWidgetId, 0f);
    }

    public static String getWidgetTimezone(Context context, int appWidgetId) {
        return getPrefs(context).getString(KEY_PREFIX_TZ + appWidgetId, "auto");
    }

    public static void removeWidgetCity(Context context, int appWidgetId) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.remove(KEY_PREFIX_CITY + appWidgetId);
        editor.remove(KEY_PREFIX_LAT + appWidgetId);
        editor.remove(KEY_PREFIX_LON + appWidgetId);
        editor.remove(KEY_PREFIX_TZ + appWidgetId);
        editor.apply();
    }

    public static void cacheWidgetData(Context context, String jsonData) {
        getPrefs(context).edit().putString(KEY_CACHED_DATA, jsonData).apply();
    }

    public static String getCachedWidgetData(Context context) {
        return getPrefs(context).getString(KEY_CACHED_DATA, null);
    }

    public static class SavedCity {
        public String id;
        public String name;
        public double latitude;
        public double longitude;
        public String country;
        public String timezone;
        public boolean isDefault;

        public SavedCity(String id, String name, double latitude, double longitude, String country, String timezone, boolean isDefault) {
            this.id = id;
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
            this.country = country;
            this.timezone = timezone;
            this.isDefault = isDefault;
        }
    }

    public static List<SavedCity> getSavedCities(Context context) {
        List<SavedCity> cities = new ArrayList<>();
        try {
            String raw = getPrefs(context).getString(KEY_SAVED_CITIES, null);
            if (raw == null) return cities;
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                cities.add(new SavedCity(
                    obj.optString("id", ""),
                    obj.optString("name", ""),
                    obj.optDouble("latitude", 0),
                    obj.optDouble("longitude", 0),
                    obj.optString("country", ""),
                    obj.optString("timezone", "auto"),
                    "1".equals(obj.optString("isDefault", "0"))
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cities;
    }

    public static void saveCities(Context context, List<SavedCity> cities) {
        try {
            JSONArray arr = new JSONArray();
            for (SavedCity city : cities) {
                JSONObject obj = new JSONObject();
                obj.put("id", city.id);
                obj.put("name", city.name);
                obj.put("latitude", city.latitude);
                obj.put("longitude", city.longitude);
                obj.put("country", city.country);
                obj.put("timezone", city.timezone);
                obj.put("isDefault", city.isDefault ? "1" : "0");
                arr.put(obj);
            }
            getPrefs(context).edit().putString(KEY_SAVED_CITIES, arr.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static SavedCity getDefaultCity(Context context) {
        List<SavedCity> cities = getSavedCities(context);
        for (SavedCity city : cities) {
            if (city.isDefault) return city;
        }
        return cities.isEmpty() ? null : cities.get(0);
    }
}
