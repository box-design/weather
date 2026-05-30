package com.skyweather.app;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class WidgetUpdateWorker extends Worker {
    private static final String TAG = "WidgetUpdateWorker";

    public WidgetUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting widget data update");
        try {
            Context context = getApplicationContext();

            WidgetPrefs.SavedCity defaultCity = WidgetPrefs.getDefaultCity(context);
            if (defaultCity == null) {
                Log.w(TAG, "No default city configured, using default coordinates");
                defaultCity = new WidgetPrefs.SavedCity("default", "北京", 39.9042, 116.4074, "CN", "Asia/Shanghai", true);
            }

            WidgetDataModel model = WidgetDataFetcher.fetchWeatherData(defaultCity.latitude, defaultCity.longitude);
            if (model == null) {
                Log.w(TAG, "Failed to fetch weather data, keeping cached data");
                return Result.retry();
            }

            String cachedJson = modelToJson(model);
            WidgetPrefs.cacheWidgetData(context, cachedJson);

            WeatherOverviewWidget.updateAll(context);
            WeatherOverviewWidgetSmall.updateAll(context);
            SunriseSunsetWidget.updateAll(context);
            AirQualityWidget.updateAll(context);

            Log.d(TAG, "Widget data updated successfully");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error updating widget data", e);
            return Result.retry();
        }
    }

    private String modelToJson(WidgetDataModel model) {
        try {
            JSONObject root = new JSONObject();

            JSONObject current = new JSONObject();
            current.put("temperature", model.current.temperature);
            current.put("weatherCode", model.current.weatherCode);
            current.put("isDay", model.current.isDay ? 1 : 0);
            current.put("humidity", model.current.humidity);
            current.put("windSpeed", model.current.windSpeed);
            current.put("apparentTemperature", model.current.apparentTemperature);
            root.put("current", current);

            if (model.hourly != null) {
                JSONArray hourlyArr = new JSONArray();
                for (WidgetDataModel.HourlyItem item : model.hourly) {
                    JSONObject h = new JSONObject();
                    h.put("time", item.time);
                    h.put("temperature", item.temperature);
                    h.put("precipitationProbability", item.precipitationProbability);
                    h.put("precipitation", item.precipitation);
                    hourlyArr.put(h);
                }
                root.put("hourly", hourlyArr);
            }

            if (model.daily != null) {
                JSONArray dailyArr = new JSONArray();
                for (WidgetDataModel.DailyItem item : model.daily) {
                    JSONObject d = new JSONObject();
                    d.put("date", item.date);
                    d.put("sunrise", item.sunrise);
                    d.put("sunset", item.sunset);
                    d.put("tempMax", item.tempMax);
                    d.put("tempMin", item.tempMin);
                    d.put("weatherCode", item.weatherCode);
                    dailyArr.put(d);
                }
                root.put("daily", dailyArr);
            }

            if (model.airQuality != null) {
                JSONObject aq = new JSONObject();
                aq.put("euAqi", model.airQuality.euAqi);
                aq.put("usAqi", model.airQuality.usAqi);
                aq.put("pm2_5", model.airQuality.pm2_5);
                aq.put("pm10", model.airQuality.pm10);
                root.put("airQuality", aq);
            }

            root.put("timezone", model.timezone);
            root.put("fetchTimestamp", model.fetchTimestamp);

            return root.toString();
        } catch (Exception e) {
            Log.e(TAG, "modelToJson error", e);
            return "{}";
        }
    }
}
