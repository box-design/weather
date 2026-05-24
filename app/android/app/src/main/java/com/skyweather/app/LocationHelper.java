package com.skyweather.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class LocationHelper {
    private static final String TAG = "LocationHelper";
    private static final long MIN_TIME = 0;
    private static final float MIN_DISTANCE = 0;

    public interface LocationCallback {
        void onLocationSuccess(double latitude, double longitude);
        void onLocationError(String error);
    }

    public static void getCurrentLocation(Context context, LocationCallback callback) {
        if (!hasLocationPermission(context)) {
            callback.onLocationError("无位置权限");
            return;
        }

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!gpsEnabled && !networkEnabled) {
            callback.onLocationError("位置服务未开启");
            return;
        }

        final LocationListener[] listener = new LocationListener[1];
        final boolean[] found = new boolean[1];
        found[0] = false;

        listener[0] = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (found[0]) return;
                found[0] = true;
                try {
                    locationManager.removeUpdates(this);
                } catch (SecurityException e) {
                    Log.e(TAG, "removeUpdates error", e);
                }
                callback.onLocationSuccess(location.getLatitude(), location.getLongitude());
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        try {
            String provider = gpsEnabled ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;
            locationManager.requestLocationUpdates(provider, MIN_TIME, MIN_DISTANCE, listener[0], Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "requestLocationUpdates error", e);
            callback.onLocationError("获取位置失败");
        }

        try {
            Location lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnown == null) {
                lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (lastKnown != null && !found[0]) {
                found[0] = true;
                try {
                    locationManager.removeUpdates(listener[0]);
                } catch (SecurityException e) {
                    Log.e(TAG, "removeUpdates error", e);
                }
                callback.onLocationSuccess(lastKnown.getLatitude(), lastKnown.getLongitude());
            }
        } catch (SecurityException e) {
            Log.e(TAG, "getLastKnownLocation error", e);
        }

        new android.os.Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!found[0]) {
                found[0] = true;
                try {
                    locationManager.removeUpdates(listener[0]);
                } catch (SecurityException e) {
                    Log.e(TAG, "removeUpdates error", e);
                }
                callback.onLocationError("定位超时");
            }
        }, 10000);
    }

    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}