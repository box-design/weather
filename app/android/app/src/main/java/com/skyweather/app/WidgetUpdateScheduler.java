package com.skyweather.app;

import android.content.Context;
import android.util.Log;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class WidgetUpdateScheduler {
    private static final String TAG = "WidgetUpdateScheduler";
    private static final String WORK_NAME = "skyweather_widget_update";

    public static void schedule(Context context) {
        try {
            PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                    WidgetUpdateWorker.class,
                    30,
                    TimeUnit.MINUTES
            )
            .setConstraints(new androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build())
            .build();

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
            );
            Log.d(TAG, "Widget update scheduled every 30 minutes");
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule widget update", e);
        }
    }

    public static void cancel(Context context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
            Log.d(TAG, "Widget update cancelled");
        } catch (Exception e) {
            Log.e(TAG, "Failed to cancel widget update", e);
        }
    }

    public static void triggerNow(Context context) {
        try {
            androidx.work.OneTimeWorkRequest oneTimeRequest =
                    new androidx.work.OneTimeWorkRequest.Builder(WidgetUpdateWorker.class)
                    .setConstraints(new androidx.work.Constraints.Builder()
                            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                            .build())
                    .build();
            WorkManager.getInstance(context).enqueue(oneTimeRequest);
            Log.d(TAG, "Widget immediate update triggered");
        } catch (Exception e) {
            Log.e(TAG, "Failed to trigger immediate update", e);
        }
    }
}
