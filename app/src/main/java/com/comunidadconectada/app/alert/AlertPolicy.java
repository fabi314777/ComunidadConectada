package com.comunidadconectada.app.alert;

import com.comunidadconectada.app.model.CommunityAlert;
import com.comunidadconectada.app.util.DuplicateDetector;

/** Decide si una alerta recibida por MQTT se procesa o se descarta. */
public final class AlertPolicy {
    private AlertPolicy() { }
    public enum Decision { ACCEPT, DUPLICATE, STALE, INVALID }

    public static Decision evaluate(CommunityAlert alert, String expectedCommunityId,
                                    DuplicateDetector detector, long now, long maxAgeMs) {
        if (alert == null || expectedCommunityId == null || !expectedCommunityId.equals(alert.getCommunityId()))
            return Decision.INVALID;
        // Una alarma antigua no reaparece como nueva (p. ej. tras reconectar).
        if (now - alert.getTimestamp() > maxAgeMs) return Decision.STALE;
        // Mismo identificador = misma alerta (QoS 1 puede repetir).
        if (!detector.markIfNew(alert.getId())) return Decision.DUPLICATE;
        return Decision.ACCEPT;
    }
}
