package com.skyweather.app;

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

    public static Bitmap drawTemperatureLineChart(
            List<Double> temperatures,
            List<String> times,
            int width,
            int height,
            int weatherCode) {
        if (temperatures == null || temperatures.isEmpty() || width <= 0 || height <= 0) {
            return Bitmap.createBitmap(Math.max(1, width), Math.max(1, height), Bitmap.Config.ARGB_8888);
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        float chartLeft = 0;
        float chartTop = 16f;
        float chartRight = width - 8f;
        float chartBottom = height - 28f;
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

        int lineColor = getChartColor(weatherCode);
        int lineColorAlpha = Color.argb(180, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor));

        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(lineColor);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(3f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(lineColor);
        dotPaint.setStyle(Paint.Style.FILL);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.argb(180, 255, 255, 255));
        textPaint.setTextSize(18f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        Paint tempTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tempTextPaint.setColor(Color.WHITE);
        tempTextPaint.setTextSize(20f);
        tempTextPaint.setTextAlign(Paint.Align.CENTER);

        int n = temperatures.size();
        float[] pointsX = new float[n];
        float[] pointsY = new float[n];

        for (int i = 0; i < n; i++) {
            pointsX[i] = chartLeft + (chartWidth * i / Math.max(1, n - 1));
            pointsY[i] = (float) (chartBottom - chartHeight * ((temperatures.get(i) - minTemp) / tempRange));
        }

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

        Path fillPath = new Path(linePath);
        fillPath.lineTo(pointsX[n - 1], chartBottom);
        fillPath.lineTo(pointsX[0], chartBottom);
        fillPath.close();

        LinearGradient gradient = new LinearGradient(0, chartTop, 0, chartBottom,
                lineColorAlpha, Color.argb(0, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor)),
                Shader.TileMode.CLAMP);
        fillPaint.setShader(gradient);
        canvas.drawPath(fillPath, fillPaint);

        for (int i = 0; i < n; i++) {
            canvas.drawCircle(pointsX[i], pointsY[i], 4f, dotPaint);
            String tempStr = Math.round(temperatures.get(i)) + "°";
            tempTextPaint.setColor(Color.WHITE);
            canvas.drawText(tempStr, pointsX[i], pointsY[i] - 10f, tempTextPaint);
        }

        if (times != null) {
            for (int i = 0; i < n; i++) {
                String timeLabel = formatTimeLabel(times.get(i));
                canvas.drawText(timeLabel, pointsX[i], chartBottom + 20f, textPaint);
            }
        }

        return bitmap;
    }

    public static Bitmap drawPrecipitationBarChart(
            List<Double> precipitations,
            List<Double> probabilities,
            List<String> times,
            int width,
            int height) {
        if (precipitations == null || precipitations.isEmpty() || width <= 0 || height <= 0) {
            return Bitmap.createBitmap(Math.max(1, width), Math.max(1, height), Bitmap.Config.ARGB_8888);
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        float chartLeft = 0;
        float chartTop = 16f;
        float chartRight = width - 8f;
        float chartBottom = height - 28f;
        float chartWidth = chartRight - chartLeft;
        float chartHeight = chartBottom - chartTop;

        double maxPrecip = 0;
        for (double p : precipitations) {
            if (p > maxPrecip) maxPrecip = p;
        }
        if (maxPrecip < 0.1) maxPrecip = 0.1;

        int n = precipitations.size();
        float barWidth = chartWidth / n * 0.6f;
        float barSpacing = chartWidth / n;

        Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(Color.argb(180, 100, 180, 255));
        barPaint.setStyle(Paint.Style.FILL);

        Paint barStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barStrokePaint.setColor(Color.argb(220, 130, 200, 255));
        barStrokePaint.setStyle(Paint.Style.STROKE);
        barStrokePaint.setStrokeWidth(1.5f);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.argb(180, 255, 255, 255));
        textPaint.setTextSize(18f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setColor(Color.WHITE);
        valuePaint.setTextSize(20f);
        valuePaint.setTextAlign(Paint.Align.CENTER);

        for (int i = 0; i < n; i++) {
            float barHeight = (float) (chartHeight * (precipitations.get(i) / maxPrecip));
            float left = chartLeft + i * barSpacing + (barSpacing - barWidth) / 2f;
            float top = chartBottom - barHeight;
            float right = left + barWidth;

            RectF barRect = new RectF(left, top, right, chartBottom);
            canvas.drawRoundRect(barRect, 4f, 4f, barPaint);
            canvas.drawRoundRect(barRect, 4f, 4f, barStrokePaint);

            if (precipitations.get(i) > 0) {
                String valStr = String.format("%.1f", precipitations.get(i));
                canvas.drawText(valStr, left + barWidth / 2f, top - 6f, valuePaint);
            }

            if (times != null && i < times.size()) {
                String timeLabel = formatTimeLabel(times.get(i));
                canvas.drawText(timeLabel, left + barWidth / 2f, chartBottom + 20f, textPaint);
            }
        }

        if (probabilities != null && !probabilities.isEmpty()) {
            double maxProb = 0;
            for (double p : probabilities) {
                if (p > maxProb) maxProb = p;
            }
            Paint probPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            probPaint.setColor(Color.argb(200, 130, 200, 255));
            probPaint.setTextSize(18f);
            probPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("降水概率 " + Math.round(maxProb) + "%", chartLeft + 4f, chartTop + 2f, probPaint);
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
        float radius = Math.min(width, height) * 0.38f;
        float strokeWidth = 10f;

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
        valuePaint.setTextSize(48f);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        String aqiStr = String.valueOf((int) Math.round(aqiValue));
        canvas.drawText(aqiStr, cx, cy + 12f, valuePaint);

        Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(aqiColor);
        labelPaint.setTextSize(22f);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(aqiLabel, cx, cy + 36f, labelPaint);

        return bitmap;
    }

    private static int getChartColor(int weatherCode) {
        if (weatherCode == 0 || weatherCode == 1) return Color.parseColor("#FFD700");
        if (weatherCode == 2 || weatherCode == 3) return Color.parseColor("#A0AEC0");
        if (weatherCode >= 51 && weatherCode <= 67) return Color.parseColor("#4A90E2");
        if (weatherCode >= 71 && weatherCode <= 77) return Color.parseColor("#B0E0E6");
        if (weatherCode >= 80 && weatherCode <= 82) return Color.parseColor("#4A90E2");
        if (weatherCode >= 85 && weatherCode <= 86) return Color.parseColor("#B0E0E6");
        if (weatherCode >= 95) return Color.parseColor("#7C3AED");
        if (weatherCode == 45 || weatherCode == 48) return Color.parseColor("#9CA3AF");
        return Color.parseColor("#FFD700");
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
