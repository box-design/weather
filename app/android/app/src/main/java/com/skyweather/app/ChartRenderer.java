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

        float chartLeft = 36f;
        float chartTop = 28f;
        float chartRight = width - 30f;
        float chartBottom = height - 24f;
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
        minTemp -= tempRange * 0.1;
        maxTemp += tempRange * 0.1;
        tempRange = maxTemp - minTemp;

        int lineColor = getChartColor(weatherCode);
        int lineColorAlpha = Color.argb(140, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor));

        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(lineColor);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2.5f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.WHITE);
        dotPaint.setStyle(Paint.Style.FILL);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.argb(160, 255, 255, 255));
        textPaint.setTextSize(11f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        Paint tempTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tempTextPaint.setColor(Color.WHITE);
        tempTextPaint.setTextSize(12f);
        tempTextPaint.setTextAlign(Paint.Align.CENTER);
        tempTextPaint.setShadowLayer(2, 0, 1, 0x66000000);

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

        int prevLabelDir = 0;
        for (int i = 0; i < n; i++) {
            canvas.drawCircle(pointsX[i], pointsY[i], 3f, dotPaint);

            String tempStr = (int) Math.round(temperatures.get(i)) + "\u00b0";

            int labelDir = 0;
            if (i > 0 && Math.abs(temperatures.get(i) - temperatures.get(i - 1)) < 0.3) {
                labelDir = -prevLabelDir;
                if (labelDir == 0) labelDir = (i % 2 == 0) ? 1 : -1;
            }
            prevLabelDir = labelDir;

            float labelY = pointsY[i] - 10f + labelDir * 18f;
            canvas.drawText(tempStr, pointsX[i], labelY, tempTextPaint);
        }

        if (times != null) {
            for (int i = 0; i < n; i++) {
                String timeLabel = formatTimeLabel(times.get(i));
                canvas.drawText(timeLabel, pointsX[i], chartBottom + 14f, textPaint);
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

        float chartLeft = 36f;
        float chartTop = 28f;
        float chartRight = width - 30f;
        float chartBottom = height - 24f;
        float chartWidth = chartRight - chartLeft;
        float chartHeight = chartBottom - chartTop;

        double maxPrecip = 0;
        for (double p : precipitations) {
            if (p > maxPrecip) maxPrecip = p;
        }
        if (maxPrecip < 0.1) maxPrecip = 0.1;

        int n = precipitations.size();
        float barWidth = chartWidth / n * 0.5f;
        float barSpacing = chartWidth / n;

        Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(Color.argb(140, 100, 180, 255));
        barPaint.setStyle(Paint.Style.FILL);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.argb(160, 255, 255, 255));
        textPaint.setTextSize(11f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        for (int i = 0; i < n; i++) {
            float barHeight = (float) (chartHeight * (precipitations.get(i) / maxPrecip));
            float left = chartLeft + i * barSpacing + (barSpacing - barWidth) / 2f;
            float top = chartBottom - barHeight;
            float right = left + barWidth;

            RectF barRect = new RectF(left, top, right, chartBottom);
            canvas.drawRoundRect(barRect, 3f, 3f, barPaint);

            if (precipitations.get(i) > 0.01) {
                String valStr = String.format("%.1f", precipitations.get(i));
                Paint valPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                valPaint.setColor(Color.WHITE);
                valPaint.setTextSize(12f);
                valPaint.setTextAlign(Paint.Align.CENTER);
                valPaint.setShadowLayer(2, 0, 1, 0x66000000);
                canvas.drawText(valStr, left + barWidth / 2f, top - 4f, valPaint);
            }

            if (times != null && i < times.size()) {
                String timeLabel = formatTimeLabel(times.get(i));
                canvas.drawText(timeLabel, left + barWidth / 2f, chartBottom + 14f, textPaint);
            }
        }

        if (probabilities != null && !probabilities.isEmpty()) {
            double maxProb = 0;
            for (double p : probabilities) {
                if (p > maxProb) maxProb = p;
            }
            Paint probPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            probPaint.setColor(Color.argb(180, 130, 200, 255));
            probPaint.setTextSize(12f);
            probPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("\u964d\u6c34\u6982\u7387 " + Math.round(maxProb) + "%", chartLeft, chartTop + 14f, probPaint);
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
            float cy = height * 0.72f;
            float radius = Math.min(width, height) * 0.35f;

            Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            arcPaint.setStyle(Paint.Style.STROKE);
            arcPaint.setStrokeCap(Paint.Cap.ROUND);

            int arcColor = isDay ? Color.argb(60, 255, 200, 50) : Color.argb(60, 100, 140, 200);
            int progressColor = isDay ? Color.argb(180, 255, 200, 50) : Color.argb(180, 100, 140, 200);

            arcPaint.setColor(arcColor);
            arcPaint.setStrokeWidth(2.5f);
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
                    Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    glowPaint.setColor(Color.argb(40, 255, 210, 60));
                    canvas.drawCircle(markerX, markerY, 16f, glowPaint);
                    markerPaint.setColor(Color.argb(255, 255, 210, 60));
                    canvas.drawCircle(markerX, markerY, 8f, markerPaint);
                } else {
                    markerPaint.setColor(Color.argb(200, 200, 210, 255));
                    canvas.drawCircle(markerX, markerY, 7f, markerPaint);
                    Paint moonShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
                    moonShadow.setColor(Color.argb(140, 30, 40, 80));
                    moonShadow.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(markerX + 3f, markerY - 2f, 5f, moonShadow);
                }
            } else if (now > sunsetMillis) {
                arcPaint.setColor(Color.argb(30, 100, 140, 200));
                arcPaint.setStrokeWidth(2f);
                canvas.drawArc(arcRect, 180, 180, false, arcPaint);
            }

            Paint horizonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            horizonPaint.setColor(Color.argb(30, 255, 255, 255));
            horizonPaint.setStrokeWidth(1f);
            canvas.drawLine(cx - radius - 12f, cy, cx + radius + 12f, cy, horizonPaint);

            Paint timeLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            timeLabelPaint.setColor(Color.argb(160, 255, 255, 255));
            timeLabelPaint.setTextSize(13f);
            timeLabelPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("\u2191 " + formatTime(sunrise), cx - radius - 4f, cy + 14f, timeLabelPaint);

            timeLabelPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("\u2193 " + formatTime(sunset), cx + radius + 4f, cy + 14f, timeLabelPaint);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    public static Bitmap drawAqiRing(
            double aqiValue,
            double pm2_5,
            int width,
            int height) {
        if (width <= 0 || height <= 0) {
            return Bitmap.createBitmap(Math.max(1, width), Math.max(1, height), Bitmap.Config.ARGB_8888);
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        float cx = width / 2f;
        float cy = height * 0.36f;
        float radius = Math.min(width, height) * 0.30f;
        float strokeWidth = 10f;

        int aqiColor = getAqiColor(aqiValue);
        String aqiLabel = getAqiLabel(aqiValue);

        Paint bgRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgRingPaint.setColor(Color.argb(25, 255, 255, 255));
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
        valuePaint.setTextSize(38f);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setShadowLayer(3, 0, 1, 0x44000000);
        String aqiStr = String.valueOf((int) Math.round(aqiValue));
        canvas.drawText(aqiStr, cx, cy + 6f, valuePaint);

        Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(aqiColor);
        labelPaint.setTextSize(18f);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(aqiLabel, cx, cy + 26f, labelPaint);

        Paint pmPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pmPaint.setColor(Color.argb(140, 255, 255, 255));
        pmPaint.setTextSize(13f);
        pmPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("PM2.5: " + String.valueOf((int) Math.round(pm2_5)), cx, height * 0.75f, pmPaint);

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
        if (aqi <= 20) return "\u4f18";
        if (aqi <= 40) return "\u826f";
        if (aqi <= 60) return "\u4e2d";
        if (aqi <= 80) return "\u5dee";
        if (aqi <= 100) return "\u5f88\u5dee";
        return "\u5371\u9669";
    }

    private static String formatTimeLabel(String isoTime) {
        try {
            if (isoTime.contains("T")) {
                String timePart = isoTime.split("T")[1];
                if (timePart.length() >= 5) {
                    String hh = timePart.substring(0, 2);
                    String mm = timePart.substring(3, 5);
                    int hour = Integer.parseInt(hh);
                    if (hour == 0) return "0:00";
                    return hour + ":" + mm;
                }
            }
            return isoTime;
        } catch (Exception e) {
            return isoTime;
        }
    }

    private static String formatTime(String isoTime) {
        try {
            if (isoTime.contains("T")) {
                String timePart = isoTime.split("T")[1];
                if (timePart.length() >= 5) {
                    return timePart.substring(0, 5);
                }
            }
            return "--:--";
        } catch (Exception e) {
            return "--:--";
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
