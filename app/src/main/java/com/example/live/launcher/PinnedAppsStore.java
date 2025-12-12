package com.example.live.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PinnedAppsStore {

    private static final String KEY_PINNED = "pinned_packages";

    // Reasonable defaults to reduce usage (may be missing on some devices).
    public static final List<String> DEFAULT_PACKAGES = Arrays.asList(
            "com.google.android.gm",      // Gmail
            "com.google.android.dialer",  // Calls (Pixel)
            "com.android.dialer",         // Calls (AOSP/OEM)
            "com.google.android.apps.messaging", // SMS (Google)
            "com.android.messaging"       // SMS (AOSP/OEM)
    );

    private PinnedAppsStore() {}

    public static Set<String> getPinned(Context context) {
        SharedPreferences sp = SecurePrefs.get(context);
        Set<String> set = sp.getStringSet(KEY_PINNED, null);
        if (set == null) {
            // First run: keep only the essentials, but allow device variance.
            return new HashSet<>(DEFAULT_PACKAGES);
        }
        return new HashSet<>(set);
    }

    public static void setPinned(Context context, Set<String> packages) {
        SharedPreferences sp = SecurePrefs.get(context);
        sp.edit().putStringSet(KEY_PINNED, new HashSet<>(packages)).apply();
    }

    public static List<String> getPinnedList(Context context) {
        return new ArrayList<>(getPinned(context));
    }
}
