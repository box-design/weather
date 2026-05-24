package com.skyweather.app;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

public class WidgetConfigureActivity extends Activity {
    private static final String TAG = "WidgetConfigureActivity";
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

        List<WidgetPrefs.SavedCity> cities = WidgetPrefs.getSavedCities(this);

        if (cities.isEmpty()) {
            WidgetPrefs.SavedCity defaultCity = new WidgetPrefs.SavedCity(
                    "default", "北京", 39.9042, 116.4074, "CN", "Asia/Shanghai", true);
            cities.add(defaultCity);
        }

        CityAdapter adapter = new CityAdapter(this, cities);
        ListView listView = findViewById(R.id.list_cities);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            WidgetPrefs.SavedCity selected = cities.get(position);
            WidgetPrefs.setWidgetCity(WidgetConfigureActivity.this, appWidgetId,
                    selected.name, selected.latitude, selected.longitude, selected.timezone);

            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_OK, resultValue);

            WidgetUpdateScheduler.triggerNow(WidgetConfigureActivity.this);

            finish();
        });
    }

    private static class CityAdapter extends ArrayAdapter<WidgetPrefs.SavedCity> {
        private final LayoutInflater inflater;

        CityAdapter(Context context, List<WidgetPrefs.SavedCity> cities) {
            super(context, 0, cities);
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.widget_configure_item, parent, false);
            }
            WidgetPrefs.SavedCity city = getItem(position);
            TextView nameView = convertView.findViewById(R.id.text_city_name);
            TextView countryView = convertView.findViewById(R.id.text_city_country);
            nameView.setText(city.name);
            String subtitle = city.country;
            if (city.isDefault) subtitle += " (默认)";
            countryView.setText(subtitle);
            return convertView;
        }
    }
}
