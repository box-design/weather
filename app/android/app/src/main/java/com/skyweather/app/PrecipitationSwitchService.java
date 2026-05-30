package com.skyweather.app;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

public class PrecipitationSwitchService extends Service {
    private static final String TAG = "PrecipSwitchService";
    private static final long SWITCH_INTERVAL_MS = 10_000;

    private Handler handler;
    private Runnable switchRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        switchRunnable = new Runnable() {
            @Override
            public void run() {
                if (!shouldSwitch()) {
                    Log.d(TAG, "No high precipitation, stopping switch service");
                    resetPages();
                    stopSelf();
                    return;
                }
                flipAllWidgets();
                handler.postDelayed(this, SWITCH_INTERVAL_MS);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler.removeCallbacks(switchRunnable);
        resetPages();
        handler.post(switchRunnable);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(switchRunnable);
        resetPages();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void flipAllWidgets() {
        Context context = this;
        AppWidgetManager manager = AppWidgetManager.getInstance(context);

        // Flip large widgets
        int[] largeIds = manager.getAppWidgetIds(new ComponentName(context, WeatherOverviewWidget.class));
        for (int id : largeIds) {
            int currentPage = WidgetPrefs.getWidgetPage(context, id, 0);
            int newPage = (currentPage + 1) % 2;
            WidgetPrefs.setWidgetPage(context, id, newPage);
            WeatherOverviewWidget.updateWidget(context, manager, id);
        }

        // Flip small widgets
        int[] smallIds = manager.getAppWidgetIds(new ComponentName(context, WeatherOverviewWidgetSmall.class));
        for (int id : smallIds) {
            int currentPage = WidgetPrefs.getWidgetPageSmall(context, id, 0);
            int newPage = (currentPage + 1) % 2;
            WidgetPrefs.setWidgetPageSmall(context, id, newPage);
            WeatherOverviewWidgetSmall.updateWidget(context, manager, id);
        }
    }

    private void resetPages() {
        Context context = this;
        AppWidgetManager manager = AppWidgetManager.getInstance(context);

        int[] largeIds = manager.getAppWidgetIds(new ComponentName(context, WeatherOverviewWidget.class));
        for (int id : largeIds) {
            WidgetPrefs.setWidgetPage(context, id, 0);
            WeatherOverviewWidget.updateWidget(context, manager, id);
        }

        int[] smallIds = manager.getAppWidgetIds(new ComponentName(context, WeatherOverviewWidgetSmall.class));
        for (int id : smallIds) {
            WidgetPrefs.setWidgetPageSmall(context, id, 0);
            WeatherOverviewWidgetSmall.updateWidget(context, manager, id);
        }
    }

    private boolean shouldSwitch() {
        String cachedJson = WidgetPrefs.getCachedWidgetData(this);
        if (cachedJson == null) return false;
        try {
            JSONObject obj = new JSONObject(cachedJson);
            JSONArray hourlyArr = obj.optJSONArray("hourly");
            if (hourlyArr == null) return false;

            Calendar now = Calendar.getInstance();
            int currentHour = now.get(Calendar.HOUR_OF_DAY);
            int count = 0;
            for (int i = 0; i < hourlyArr.length() && count < 2; i++) {
                JSONObject h = hourlyArr.getJSONObject(i);
                String time = h.optString("time", "");
                if (time.contains("T")) {
                    String hourStr = time.split("T")[1];
                    if (hourStr.length() >= 2) {
                        int h2 = Integer.parseInt(hourStr.substring(0, 2));
                        if (h2 >= currentHour) {
                            double prob = h.optDouble("precipitationProbability", 0);
                            if (prob > 50) return true;
                            count++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "shouldSwitch error", e);
        }
        return false;
    }

    public static void startIfNeeded(Context context) {
        Intent intent = new Intent(context, PrecipitationSwitchService.class);
        context.startService(intent);
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, PrecipitationSwitchService.class));
    }
}
