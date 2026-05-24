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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class PrecipitationSwitchService extends Service {
    private static final String TAG = "PrecipSwitchService";
    private static final long SWITCH_INTERVAL_MS = 10_000;

    private Handler handler;
    private boolean showingPrecip = false;
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
                    resetToTemperature();
                    stopSelf();
                    return;
                }
                showingPrecip = !showingPrecip;
                updateAllOverviewWidgets();
                handler.postDelayed(this, SWITCH_INTERVAL_MS);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler.removeCallbacks(switchRunnable);
        showingPrecip = false;
        handler.post(switchRunnable);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(switchRunnable);
        resetToTemperature();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
            for (int i = 0; i < hourlyArr.length() && count < 1; i++) {
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

    private void updateAllOverviewWidgets() {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        int[] ids = manager.getAppWidgetIds(new ComponentName(this, WeatherOverviewWidget.class));
        for (int id : ids) {
            WeatherOverviewWidget.updateWidget(this, manager, id, showingPrecip);
        }
    }

    private void resetToTemperature() {
        showingPrecip = false;
        updateAllOverviewWidgets();
    }

    public static void startIfNeeded(Context context) {
        Intent intent = new Intent(context, PrecipitationSwitchService.class);
        context.startService(intent);
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, PrecipitationSwitchService.class));
    }
}
