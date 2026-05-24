package com.skyweather.app;

import java.util.List;

public class WidgetDataModel {
    public static class CurrentWeather {
        public double temperature;
        public int weatherCode;
        public boolean isDay;
        public double humidity;
        public double windSpeed;
        public double apparentTemperature;
    }

    public static class HourlyItem {
        public String time;
        public double temperature;
        public double precipitationProbability;
        public double precipitation;
    }

    public static class DailyItem {
        public String date;
        public String sunrise;
        public String sunset;
        public double tempMax;
        public double tempMin;
        public int weatherCode;
    }

    public static class AirQuality {
        public double euAqi;
        public double usAqi;
        public double pm2_5;
        public double pm10;
    }

    public CurrentWeather current;
    public List<HourlyItem> hourly;
    public List<DailyItem> daily;
    public AirQuality airQuality;
    public String timezone;
    public long fetchTimestamp;
}
