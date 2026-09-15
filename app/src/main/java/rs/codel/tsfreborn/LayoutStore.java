package rs.codel.tsfreborn;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class LayoutStore {
    private static final String PREFS = "workspace_layout_v1";
    private static final String KEY_SHORTCUTS = "shortcuts";

    private final SharedPreferences prefs;

    public LayoutStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized List<ShortcutRecord> load() {
        ArrayList<ShortcutRecord> out = new ArrayList<>();
        String raw = prefs.getString(KEY_SHORTCUTS, "[]");
        try {
            JSONObject root = new JSONObject(raw);
            JSONArray a = root.optJSONArray("shortcuts");
            if (a == null) a = new JSONArray();
            for (int i = 0; i < a.length(); i++) out.add(ShortcutRecord.fromJson(a.getJSONObject(i)));
        } catch (Exception oldFormat) {
            // Accept the earliest v0.1 array-only format too.
            try {
                JSONArray a = new JSONArray(raw);
                for (int i = 0; i < a.length(); i++) out.add(ShortcutRecord.fromJson(a.getJSONObject(i)));
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    public synchronized void save(List<ShortcutRecord> records) {
        prefs.edit().putString(KEY_SHORTCUTS, serialize(records)).apply();
    }

    public synchronized String serialize(List<ShortcutRecord> records) {
        try {
            JSONObject root = new JSONObject();
            root.put("format", "TSF-Reborn-layout");
            root.put("version", 1);
            JSONArray a = new JSONArray();
            for (ShortcutRecord r : records) a.put(r.toJson());
            root.put("shortcuts", a);
            return root.toString(2);
        } catch (Exception e) {
            return "{\"format\":\"TSF-Reborn-layout\",\"version\":1,\"shortcuts\":[]}";
        }
    }

    public synchronized List<ShortcutRecord> importJson(String raw) throws Exception {
        JSONObject root = new JSONObject(raw);
        if (!"TSF-Reborn-layout".equals(root.optString("format"))) {
            throw new IllegalArgumentException("Not a TSF Reborn layout backup");
        }
        JSONArray a = root.getJSONArray("shortcuts");
        ArrayList<ShortcutRecord> out = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) out.add(ShortcutRecord.fromJson(a.getJSONObject(i)));
        save(out);
        return out;
    }
}
