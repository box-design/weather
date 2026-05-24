package com.skyweather.app;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class WidgetConfigureActivity extends Activity {
    private static final String TAG = "WidgetConfigureActivity";
    private static final int ITEM_CURRENT_LOCATION = 0;
    private static final int ITEM_CITY_START = 1;
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.widget_configure);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        List<WidgetItem> items = new ArrayList<>();
        items.add(new WidgetItem(ITEM_CURRENT_LOCATION, "当前位置", "使用 GPS 获取当前位置", "", false, true));

        List<WidgetPrefs.SavedCity> savedCities = WidgetPrefs.getSavedCities(this);
        for (WidgetPrefs.SavedCity city : savedCities) {
            items.add(new WidgetItem(ITEM_CITY_START, city.name, city.country, city.id, city.isDefault, false));
        }

        if (savedCities.isEmpty()) {
            items.add(new WidgetItem(ITEM_CITY_START, "北京", "中国", "beijing", true, false));
        }

        CityAdapter adapter = new CityAdapter(this, items);
        ListView listView = findViewById(R.id.list_cities);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            WidgetItem item = items.get(position);
            if (item.isCurrentLocation) {
                selectCurrentLocation();
            } else {
                selectCity(item);
            }
        });
    }

    private void selectCurrentLocation() {
        if (!LocationHelper.hasLocationPermission(this)) {
            Toast.makeText(this, "请先在应用设置中授予位置权限", Toast.LENGTH_LONG).show();
            WidgetPrefs.SavedCity fallback = new WidgetPrefs.SavedCity(
                    "default", "北京", 39.9042, 116.4074, "CN", "Asia/Shanghai", true);
            WidgetPrefs.setWidgetCity(this, appWidgetId, fallback.name, fallback.latitude, fallback.longitude, fallback.timezone);
            finishWithResult();
            return;
        }

        Toast.makeText(this, "正在获取位置...", Toast.LENGTH_SHORT).show();

        LocationHelper.getCurrentLocation(this, new LocationHelper.LocationCallback() {
            @Override
            public void onLocationSuccess(double latitude, double longitude) {
                runOnUiThread(() -> {
                    String name = "当前位置";
                    WidgetPrefs.setWidgetCity(WidgetConfigureActivity.this, appWidgetId, name, latitude, longitude, "auto");
                    Toast.makeText(WidgetConfigureActivity.this, "已定位: " + name, Toast.LENGTH_SHORT).show();
                    finishWithResult();
                });
            }

            @Override
            public void onLocationError(String error) {
                runOnUiThread(() -> {
                    Log.w(TAG, "Location error: " + error);
                    Toast.makeText(WidgetConfigureActivity.this, "定位失败: " + error + "，使用默认城市", Toast.LENGTH_LONG).show();
                    WidgetPrefs.SavedCity fallback = new WidgetPrefs.SavedCity(
                            "default", "北京", 39.9042, 116.4074, "CN", "Asia/Shanghai", true);
                    WidgetPrefs.setWidgetCity(WidgetConfigureActivity.this, appWidgetId, fallback.name, fallback.latitude, fallback.longitude, fallback.timezone);
                    finishWithResult();
                });
            }
        });
    }

    private void selectCity(WidgetItem item) {
        WidgetPrefs.setWidgetCity(this, appWidgetId, item.name,
                parseSavedCityLatitude(item), parseSavedCityLongitude(item), "auto");
        finishWithResult();
    }

    private double parseSavedCityLatitude(WidgetItem item) {
        List<WidgetPrefs.SavedCity> cities = WidgetPrefs.getSavedCities(this);
        for (WidgetPrefs.SavedCity city : cities) {
            if (city.name.equals(item.name) && city.id.equals(item.savedCityId)) {
                return city.latitude;
            }
        }
        if (item.name.equals("北京")) return 39.9042;
        return 39.9042;
    }

    private double parseSavedCityLongitude(WidgetItem item) {
        List<WidgetPrefs.SavedCity> cities = WidgetPrefs.getSavedCities(this);
        for (WidgetPrefs.SavedCity city : cities) {
            if (city.name.equals(item.name) && city.id.equals(item.savedCityId)) {
                return city.longitude;
            }
        }
        if (item.name.equals("北京")) return 116.4074;
        return 116.4074;
    }

    private void finishWithResult() {
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        WidgetUpdateScheduler.triggerNow(WidgetConfigureActivity.this);
        finish();
    }

    static class WidgetItem {
        final int type;
        final String name;
        final String subtitle;
        final String savedCityId;
        final boolean isDefault;
        final boolean isCurrentLocation;

        WidgetItem(int type, String name, String subtitle, String savedCityId, boolean isDefault, boolean isCurrentLocation) {
            this.type = type;
            this.name = name;
            this.subtitle = subtitle;
            this.savedCityId = savedCityId;
            this.isDefault = isDefault;
            this.isCurrentLocation = isCurrentLocation;
        }
    }

    private static class CityAdapter extends ArrayAdapter<WidgetItem> {
        private final LayoutInflater inflater;

        CityAdapter(Context context, List<WidgetItem> items) {
            super(context, 0, items);
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.widget_configure_item, parent, false);
            }
            WidgetItem item = getItem(position);
            TextView nameView = convertView.findViewById(R.id.text_city_name);
            TextView countryView = convertView.findViewById(R.id.text_city_country);

            if (item.isCurrentLocation) {
                nameView.setText("\uD83D\uDCCD " + item.name);
            } else {
                nameView.setText(item.name);
            }

            String subtitle = item.subtitle;
            if (item.isDefault) subtitle += " (默认)";
            countryView.setText(subtitle);

            return convertView;
        }
    }
}