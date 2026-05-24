package com.skyweather.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class SunriseSunsetWidget extends AppWidgetProvider {
    private static final String TAG = "SunriseSunsetWidget";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        WidgetUpdateScheduler.schedule(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        WidgetUpdateScheduler.cancel(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            WidgetPrefs.removeWidgetCity(context, id);
        }
        super.onDeleted(context, appWidgetIds);
    }

    public static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_sunrise_sunset);

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

        String cachedJson = WidgetPrefs.getCachedWidgetData(context);
        if (cachedJson != null) {
            try {
                JSONObject obj = new JSONObject(cachedJson);
                JSONArray dailyArr = obj.optJSONArray("daily");
                if (dailyArr != null && dailyArr.length() > 0) {
                    JSONObject today = dailyArr.getJSONObject(0);
                    String sunrise = today.optString("sunrise", "");
                    String sunset = today.optString("sunset", "");

                    views.setTextViewText(R.id.text_sunrise, "↑ " + formatTime(sunrise));
                    views.setTextViewText(R.id.text_sunset, "↓ " + formatTime(sunset));

                    boolean isDay = true;
                    JSONObject currentObj = obj.optJSONObject("current");
                    if (currentObj != null) {
                        isDay = currentObj.optInt("isDay", 1) == 1;
                    }

                    views.setImageViewBitmap(R.id.image_sun_arc,
                        ChartRenderer.drawSunArc(sunrise, sunset, isDay, 300, 200));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating widget", e);
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static String formatTime(String isoTime) {
        try {
            if (isoTime.contains("T")) {
                String timePart = isoTime.split("T")[1];
                if (timePart.length() >= 5) {
                    return timePart.substring(0, 5);
                }
            }
            return isoTime;
        } catch (Exception e) {
            return "--:--";
        }
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, SunriseSunsetWidget.class));
        for (int id : ids) {
            updateWidget(context, manager, id);
        }
    }
}
