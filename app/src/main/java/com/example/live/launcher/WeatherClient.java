package com.example.live.launcher;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;

public final class WeatherClient {

    public interface Callback {
        void onResult(@NonNull WeatherResult result);
    }

    public static final class WeatherResult {
        public final boolean ok;
        public final String displayLine;

        WeatherResult(boolean ok, @NonNull String displayLine) {
            this.ok = ok;
            this.displayLine = displayLine;
        }
    }

    private static final String KEY_LAST_WEATHER_LINE = "last_weather_line";
    private static final String KEY_LAST_WEATHER_TS = "last_weather_ts";

    private static final String KEY_SAVED_LAT_BITS = "saved_weather_lat_bits";
    private static final String KEY_SAVED_LON_BITS = "saved_weather_lon_bits";
    private static final String KEY_SAVED_LABEL = "saved_weather_label";

    public static final class SavedLocation {
        public final double lat;
        public final double lon;
        @Nullable public final String label;

        SavedLocation(double lat, double lon, @Nullable String label) {
            this.lat = lat;
            this.lon = lon;
            this.label = label;
        }
    }

    public interface LocationSearchCallback {
        void onResult(@NonNull LocationSearchResult result);
    }

    public static final class LocationSearchResult {
        public final boolean ok;
        @Nullable public final SavedLocation location;
        @Nullable public final String errorMessage;

        LocationSearchResult(boolean ok, @Nullable SavedLocation location, @Nullable String errorMessage) {
            this.ok = ok;
            this.location = location;
            this.errorMessage = errorMessage;
        }
    }

    private WeatherClient() {}

    public static boolean hasSavedLocation(@NonNull Context context) {
        return getSavedLocation(context) != null;
    }

    public static void saveLocation(@NonNull Context context, @NonNull String label, double lat, double lon) {
        SecurePrefs.get(context).edit()
                .putLong(KEY_SAVED_LAT_BITS, Double.doubleToRawLongBits(lat))
                .putLong(KEY_SAVED_LON_BITS, Double.doubleToRawLongBits(lon))
                .putString(KEY_SAVED_LABEL, label)
                // force refresh
                .putLong(KEY_LAST_WEATHER_TS, 0L)
                .putString(KEY_LAST_WEATHER_LINE, "")
                .apply();
    }

    @Nullable
    public static SavedLocation getSavedLocation(@NonNull Context context) {
        long latBits = SecurePrefs.get(context).getLong(KEY_SAVED_LAT_BITS, Long.MIN_VALUE);
        long lonBits = SecurePrefs.get(context).getLong(KEY_SAVED_LON_BITS, Long.MIN_VALUE);
        if (latBits == Long.MIN_VALUE || lonBits == Long.MIN_VALUE) return null;
        double lat = Double.longBitsToDouble(latBits);
        double lon = Double.longBitsToDouble(lonBits);
        String label = SecurePrefs.get(context).getString(KEY_SAVED_LABEL, null);
        return new SavedLocation(lat, lon, label);
    }

    public static void refreshIfStale(@NonNull final Context context, long minIntervalMs, @NonNull final Callback cb) {
        final long now = System.currentTimeMillis();
        long lastTs = SecurePrefs.get(context).getLong(KEY_LAST_WEATHER_TS, 0L);
        String cached = SecurePrefs.get(context).getString(KEY_LAST_WEATHER_LINE, "--");

        if ((now - lastTs) < minIntervalMs && cached != null && !cached.trim().isEmpty()) {
            post(cb, new WeatherResult(true, cached));
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                WeatherResult out = fetch(context);
                if (out.ok) {
                    SecurePrefs.get(context).edit()
                            .putString(KEY_LAST_WEATHER_LINE, out.displayLine)
                            .putLong(KEY_LAST_WEATHER_TS, System.currentTimeMillis())
                            .apply();
                }
                post(cb, out);
            }
        }, "weather-refresh").start();
    }

    @NonNull
    private static WeatherResult fetch(@NonNull Context context) {
        SavedLocation saved = getSavedLocation(context);
        if (saved == null) return new WeatherResult(false, "Set location • --°C");

        Double tempC = fetchTempC(saved.lat, saved.lon);
        String label = (saved.label == null || saved.label.trim().isEmpty()) ? "Saved location" : saved.label.trim();

        if (tempC == null) return new WeatherResult(false, label + " • --°C");
        int rounded = (int) Math.round(tempC);
        return new WeatherResult(true, label + " • " + rounded + "°C");
    }

    public static void searchBestMatch(@NonNull final Context context, @NonNull final String query, @NonNull final LocationSearchCallback cb) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                LocationSearchResult out = geocodeBest(query);
                post(cb, out);
            }
        }, "geo-search").start();
    }

    @NonNull
    private static LocationSearchResult geocodeBest(@NonNull String query) {
        HttpURLConnection conn = null;
        try {
            String q = query.trim();
            if (q.isEmpty()) return new LocationSearchResult(false, null, "Enter a location");

            String lang = Locale.getDefault().getLanguage();
            String url = "https://geocoding-api.open-meteo.com/v1/search?name="
                    + URLEncoder.encode(q, "UTF-8")
                    + "&count=1&language=" + URLEncoder.encode(lang, "UTF-8")
                    + "&format=json";

            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(7000);
            conn.setReadTimeout(7000);
            conn.setRequestMethod("GET");

            int code = conn.getResponseCode();
            InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) return new LocationSearchResult(false, null, "Search failed");

            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            JSONObject root = new JSONObject(sb.toString());
            JSONArray results = root.optJSONArray("results");
            if (results == null || results.length() == 0) {
                return new LocationSearchResult(false, null, "No matches found");
            }

            JSONObject r = results.getJSONObject(0);
            double lat = r.getDouble("latitude");
            double lon = r.getDouble("longitude");
            String name = r.optString("name", "");
            String admin1 = r.optString("admin1", "");
            String country = r.optString("country", "");

            StringBuilder label = new StringBuilder();
            if (!name.isEmpty()) label.append(name);
            if (!admin1.isEmpty() && (label.length() == 0 || !admin1.equalsIgnoreCase(name))) {
                if (label.length() > 0) label.append(", ");
                label.append(admin1);
            }
            if (!country.isEmpty()) {
                if (label.length() > 0) label.append(", ");
                label.append(country);
            }

            String finalLabel = label.length() == 0 ? q : label.toString();
            return new LocationSearchResult(true, new SavedLocation(lat, lon, finalLabel), null);
        } catch (Throwable t) {
            return new LocationSearchResult(false, null, "Search failed");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    @Nullable
    private static Double fetchTempC(double lat, double lon) {
        HttpURLConnection conn = null;
        try {
            String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon + "&current=temperature_2m&temperature_unit=celsius";
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestMethod("GET");

            int code = conn.getResponseCode();
            InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) return null;
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            JSONObject root = new JSONObject(sb.toString());
            JSONObject current = root.optJSONObject("current");
            if (current == null) return null;
            if (!current.has("temperature_2m")) return null;
            return current.getDouble("temperature_2m");
        } catch (Throwable t) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static void post(@NonNull final Callback cb, @NonNull final WeatherResult result) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                cb.onResult(result);
            }
        });
    }

    private static void post(@NonNull final LocationSearchCallback cb, @NonNull final LocationSearchResult result) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                cb.onResult(result);
            }
        });
    }
}
