package com.example.live.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class QuickContactsStore {

    private static final String PREFIX_NAME = "qc_name_";
    private static final String PREFIX_NUMBER = "qc_number_";
    private static final String PREFIX_POSITION = "qc_pos_";
    private static final String KEY_IDS = "qc_ids";

    private QuickContactsStore() {}

    public static List<QuickContact> getContacts(Context context) {
        SharedPreferences sp = SecurePrefs.get(context);
        String idsStr = sp.getString(KEY_IDS, "");
        
        List<QuickContact> contacts = new ArrayList<>();
        if (idsStr.isEmpty()) {
            return contacts;
        }

        String[] ids = idsStr.split(",");
        for (String id : ids) {
            id = id.trim();
            if (id.isEmpty()) continue;
            
            String name = sp.getString(PREFIX_NAME + id, "");
            String number = sp.getString(PREFIX_NUMBER + id, "");
            int position = sp.getInt(PREFIX_POSITION + id, 0);
            
            if (!name.isEmpty() && !number.isEmpty()) {
                contacts.add(new QuickContact(id, name, number, position));
            }
        }

        // Sort by position
        contacts.sort((a, b) -> Integer.compare(a.position, b.position));
        return contacts;
    }

    public static void addContact(Context context, String name, String number) {
        String id = UUID.randomUUID().toString();
        SharedPreferences sp = SecurePrefs.get(context);
        
        List<QuickContact> existing = getContacts(context);
        int nextPosition = existing.size();
        
        sp.edit()
                .putString(PREFIX_NAME + id, name)
                .putString(PREFIX_NUMBER + id, number)
                .putInt(PREFIX_POSITION + id, nextPosition)
                .putString(KEY_IDS, appendId(sp.getString(KEY_IDS, ""), id))
                .apply();
    }

    public static void removeContact(Context context, String id) {
        SharedPreferences sp = SecurePrefs.get(context);
        sp.edit()
                .remove(PREFIX_NAME + id)
                .remove(PREFIX_NUMBER + id)
                .remove(PREFIX_POSITION + id)
                .putString(KEY_IDS, removeId(sp.getString(KEY_IDS, ""), id))
                .apply();
    }

    public static void updatePositions(Context context, List<QuickContact> contacts) {
        SharedPreferences.Editor ed = SecurePrefs.get(context).edit();
        for (int i = 0; i < contacts.size(); i++) {
            QuickContact c = contacts.get(i);
            ed.putInt(PREFIX_POSITION + c.id, i);
        }
        ed.apply();
    }

    public static void clear(Context context) {
        SharedPreferences sp = SecurePrefs.get(context);
        String idsStr = sp.getString(KEY_IDS, "");
        if (!idsStr.isEmpty()) {
            SharedPreferences.Editor ed = sp.edit();
            String[] ids = idsStr.split(",");
            for (String id : ids) {
                id = id.trim();
                if (!id.isEmpty()) {
                    ed.remove(PREFIX_NAME + id)
                      .remove(PREFIX_NUMBER + id)
                      .remove(PREFIX_POSITION + id);
                }
            }
            ed.putString(KEY_IDS, "").apply();
        }
    }

    private static String appendId(String idsStr, String id) {
        if (idsStr.isEmpty()) return id;
        return idsStr + "," + id;
    }

    private static String removeId(String idsStr, String id) {
        String[] ids = idsStr.split(",");
        StringBuilder sb = new StringBuilder();
        for (String existingId : ids) {
            existingId = existingId.trim();
            if (!existingId.isEmpty() && !existingId.equals(id)) {
                if (sb.length() > 0) sb.append(",");
                sb.append(existingId);
            }
        }
        return sb.toString();
    }
}
