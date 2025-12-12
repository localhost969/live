package com.example.live.launcher;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;

import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public final class DashboardDataRefresher {

    private DashboardDataRefresher() {}

    public static void refreshInBackground(final Context context) {
        final Context appContext = context.getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                refreshCalendar(appContext);
                refreshTemperature(appContext);
                DashboardDataStore.setLastUpdatedAt(appContext, System.currentTimeMillis());
            }
        }).start();
    }

    private static void refreshCalendar(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Minimal: query next event title starting now.
        long now = System.currentTimeMillis();
        Uri uri = Uri.parse("content://com.android.calendar/instances/when").buildUpon()
                .appendPath(Long.toString(now))
                .appendPath(Long.toString(now + 7L * 24L * 60L * 60L * 1000L))
                .build();

        String[] projection = new String[] { "title", "begin" };
        Cursor c = null;
        try {
            c = context.getContentResolver().query(uri, projection, null, null, "begin ASC");
            if (c != null && c.moveToFirst()) {
                String title = c.getString(0);
                if (title != null) {
                    title = title.trim();
                    if (!title.isEmpty()) {
                        DashboardDataStore.setNextEvent(context, title);
                    }
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) {
                try { c.close(); } catch (Throwable ignored) {}
            }
        }
    }

    private static void refreshTemperature(Context context) {
        WeatherClient.SavedLocation loc = WeatherClient.getSavedLocation(context);
        if (loc == null) return;

        try {
            String urlStr = String.format(Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current=temperature_2m",
                    loc.lat, loc.lon);

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestMethod("GET");

            int code = conn.getResponseCode();
            if (code != 200) return;

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            JSONObject root = new JSONObject(sb.toString());
            JSONObject current = root.optJSONObject("current");
            if (current == null) return;

            if (current.has("temperature_2m")) {
                double t = current.optDouble("temperature_2m");
                String val = String.format(Locale.getDefault(), "%.0f°", t);
                DashboardDataStore.setTemperature(context, val);
            }
        } catch (Throwable ignored) {
        }
    }
}
