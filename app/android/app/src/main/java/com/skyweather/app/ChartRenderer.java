package com.skyweather.app;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

import java.util.List;

public class ChartRenderer {

    public static boolean isDarkMode(android.content.Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    public static Bitmap drawTemperatureLineChart(
            List<Double> temperatures,
            List<String> times,
            int width,
            int height,
            int weatherCode,
            boolean isDarkMode) {
        if (temperatures == null || temperatures.isEmpty() || width <= 0 || height <= 0) {
            return Bitmap.createBitmap(Math.max(1, width), Math.max(1, height), Bitmap.Config.ARGB_8888);
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        int textPrimary = isDarkMode ? Color.parseColor("#FFFFFF") : Color.parseColor("#1C1C1E");
        int textSecondary = isDarkMode ? Color.parseColor("#A1A1A6") : Color.parseColor("#636366");

        float chartLeft = 4f;
        float chartTop = 22f;
        float chartRight = width - 4f;
        float chartBottom = height - 20f;
        float chartWidth = chartRight - chartLeft;
        float chartHeight = chartBottom - chartTop;

        double minTemp = Double.MAX_VALUE;
        double maxTemp = Double.MIN_VALUE;
        for (double t : temperatures) {
            if (t < minTemp) minTemp = t;
            if (t > maxTemp) maxTemp = t;
        }
        double tempRange = maxTemp - minTemp;
        if (tempRange < 1) {
            minTemp -= 0.5;
            maxTemp += 0.5;
            tempRange = maxTemp - minTemp;
        }

        int lineColor = getChartColor(weatherCode, isDarkMode);
        int lineColorAlpha = Color.argb(140, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor));

        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(lineColor);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2.8f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(lineColor);
        dotPaint.setStyle(Paint.Style.FILL);

        Paint dotStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotStrokePaint.setColor(isDarkMode ? Color.parseColor("#1C1C1E") : Color.WHITE);
        dotStrokePaint.setStyle(Paint.Style.STROKE);
        dotStrokePaint.setStrokeWidth(2f);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(textSecondary);
        textPaint.setTextSize(16f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        Paint tempTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tempTextPaint.setColor(textPrimary);
        tempTextPaint.setTextSize(18f);
        tempTextPaint.setTextAlign(Paint.Align.CENTER);

        int n = temperatures.size();
        float[] pointsX = new float[n];
        float[] pointsY = new float[n];

        for (int i = 0; i < n; i++) {
            pointsX[i] = chartLeft + (chartWidth * i / Math.max(1, n - 1));
            pointsY[i] = (float) (chartBottom - chartHeight * ((temperatures.get(i) - minTemp) / tempRange));
        }

        // Smooth cubic bezier curve
        Path linePath = new Path();
        linePath.moveTo(pointsX[0], pointsY[0]);
        for (int i = 0; i < n - 1; i++) {
            float ctrl1X = (pointsX[i] + pointsX[i + 1]) / 2f;
            float ctrl1Y = pointsY[i];
            float ctrl2X = (pointsX[i] + pointsX[i + 1]) / 2f;
            float ctrl2Y = pointsY[i + 1];
            linePath.cubicTo(ctrl1X, ctrl1Y, ctrl2X, ctrl2Y, pointsX[i + 1], pointsY[i + 1]);
        }

        canvas.drawPath(linePath, linePaint);

        // Gradient fill under curve
        Path fillPath = new Path(linePath);
        fillPath.lineTo(pointsX[n - 1], chartBottom);
        fillPath.lineTo(pointsX[0], chartBottom);
        fillPath.close();

        int fillStartColor = Color.argb(100, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor));
        int fillEndColor = Color.argb(5, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor));
        LinearGradient gradient = new LinearGradient(0, chartTop, 0, chartBottom,
                fillStartColor, fillEndColor, Shader.TileMode.CLAMP);
        fillPaint.setShader(gradient);
        canvas.drawPath(fillPath, fillPaint);

        // Dots and temp labels
        for (int i = 0; i < n; i++) {
            canvas.drawCircle(pointsX[i], pointsY[i], 5.5f, dotStrokePaint);
            canvas.drawCircle(pointsX[i], pointsY[i], 4f, dotPaint);
            String tempStr = Math.round(temperatures.get(i)) + "°";
            float tempTextY = pointsY[i] - 14f;
            if (tempTextY < 16f) {
                tempTextY = pointsY[i] + 26f;
            }
            canvas.drawText(tempStr, pointsX[i], tempTextY, tempTextPaint);
        }

        // Time labels
        if (times != null) {
            for (int i = 0; i < n; i++) {
                String timeLabel = formatTimeLabel(times.get(i));
                canvas.drawText(timeLabel, pointsX[i], chartBottom + 18f, textPaint);
            }
        }

        return bitmap;
    }

    public static Bitmap drawPrecipitationBarChart(
            List<Double> precipitations,
            List<Double> probabilities,
            List<String> times,
            int width,
            int height,
            boolean isDarkMode) {
        if (precipitations == null || precipitations.isEmpty() || width <= 0 || height <= 0) {
            return Bitmap.createBitmap(Math.max(1, width), Math.max(1, height), Bitmap.Config.ARGB_8888);
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        int textPrimary = isDarkMode ? Color.parseColor("#FFFFFF") : Color.parseColor("#1C1C1E");
        int textSecondary = isDarkMode ? Color.parseColor("#A1A1A6") : Color.parseColor("#636366");
        int barColor = isDarkMode ? Color.parseColor("#0A84FF") : Color.parseColor("#007AFF");
        int barColorLight = isDarkMode ? Color.parseColor("#5AC8FA") : Color.parseColor("#32ADE6");

        float chartLeft = 4f;
        float chartTop = 18f;
        float chartRight = width - 4f;
        float chartBottom = height - 22f;
        float chartWidth = chartRight - chartLeft;
        float chartHeight = chartBottom - chartTop;

        double maxPrecip = 0;
        for (double p : precipitations) {
            if (p > maxPrecip) maxPrecip = p;
        }
        if (maxPrecip < 0.1) maxPrecip = 0.1;

        int n = precipitations.size();
        float barWidth = chartWidth / n * 0.55f;
        float barSpacing = chartWidth / n;

        Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(barColor);
        barPaint.setStyle(Paint.Style.FILL);

        Paint barStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barStrokePaint.setColor(barColorLight);
        barStrokePaint.setStyle(Paint.Style.STROKE);
        barStrokePaint.setStrokeWidth(1.5f);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(textSecondary);
        textPaint.setTextSize(15f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setColor(textPrimary);
        valuePaint.setTextSize(17f);
        valuePaint.setTextAlign(Paint.Align.CENTER);

        for (int i = 0; i < n; i++) {
            float barHeight = (float) (chartHeight * (precipitations.get(i) / maxPrecip));
            float left = chartLeft + i * barSpacing + (barSpacing - barWidth) / 2f;
            float top = chartBottom - barHeight;
            float right = left + barWidth;

            // Rounded bar
            RectF barRect = new RectF(left, top, right, chartBottom);
            canvas.drawRoundRect(barRect, 6f, 6f, barPaint);
            canvas.drawRoundRect(barRect, 6f, 6f, barStrokePaint);

            if (precipitations.get(i) > 0) {
                String valStr = String.format("%.1f", precipitations.get(i));
                canvas.drawText(valStr, left + barWidth / 2f, top - 6f, valuePaint);
            }

            if (times != null && i < times.size()) {
                String timeLabel = formatTimeLabel(times.get(i));
                canvas.drawText(timeLabel, left + barWidth / 2f, chartBottom + 18f, textPaint);
            }
        }

        // Probability label at top
        if (probabilities != null && !probabilities.isEmpty()) {
            double maxProb = 0;
            for (double p : probabilities) {
                if (p > maxProb) maxProb = p;
            }
            Paint probPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            probPaint.setColor(barColorLight);
            probPaint.setTextSize(15f);
            probPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("降水概率 " + Math.round(maxProb) + "%", chartLeft + 2f, chartTop + 2f, probPaint);
        }

        return bitmap;
    }

    public static Bitmap drawSunArc(
            String sunrise,
            String sunset,
            boolean isDay,
            int width,
            int height) {
        if (width <= 0 || height <= 0) {
            return Bitmap.createBitmap(Math.max(1, width), Math.max(1, height), Bitmap.Config.ARGB_8888);
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        try {
            long sunriseMillis = parseIsoTime(sunrise);
            long sunsetMillis = parseIsoTime(sunset);
            long now = System.currentTimeMillis();
            long dayLength = sunsetMillis - sunriseMillis;

            float cx = width / 2f;
            float cy = height * 0.85f;
            float radius = Math.min(width, height) * 0.38f;

            Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            arcPaint.setStyle(Paint.Style.STROKE);
            arcPaint.setStrokeWidth(2.5f);
            arcPaint.setStrokeCap(Paint.Cap.ROUND);

            int arcColor = isDay ? Color.argb(120, 255, 200, 50) : Color.argb(120, 100, 140, 200);
            int progressColor = isDay ? Color.argb(220, 255, 200, 50) : Color.argb(220, 100, 140, 200);

            arcPaint.setColor(arcColor);
            RectF arcRect = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
            canvas.drawArc(arcRect, 180, 180, false, arcPaint);

            if (dayLength > 0 && now >= sunriseMillis && now <= sunsetMillis) {
                float progress = (float) (now - sunriseMillis) / dayLength;
                float sweepAngle = progress * 180;
                arcPaint.setColor(progressColor);
                arcPaint.setStrokeWidth(3.5f);
                canvas.drawArc(arcRect, 180, sweepAngle, false, arcPaint);

                float angle = (float) Math.toRadians(180 - progress * 180);
                float markerX = cx + radius * (float) Math.cos(angle);
                float markerY = cy + radius * (float) Math.sin(angle);

                Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                markerPaint.setStyle(Paint.Style.FILL);
                if (isDay) {
                    markerPaint.setColor(Color.argb(255, 255, 210, 60));
                    canvas.drawCircle(markerX, markerY, 8f, markerPaint);
                    Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    glowPaint.setColor(Color.argb(60, 255, 210, 60));
                    canvas.drawCircle(markerX, markerY, 14f, glowPaint);
                } else {
                    markerPaint.setColor(Color.argb(220, 180, 200, 255));
                    canvas.drawCircle(markerX, markerY, 7f, markerPaint);
                    Paint moonShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
                    moonShadow.setColor(Color.argb(180, 30, 40, 80));
                    moonShadow.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(markerX + 3f, markerY - 2f, 5f, moonShadow);
                }
            } else if (now > sunsetMillis) {
                arcPaint.setColor(Color.argb(60, 100, 140, 200));
                arcPaint.setStrokeWidth(2f);
                canvas.drawArc(arcRect, 180, 180, false, arcPaint);
            }

            Paint horizonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            horizonPaint.setColor(Color.argb(60, 255, 255, 255));
            horizonPaint.setStrokeWidth(1f);
            canvas.drawLine(cx - radius - 10f, cy, cx + radius + 10f, cy, horizonPaint);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    public static Bitmap drawAqiRing(
            double aqiValue,
            int width,
            int height) {
        if (width <= 0 || height <= 0) {
            return Bitmap.createBitmap(Math.max(1, width), Math.max(1, height), Bitmap.Config.ARGB_8888);
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        float cx = width / 2f;
        float cy = height / 2f;
        float radius = Math.min(width, height) * 0.32f;
        float strokeWidth = 8f;

        int aqiColor = getAqiColor(aqiValue);
        String aqiLabel = getAqiLabel(aqiValue);

        Paint bgRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgRingPaint.setColor(Color.argb(40, 255, 255, 255));
        bgRingPaint.setStyle(Paint.Style.STROKE);
        bgRingPaint.setStrokeWidth(strokeWidth);
        bgRingPaint.setStrokeCap(Paint.Cap.ROUND);
        RectF ringRect = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
        canvas.drawArc(ringRect, 135, 270, false, bgRingPaint);

        float progress = (float) Math.min(aqiValue / 150.0, 1.0);
        float sweepAngle = progress * 270;
        Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(aqiColor);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidth);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawArc(ringRect, 135, sweepAngle, false, progressPaint);

        Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setColor(Color.WHITE);
        valuePaint.setTextSize(40f);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        String aqiStr = String.valueOf((int) Math.round(aqiValue));
        canvas.drawText(aqiStr, cx, cy + 8f, valuePaint);

        Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(aqiColor);
        labelPaint.setTextSize(20f);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(aqiLabel, cx, cy + 30f, labelPaint);

        return bitmap;
    }

    private static int getChartColor(int weatherCode, boolean isDarkMode) {
        int color;
        if (weatherCode == 0 || weatherCode == 1) color = Color.parseColor("#FFB800");
        else if (weatherCode == 2 || weatherCode == 3) color = Color.parseColor("#8E8E93");
        else if (weatherCode >= 51 && weatherCode <= 67) color = Color.parseColor("#007AFF");
        else if (weatherCode >= 71 && weatherCode <= 77) color = Color.parseColor("#5AC8FA");
        else if (weatherCode >= 80 && weatherCode <= 82) color = Color.parseColor("#007AFF");
        else if (weatherCode >= 85 && weatherCode <= 86) color = Color.parseColor("#5AC8FA");
        else if (weatherCode >= 95) color = Color.parseColor("#AF52DE");
        else if (weatherCode == 45 || weatherCode == 48) color = Color.parseColor("#8E8E93");
        else color = Color.parseColor("#FFB800");

        if (isDarkMode) {
            // Slightly brighten colors for dark mode
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            hsv[2] = Math.min(1.0f, hsv[2] * 1.15f);
            return Color.HSVToColor(hsv);
        }
        return color;
    }

    private static int getAqiColor(double aqi) {
        if (aqi <= 20) return Color.parseColor("#10B981");
        if (aqi <= 40) return Color.parseColor("#34D399");
        if (aqi <= 60) return Color.parseColor("#FBBF24");
        if (aqi <= 80) return Color.parseColor("#F97316");
        if (aqi <= 100) return Color.parseColor("#EF4444");
        return Color.parseColor("#DC2626");
    }

    private static String getAqiLabel(double aqi) {
        if (aqi <= 20) return "优";
        if (aqi <= 40) return "良";
        if (aqi <= 60) return "中";
        if (aqi <= 80) return "差";
        if (aqi <= 100) return "很差";
        return "危险";
    }

    private static String formatTimeLabel(String isoTime) {
        try {
            if (isoTime.contains("T")) {
                String timePart = isoTime.split("T")[1];
                if (timePart.length() >= 5) {
                    return timePart.substring(0, 5);
                }
            }
            return isoTime;
        } catch (Exception e) {
            return isoTime;
        }
    }

    private static long parseIsoTime(String isoTime) {
        try {
            String cleaned = isoTime.replace("T", " ");
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
            return sdf.parse(cleaned).getTime();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }
}
