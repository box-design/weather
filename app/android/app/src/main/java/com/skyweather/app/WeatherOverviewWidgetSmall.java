package com.skyweather.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class WeatherOverviewWidgetSmall extends AppWidgetProvider {
    private static final String TAG = "WeatherOverviewWidgetSmall";

    public static final String ACTION_SWITCH_VIEW = "com.skyweather.app.ACTION_SWITCH_SMALL_VIEW";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            WidgetPrefs.removeWidgetCity(context, id);
        }
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (ACTION_SWITCH_VIEW.equals(action)) {
            int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            int direction = intent.getIntExtra("direction", 1);
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                int currentPage = WidgetPrefs.getWidgetPageSmall(context, widgetId, 0);
                int newPage = (currentPage + direction + 2) % 2;
                WidgetPrefs.setWidgetPageSmall(context, widgetId, newPage);
                updateWidget(context, AppWidgetManager.getInstance(context), widgetId);
            }
        }
    }

    public static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_weather_overview_small);
        boolean isDarkMode = isDarkMode(context);

        // Click anywhere on the widget to flip to the next card
        Intent flipIntent = new Intent(context, WeatherOverviewWidgetSmall.class);
        flipIntent.setAction(ACTION_SWITCH_VIEW);
        flipIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        flipIntent.putExtra("direction", 1);
        PendingIntent flipPending = PendingIntent.getBroadcast(
                context, appWidgetId * 200, flipIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, flipPending);

        int currentPage = WidgetPrefs.getWidgetPageSmall(context, appWidgetId, 0);
        views.setInt(R.id.view_flipper, "setDisplayedChild", currentPage);

        String cachedJson = WidgetPrefs.getCachedWidgetData(context);
        if (cachedJson != null) {
            try {
                WidgetDataModel model = parseCachedData(cachedJson);
                if (model != null && model.current != null) {
                    String cityName = WidgetPrefs.getWidgetCityName(context, appWidgetId);
                    String displayCity = cityName != null ? cityName : "当前位置";
                    String tempStr = Math.round(model.current.temperature) + "°";
                    String weatherDesc = getWeatherLabel(model.current.weatherCode);

                    views.setTextViewText(R.id.text_temperature, tempStr);
                    views.setTextViewText(R.id.text_weather_desc, weatherDesc);
                    views.setTextViewText(R.id.text_city, displayCity);

                    views.setTextViewText(R.id.text_temperature_precip, tempStr);
                    views.setTextViewText(R.id.text_weather_desc_precip, weatherDesc);
                    views.setTextViewText(R.id.text_city_precip, displayCity);

                    // Temperature chart (3 hours for small widget)
                    List<WidgetDataModel.HourlyItem> next3Hours = getNextHours(model, 3);
                    List<Double> temps = new ArrayList<>();
                    List<String> times = new ArrayList<>();
                    for (WidgetDataModel.HourlyItem item : next3Hours) {
                        temps.add(item.temperature);
                        times.add(item.time);
                    }

                    int chartWidth = 220;
                    int chartHeight = 130;
                    views.setImageViewBitmap(R.id.image_chart_temp,
                            ChartRenderer.drawTemperatureLineChart(temps, times, chartWidth, chartHeight,
                                    model.current.weatherCode, isDarkMode));

                    // Precipitation chart (1 hour for small widget)
                    List<WidgetDataModel.HourlyItem> next1Hour = getNextHours(model, 1);
                    List<Double> precips = new ArrayList<>();
                    List<Double> probs = new ArrayList<>();
                    List<String> pTimes = new ArrayList<>();
                    for (WidgetDataModel.HourlyItem item : next1Hour) {
                        precips.add(item.precipitation);
                        probs.add(item.precipitationProbability);
                        pTimes.add(item.time);
                    }

                    views.setImageViewBitmap(R.id.image_chart_precip,
                            ChartRenderer.drawPrecipitationBarChart(precips, probs, pTimes, chartWidth, chartHeight,
                                    isDarkMode));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating small widget", e);
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static boolean isDarkMode(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    private static WidgetDataModel parseCachedData(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            WidgetDataModel model = new WidgetDataModel();

            WidgetDataModel.CurrentWeather current = new WidgetDataModel.CurrentWeather();
            JSONObject currentObj = obj.getJSONObject("current");
            current.temperature = currentObj.optDouble("temperature", 0);
            current.weatherCode = currentObj.optInt("weatherCode", 0);
            current.isDay = currentObj.optInt("isDay", 1) == 1;
            current.humidity = currentObj.optDouble("humidity", 0);
            current.windSpeed = currentObj.optDouble("windSpeed", 0);
            current.apparentTemperature = currentObj.optDouble("apparentTemperature", 0);
            model.current = current;

            JSONArray hourlyArr = obj.optJSONArray("hourly");
            if (hourlyArr != null) {
                List<WidgetDataModel.HourlyItem> items = new ArrayList<>();
                for (int i = 0; i < hourlyArr.length(); i++) {
                    JSONObject h = hourlyArr.getJSONObject(i);
                    WidgetDataModel.HourlyItem item = new WidgetDataModel.HourlyItem();
                    item.time = h.optString("time", "");
                    item.temperature = h.optDouble("temperature", 0);
                    item.precipitationProbability = h.optDouble("precipitationProbability", 0);
                    item.precipitation = h.optDouble("precipitation", 0);
                    items.add(item);
                }
                model.hourly = items;
            }

            JSONArray dailyArr = obj.optJSONArray("daily");
            if (dailyArr != null) {
                List<WidgetDataModel.DailyItem> items = new ArrayList<>();
                for (int i = 0; i < dailyArr.length(); i++) {
                    JSONObject d = dailyArr.getJSONObject(i);
                    WidgetDataModel.DailyItem item = new WidgetDataModel.DailyItem();
                    item.date = d.optString("date", "");
                    item.sunrise = d.optString("sunrise", "");
                    item.sunset = d.optString("sunset", "");
                    item.tempMax = d.optDouble("tempMax", 0);
                    item.tempMin = d.optDouble("tempMin", 0);
                    item.weatherCode = d.optInt("weatherCode", 0);
                    items.add(item);
                }
                model.daily = items;
            }

            JSONObject aqObj = obj.optJSONObject("airQuality");
            if (aqObj != null) {
                WidgetDataModel.AirQuality aq = new WidgetDataModel.AirQuality();
                aq.euAqi = aqObj.optDouble("euAqi", 0);
                aq.usAqi = aqObj.optDouble("usAqi", 0);
                aq.pm2_5 = aqObj.optDouble("pm2_5", 0);
                aq.pm10 = aqObj.optDouble("pm10", 0);
                model.airQuality = aq;
            }

            model.timezone = obj.optString("timezone", "UTC");
            model.fetchTimestamp = obj.optLong("fetchTimestamp", 0);
            return model;
        } catch (Exception e) {
            Log.e("WeatherOverviewWidgetSmall", "parseCachedData error", e);
            return null;
        }
    }

    private static List<WidgetDataModel.HourlyItem> getNextHours(WidgetDataModel model, int count) {
        List<WidgetDataModel.HourlyItem> result = new ArrayList<>();
        if (model.hourly == null) return result;
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int startIdx = -1;
        for (int i = 0; i < model.hourly.size(); i++) {
            String time = model.hourly.get(i).time;
            if (time.contains("T")) {
                String hourStr = time.split("T")[1];
                if (hourStr.length() >= 2) {
                    int h = Integer.parseInt(hourStr.substring(0, 2));
                    if (h >= currentHour) {
                        startIdx = i;
                        break;
                    }
                }
            }
        }
        if (startIdx == -1) startIdx = 0;
        for (int i = startIdx; i < Math.min(startIdx + count, model.hourly.size()); i++) {
            result.add(model.hourly.get(i));
        }
        return result;
    }

    private static String getWeatherLabel(int code) {
        if (code == 0) return "晴";
        if (code == 1) return "大部晴朗";
        if (code == 2) return "多云";
        if (code == 3) return "阴天";
        if (code == 45 || code == 48) return "雾";
        if (code >= 51 && code <= 57) return "毛毛雨";
        if (code >= 61 && code <= 67) return "雨";
        if (code >= 71 && code <= 77) return "雪";
        if (code >= 80 && code <= 82) return "阵雨";
        if (code >= 85 && code <= 86) return "阵雪";
        if (code >= 95) return "雷暴";
        return "未知";
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, WeatherOverviewWidgetSmall.class));
        for (int id : ids) {
            updateWidget(context, manager, id);
        }
    }
}
