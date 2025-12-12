package com.example.live.launcher;

import android.content.Context;
import android.content.SharedPreferences;

public final class DashboardDataStore {

    private static final String KEY_EVENT = "dash_next_event";
    private static final String KEY_TEMP = "dash_temp";
    private static final String KEY_LAST_UPDATED = "dash_updated_at";

    private DashboardDataStore() {}

    public static void setNextEvent(Context context, String value) {
        SharedPreferences sp = SecurePrefs.get(context);
        sp.edit().putString(KEY_EVENT, value).apply();
    }

    public static void setTemperature(Context context, String value) {
        SharedPreferences sp = SecurePrefs.get(context);
        sp.edit().putString(KEY_TEMP, value).apply();
    }

    public static void setLastUpdatedAt(Context context, long epochMs) {
        SharedPreferences sp = SecurePrefs.get(context);
        sp.edit().putLong(KEY_LAST_UPDATED, epochMs).apply();
    }

    public static String getNextEvent(Context context) {
        return SecurePrefs.get(context).getString(KEY_EVENT, null);
    }

    public static String getTemperature(Context context) {
        return SecurePrefs.get(context).getString(KEY_TEMP, null);
    }

    public static long getLastUpdatedAt(Context context) {
        return SecurePrefs.get(context).getLong(KEY_LAST_UPDATED, 0L);
    }
}
