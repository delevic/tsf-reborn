package rs.codel.tsfreborn;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public final class ShortcutRecord {
    public String uid;
    public String component;
    public int page;
    public float x;
    public float y;

    public ShortcutRecord(String uid, String component, int page, float x, float y) {
        this.uid = uid;
        this.component = component;
        this.page = page;
        this.x = x;
        this.y = y;
    }

    public static ShortcutRecord create(String component, int page, float x, float y) {
        return new ShortcutRecord(UUID.randomUUID().toString(), component, page, x, y);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("uid", uid);
        o.put("component", component);
        o.put("page", page);
        o.put("x", x);
        o.put("y", y);
        return o;
    }

    public static ShortcutRecord fromJson(JSONObject o) throws JSONException {
        return new ShortcutRecord(
                o.getString("uid"),
                o.getString("component"),
                o.optInt("page", 1),
                (float) o.optDouble("x", 0.5),
                (float) o.optDouble("y", 0.45)
        );
    }
}
