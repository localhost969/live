package com.example.live.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordManager {

    private static final String KEY_SALT = "pwd_salt";
    private static final String KEY_HASH = "pwd_hash";
    private static final String KEY_ITERS = "pwd_iters";

    private static final int DEFAULT_ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;

    private PasswordManager() {}

    public static boolean isPasswordSet(Context context) {
        SharedPreferences sp = SecurePrefs.get(context);
        return sp.contains(KEY_SALT) && sp.contains(KEY_HASH);
    }

    public static void setPassword(Context context, String password) throws Exception {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);

        int iters = DEFAULT_ITERATIONS;
        byte[] hash = pbkdf2(password, salt, iters);

        SharedPreferences sp = SecurePrefs.get(context);
        sp.edit()
                .putString(KEY_SALT, b64(salt))
                .putString(KEY_HASH, b64(hash))
                .putInt(KEY_ITERS, iters)
                .apply();
    }

    public static boolean verify(Context context, String password) {
        SharedPreferences sp = SecurePrefs.get(context);
        String saltB64 = sp.getString(KEY_SALT, null);
        String hashB64 = sp.getString(KEY_HASH, null);
        int iters = sp.getInt(KEY_ITERS, DEFAULT_ITERATIONS);

        if (saltB64 == null || hashB64 == null) return false;

        try {
            byte[] salt = Base64.decode(saltB64, Base64.NO_WRAP);
            byte[] expected = Base64.decode(hashB64, Base64.NO_WRAP);
            byte[] actual = pbkdf2(password, salt, iters);
            return constantTimeEquals(expected, actual);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean meetsPolicy(String password) {
        if (password == null) return false;
        if (password.length() < 12) return false;

        boolean hasLetter = false;
        boolean hasDigit = false;
        boolean hasSymbol = false;

        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isLetter(c)) hasLetter = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSymbol = true;
        }

        return hasLetter && hasDigit && hasSymbol;
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BITS);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null) return false;
        if (a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= (a[i] ^ b[i]);
        }
        return diff == 0;
    }

    private static String b64(byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }
}
