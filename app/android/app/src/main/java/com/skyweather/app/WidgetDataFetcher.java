package com.skyweather.app;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class WidgetDataFetcher {
    private static final String TAG = "WidgetDataFetcher";
    private static final String FORECAST_BASE = "https://api.open-meteo.com/v1/forecast";
    private static final String AIR_QUALITY_BASE = "https://air-quality-api.open-meteo.com/v1/air-quality";

    public static WidgetDataModel fetchWeatherData(double latitude, double longitude) {
        try {
            String forecastUrl = FORECAST_BASE +
                "?latitude=" + latitude +
                "&longitude=" + longitude +
                "&hourly=temperature_2m,precipitation_probability,precipitation,weather_code,is_day" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset" +
                "&current=temperature_2m,weather_code,is_day,relative_humidity_2m,wind_speed_10m,apparent_temperature" +
                "&timezone=auto&forecast_days=2";

            String forecastJson = fetchJson(forecastUrl);
            if (forecastJson == null) return null;

            JSONObject data = new JSONObject(forecastJson);
            WidgetDataModel model = new WidgetDataModel();

            WidgetDataModel.CurrentWeather current = new WidgetDataModel.CurrentWeather();
            JSONObject currentObj = data.getJSONObject("current");
            current.temperature = currentObj.optDouble("temperature_2m", 0);
            current.weatherCode = currentObj.optInt("weather_code", 0);
            current.isDay = currentObj.optInt("is_day", 1) == 1;
            current.humidity = currentObj.optDouble("relative_humidity_2m", 0);
            current.windSpeed = currentObj.optDouble("wind_speed_10m", 0);
            current.apparentTemperature = currentObj.optDouble("apparent_temperature", 0);
            model.current = current;

            JSONObject hourlyObj = data.getJSONObject("hourly");
            JSONArray times = hourlyObj.getJSONArray("time");
            JSONArray temps = hourlyObj.getJSONArray("temperature_2m");
            JSONArray precipProb = hourlyObj.getJSONArray("precipitation_probability");
            JSONArray precip = hourlyObj.getJSONArray("precipitation");
            JSONArray weatherCodes = hourlyObj.getJSONArray("weather_code");
            JSONArray isDayArr = hourlyObj.getJSONArray("is_day");

            List<WidgetDataModel.HourlyItem> hourlyItems = new ArrayList<>();
            for (int i = 0; i < times.length(); i++) {
                WidgetDataModel.HourlyItem item = new WidgetDataModel.HourlyItem();
                item.time = times.getString(i);
                item.temperature = temps.optDouble(i, 0);
                item.precipitationProbability = precipProb.optDouble(i, 0);
                item.precipitation = precip.optDouble(i, 0);
                hourlyItems.add(item);
            }
            model.hourly = hourlyItems;

            JSONObject dailyObj = data.getJSONObject("daily");
            JSONArray dates = dailyObj.getJSONArray("time");
            JSONArray sunrises = dailyObj.getJSONArray("sunrise");
            JSONArray sunsets = dailyObj.getJSONArray("sunset");
            JSONArray tempMax = dailyObj.getJSONArray("temperature_2m_max");
            JSONArray tempMin = dailyObj.getJSONArray("temperature_2m_min");
            JSONArray dailyCodes = dailyObj.getJSONArray("weather_code");

            List<WidgetDataModel.DailyItem> dailyItems = new ArrayList<>();
            for (int i = 0; i < dates.length(); i++) {
                WidgetDataModel.DailyItem item = new WidgetDataModel.DailyItem();
                item.date = dates.getString(i);
                item.sunrise = sunrises.getString(i);
                item.sunset = sunsets.getString(i);
                item.tempMax = tempMax.optDouble(i, 0);
                item.tempMin = tempMin.optDouble(i, 0);
                item.weatherCode = dailyCodes.optInt(i, 0);
                dailyItems.add(item);
            }
            model.daily = dailyItems;

            model.timezone = data.optString("timezone", "UTC");

            try {
                String aqUrl = AIR_QUALITY_BASE +
                    "?latitude=" + latitude +
                    "&longitude=" + longitude +
                    "&hourly=european_aqi,us_aqi,pm2_5,pm10&timezone=auto";
                String aqJson = fetchJson(aqUrl);
                if (aqJson != null) {
                    JSONObject aqData = new JSONObject(aqJson);
                    JSONObject aqHourly = aqData.getJSONObject("hourly");
                    int currentHourIdx = Math.min(new java.util.Date().getHours(), aqHourly.getJSONArray("time").length() - 1);
                    WidgetDataModel.AirQuality aq = new WidgetDataModel.AirQuality();
                    aq.euAqi = aqHourly.getJSONArray("european_aqi").optDouble(currentHourIdx, 0);
                    aq.usAqi = aqHourly.getJSONArray("us_aqi").optDouble(currentHourIdx, 0);
                    aq.pm2_5 = aqHourly.getJSONArray("pm2_5").optDouble(currentHourIdx, 0);
                    aq.pm10 = aqHourly.getJSONArray("pm10").optDouble(currentHourIdx, 0);
                    model.airQuality = aq;
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to fetch air quality", e);
            }

            model.fetchTimestamp = System.currentTimeMillis();
            return model;
        } catch (Exception e) {
            Log.e(TAG, "Failed to fetch weather data", e);
            return null;
        }
    }

    private static String fetchJson(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            int code = conn.getResponseCode();
            if (code != 200) return null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "fetchJson error: " + urlStr, e);
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
