package com.example.live.launcher;

import android.content.Context;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Local (bundled) holiday source.
 *
 * Source of truth is the app asset: assets/holidays_2026.json
 * Only year 2026 is supported; all other years return an empty list.
 */
public final class HolidaysRepository {

    private static final String ASSET_2026 = "holidays_2026.json";
    private static volatile List<HolidaysClient.Holiday> CACHE_2026;

    private HolidaysRepository() {}

    @NonNull
    public static List<HolidaysClient.Holiday> getPublicHolidaysBlocking(
            @NonNull Context context,
            int year,
            @NonNull String countryCode
    ) {
        // countryCode intentionally ignored: holidays are locally defined for a single year.
        if (year != 2026) return Collections.emptyList();
        List<HolidaysClient.Holiday> cached = CACHE_2026;
        if (cached != null) return cached;

        synchronized (HolidaysRepository.class) {
            if (CACHE_2026 != null) return CACHE_2026;
            List<HolidaysClient.Holiday> loaded = loadFromAssets(context.getApplicationContext(), ASSET_2026);
            CACHE_2026 = loaded;
            return loaded;
        }
    }

    @NonNull
    private static List<HolidaysClient.Holiday> loadFromAssets(@NonNull Context context, @NonNull String assetName) {
        InputStream is = null;
        try {
            is = context.getAssets().open(assetName);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            JSONArray arr = new JSONArray(sb.toString());
            Set<String> seen = new HashSet<>();
            List<HolidaysClient.Holiday> out = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String date = o.optString("date", null);
                String name = o.optString("name", null);
                if (date == null || date.isEmpty()) continue;
                if (name == null || name.isEmpty()) name = "Holiday";
                String key = date + "|" + name;
                if (seen.contains(key)) continue;
                seen.add(key);
                out.add(new HolidaysClient.Holiday(date, name));
            }
            return out;
        } catch (Throwable t) {
            return Collections.emptyList();
        } finally {
            if (is != null) {
                try { is.close(); } catch (Throwable ignore) {}
            }
        }
    }
}
