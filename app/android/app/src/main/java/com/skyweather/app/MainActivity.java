package com.skyweather.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Sync cities from web localStorage to native after a short delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                WebView webView = getBridge().getWebView();
                WidgetSyncBridge.syncCitiesFromWeb(webView, this, new WidgetSyncBridge.SyncCallback() {
                    @Override
                    public void onSuccess(int count) {
                        Log.d(TAG, "Widget cities synced: " + count);
                    }

                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "Widget sync failed: " + error);
                    }
                });
            } catch (Exception e) {
                Log.w(TAG, "Could not sync widget cities: " + e.getMessage());
            }
        }, 2000);
    }
}
