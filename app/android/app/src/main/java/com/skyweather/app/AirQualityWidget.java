package com.skyweather.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import org.json.JSONObject;

public class AirQualityWidget extends AppWidgetProvider {
    private static final String TAG = "AirQualityWidget";

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
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_air_quality);

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

        String cachedJson = WidgetPrefs.getCachedWidgetData(context);
        if (cachedJson != null) {
            try {
                JSONObject obj = new JSONObject(cachedJson);
                JSONObject aqObj = obj.optJSONObject("airQuality");
                if (aqObj != null) {
                    double euAqi = aqObj.optDouble("euAqi", 0);
                    views.setImageViewBitmap(R.id.image_aqi_ring,
                        ChartRenderer.drawAqiRing(euAqi, 300, 300));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating widget", e);
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, AirQualityWidget.class));
        for (int id : ids) {
            updateWidget(context, manager, id);
        }
    }
}
