package com.example.live.launcher;

import android.content.Context;
import android.content.SharedPreferences;

public final class ImageWallpaperPrefs {

    private static final String KEY_IMAGE_URI = "image_uri";

    private ImageWallpaperPrefs() {}

    public static String getImageUri(Context context) {
        return SecurePrefs.get(context).getString(KEY_IMAGE_URI, null);
    }

    public static void setImageUri(Context context, String uri) {
        SharedPreferences sp = SecurePrefs.get(context);
        sp.edit().putString(KEY_IMAGE_URI, uri).apply();
    }
}
