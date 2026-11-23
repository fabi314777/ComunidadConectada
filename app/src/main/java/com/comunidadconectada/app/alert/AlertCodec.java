package com.comunidadconectada.app.alert;

import com.comunidadconectada.app.model.CommunityAlert;
import org.json.JSONException;
import org.json.JSONObject;

/** Serializa/deserializa la alerta que viaja por MQTT (JSON UTF-8). */
public final class AlertCodec {
    private AlertCodec() { }
    private static final int MAX_FIELD = 64;

    public static String toJson(CommunityAlert a) {
        try {
            JSONObject o = new JSONObject();
            o.put("id", a.getId());
            o.put("communityId", a.getCommunityId());
            o.put("userId", a.getUserId());
            o.put("userName", a.getUserName());
            o.put("unit", a.getUnit());
            o.put("timestamp", a.getTimestamp());
            o.put("status", a.getStatus());
            return o.toString();
        } catch (JSONException e) { throw new IllegalStateException(e); }
    }

    /** @return la alerta, o null si el JSON es inválido o faltan datos mínimos. */
    public static CommunityAlert fromJson(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            JSONObject o = new JSONObject(json);
            String id = o.optString("id", ""), communityId = o.optString("communityId", "");
            String userId = o.optString("userId", ""), userName = o.optString("userName", "");
            String unit = o.optString("unit", ""), status = o.optString("status", "");
            long ts = o.optLong("timestamp", 0L);
            if (id.isEmpty() || communityId.isEmpty() || userId.isEmpty() || userName.isEmpty()
                    || unit.isEmpty() || ts <= 0 || status.isEmpty()) return null;
            if (id.length() > MAX_FIELD || userName.length() > MAX_FIELD || unit.length() > MAX_FIELD) return null;
            return new CommunityAlert(id, communityId, userId, userName, unit, ts, status);
        } catch (JSONException e) { return null; }
    }
}
