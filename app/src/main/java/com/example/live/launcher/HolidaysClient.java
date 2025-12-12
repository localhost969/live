package com.example.live.launcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Legacy network holiday client.
 *
 * Network fetching is intentionally disabled. Holidays must come from the bundled JSON
 * source via {@link HolidaysRepository}.
 */
public final class HolidaysClient {

    public static final class Holiday {
        public final String dateIso; // yyyy-MM-dd
        public final String name;

        Holiday(@NonNull String dateIso, @NonNull String name) {
            this.dateIso = dateIso;
            this.name = name;
        }
    }

    private HolidaysClient() {}

    @NonNull
    public static String defaultCountryCode() {
        String cc = Locale.getDefault().getCountry();
        if (cc == null || cc.trim().isEmpty()) return "US";
        return cc.toUpperCase(Locale.ROOT);
    }

    @NonNull
    public static List<Holiday> getPublicHolidays(int year, @Nullable String countryCode) {
        throw new UnsupportedOperationException("Network holidays disabled. Use HolidaysRepository (local JSON) instead.");
    }
}
