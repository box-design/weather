package com.skyweather.app;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Bridges localStorage city data from Web layer to Native SharedPreferences.
 * Call syncCitiesFromWeb() after the WebView has loaded.
 */
public class WidgetSyncBridge {
    private static final String TAG = "WidgetSyncBridge";
    private static final String STORAGE_KEY = "skyweather_saved_cities";

    public interface SyncCallback {
        void onSuccess(int syncedCount);
        void onError(String error);
    }

    /**
     * Sync cities from Web localStorage to Native SharedPreferences.
     * Uses JavaScript injection to read localStorage data.
     */
    public static void syncCitiesFromWeb(WebView webView, Context context, SyncCallback callback) {
        if (webView == null) {
            if (callback != null) callback.onError("WebView is null");
            return;
        }

        String js = "(() => {" +
            "  try {" +
            "    const raw = localStorage.getItem('" + STORAGE_KEY + "');" +
            "    return raw || '[]';" +
            "  } catch(e) {" +
            "    return '[]';" +
            "  }" +
            "})()";

        try {
            webView.evaluateJavascript(js, result -> {
                try {
                    String cleanResult = result.replace("\"", "");
                    if (cleanResult.equals("null") || cleanResult.isEmpty()) {
                        if (callback != null) callback.onSuccess(0);
                        return;
                    }
                    JSONArray arr = new JSONArray(cleanResult);
                    int syncedCount = 0;
                    List<WidgetPrefs.SavedCity> nativeCities = new ArrayList<>();

                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        WidgetPrefs.SavedCity city = new WidgetPrefs.SavedCity(
                            obj.optString("id", ""),
                            obj.optString("name", ""),
                            Double.parseDouble(obj.optString("latitude", "0")),
                            Double.parseDouble(obj.optString("longitude", "0")),
                            obj.optString("country", ""),
                            obj.optString("timezone", "auto"),
                            "1".equals(obj.optString("isDefault", "0"))
                        );
                        nativeCities.add(city);
                        syncedCount++;
                    }

                    WidgetPrefs.saveCities(context, nativeCities);
                    Log.d(TAG, "Synced " + syncedCount + " cities from web to native");
                    if (callback != null) callback.onSuccess(syncedCount);
                } catch (Exception e) {
                    Log.e(TAG, "syncCitiesFromWeb parse error", e);
                    if (callback != null) callback.onError(e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "syncCitiesFromWeb error", e);
            if (callback != null) callback.onError(e.getMessage());
        }
    }

    /**
     * Also sync a single city selection directly (used when user picks a city in the app).
     */
    public static void syncDefaultCity(Context context) {
        WidgetPrefs.SavedCity defaultCity = WidgetPrefs.getDefaultCity(context);
        if (defaultCity != null) {
            // Sync to web layer via JS
            Log.d(TAG, "Default city for widget: " + defaultCity.name);
        }
    }
}
