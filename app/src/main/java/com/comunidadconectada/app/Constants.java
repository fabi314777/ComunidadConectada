package com.comunidadconectada.app;

/** Constantes globales de la app (comunidad, tópicos, canales, límites). */
public final class Constants {
    private Constants() { }

    public static final String COMMUNITY_ID = BuildConfig.COMMUNITY_ID;

    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_VECINO = "vecino";

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_ATTENDED = "attended";
    public static final String STATUS_CLOSED = "closed";

    /** Tópico FCM al que se suscriben los residentes. */
    public static final String FCM_TOPIC = "anuncios_" + COMMUNITY_ID;

    public static final String CHANNEL_ANNOUNCEMENTS = "anuncios";
    public static final String CHANNEL_ALERTS = "alertas";
    public static final String CHANNEL_SERVICE = "servicio";

    public static final String EXTRA_ANNOUNCEMENT_ID = "announcement_id";
    public static final String EXTRA_TAB = "tab";
    public static final String TAB_ALERTS = "alerts";

    public static final long ALERT_COOLDOWN_MS = 30_000L;
    public static final long ALERT_MAX_AGE_MS = 5 * 60_000L;
    public static final int DEDUP_CAPACITY = 200;
    public static final int CHAT_PAGE_SIZE = 30;
    public static final int ALERT_HISTORY_LIMIT = 50;
    public static final int ANNOUNCEMENT_LIMIT = 50;

    /** Tópico MQTT de alertas: comunidades/{id}/alertas. */
    public static String topicAlerts(String communityId) {
        return "comunidades/" + communityId + "/alertas";
    }
}
