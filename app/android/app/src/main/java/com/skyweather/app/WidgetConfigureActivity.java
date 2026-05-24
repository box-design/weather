package com.skyweather.app;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class WidgetConfigureActivity extends Activity {
    private static final String TAG = "WidgetConfigureActivity";
    private static final int PERMISSION_REQUEST_LOCATION = 1001;
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

        List<CityItem> items = new ArrayList<>();

        // 添加"使用当前位置"选项
        items.add(new CityItem(null, "📍 使用当前位置", null, 0, 0, "auto", false, true));

        List<WidgetPrefs.SavedCity> cities = WidgetPrefs.getSavedCities(this);
        if (cities.isEmpty()) {
            WidgetPrefs.SavedCity defaultCity = new WidgetPrefs.SavedCity(
                    "default", "北京", 39.9042, 116.4074, "CN", "Asia/Shanghai", true);
            cities.add(defaultCity);
        }

        for (WidgetPrefs.SavedCity city : cities) {
            items.add(new CityItem(city.id, city.name, city.country, city.latitude, city.longitude, city.timezone, city.isDefault, false));
        }

        CityAdapter adapter = new CityAdapter(this, items);
        ListView listView = findViewById(R.id.list_cities);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            CityItem selected = items.get(position);
            if (selected.isCurrentLocation) {
                requestCurrentLocation();
            } else {
                WidgetPrefs.setWidgetCity(WidgetConfigureActivity.this, appWidgetId,
                        selected.name, selected.latitude, selected.longitude, selected.timezone);

                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                setResult(RESULT_OK, resultValue);

                WidgetUpdateScheduler.triggerNow(WidgetConfigureActivity.this);
                finish();
            }
        });
    }

    private void requestCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_REQUEST_LOCATION);
        } else {
            fetchCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchCurrentLocation();
            } else {
                Toast.makeText(this, "需要位置权限才能获取当前位置", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchCurrentLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            Toast.makeText(this, "无法获取位置服务", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Location location = null;
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            }
            if (location == null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
            }

            if (location != null) {
                useLocation(location.getLatitude(), location.getLongitude());
            } else {
                Toast.makeText(this, "正在获取位置...", Toast.LENGTH_SHORT).show();
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, new android.location.LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            useLocation(location.getLatitude(), location.getLongitude());
                        }

                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {}

                        @Override
                        public void onProviderEnabled(String provider) {}

                        @Override
                        public void onProviderDisabled(String provider) {
                            runOnUiThread(() -> Toast.makeText(WidgetConfigureActivity.this, "请开启位置服务", Toast.LENGTH_SHORT).show());
                        }
                    }, Looper.getMainLooper());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting location", e);
            Toast.makeText(this, "获取位置失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void useLocation(double lat, double lon) {
        new Thread(() -> {
            String cityName = reverseGeocode(lat, lon);
            if (cityName == null || cityName.isEmpty()) {
                cityName = "当前位置";
            }

            final String finalName = cityName;
            runOnUiThread(() -> {
                WidgetPrefs.setWidgetCity(WidgetConfigureActivity.this, appWidgetId,
                        finalName, lat, lon, "auto");

                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                setResult(RESULT_OK, resultValue);

                WidgetUpdateScheduler.triggerNow(WidgetConfigureActivity.this);
                finish();
            });
        }).start();
    }

    private String reverseGeocode(double lat, double lon) {
        HttpURLConnection conn = null;
        try {
            URL apiUrl = new URL("https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=" + lat + "&longitude=" + lon + "&localityLanguage=zh");
            conn = (HttpURLConnection) apiUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                JSONObject obj = new JSONObject(sb.toString());
                String city = obj.optString("city", null);
                if (city == null || city.isEmpty()) {
                    city = obj.optString("locality", null);
                }
                if (city == null || city.isEmpty()) {
                    city = obj.optString("principalSubdivision", null);
                }
                return city;
            }
        } catch (Exception e) {
            Log.w(TAG, "Reverse geocode failed", e);
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private static class CityItem {
        String id;
        String name;
        String country;
        double latitude;
        double longitude;
        String timezone;
        boolean isDefault;
        boolean isCurrentLocation;

        CityItem(String id, String name, String country, double latitude, double longitude, String timezone, boolean isDefault, boolean isCurrentLocation) {
            this.id = id;
            this.name = name;
            this.country = country;
            this.latitude = latitude;
            this.longitude = longitude;
            this.timezone = timezone;
            this.isDefault = isDefault;
            this.isCurrentLocation = isCurrentLocation;
        }
    }

    private static class CityAdapter extends ArrayAdapter<CityItem> {
        private final LayoutInflater inflater;

        CityAdapter(Context context, List<CityItem> cities) {
            super(context, 0, cities);
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.widget_configure_item, parent, false);
            }
            CityItem city = getItem(position);
            TextView nameView = convertView.findViewById(R.id.text_city_name);
            TextView countryView = convertView.findViewById(R.id.text_city_country);
            nameView.setText(city.name);
            String subtitle;
            if (city.isCurrentLocation) {
                subtitle = "基于GPS定位";
            } else {
                subtitle = city.country != null ? city.country : "";
                if (city.isDefault) subtitle += " (默认)";
            }
            countryView.setText(subtitle);
            return convertView;
        }
    }
}
