package com.example.live.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public final class SecurePrefs {

    private static final String PREFS_NAME = "launcher_secure_prefs";

    private SecurePrefs() {}

    public static SharedPreferences get(Context context) {
        Context appContext = context.getApplicationContext();
        try {
            MasterKey key = new MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    appContext,
                    PREFS_NAME,
                    key,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Throwable t) {
            // Fallback (rare): some devices/emulators can fail keystore. Still keep the app usable.
            return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
    }
}
