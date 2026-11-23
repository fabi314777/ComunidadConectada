package com.comunidadconectada.app.alert;

import com.comunidadconectada.app.model.CommunityAlert;
import com.comunidadconectada.app.util.Clock;

/**
 * Orquesta la activación de una alarma: 1) cooldown anti-repetición, 2) alerta con ID único,
 * 3) publica por MQTT, 4) solo si MQTT confirma guarda el registro en Realtime Database.
 * Nunca informa éxito si el mensaje no salió: sin conexión queda FAILED y se puede reintentar.
 */
public class AlertDispatcher {

    public interface Listener { void onState(SendState state, CommunityAlert alert, String message); }

    private final AlertTransport transport;
    private final AlertStore store;
    private final Clock clock;
    private final long cooldownMs;
    private long lastTriggerAt = -1;
    private CommunityAlert lastAlert;

    public AlertDispatcher(AlertTransport transport, AlertStore store, Clock clock, long cooldownMs) {
        this.transport = transport; this.store = store; this.clock = clock; this.cooldownMs = cooldownMs;
    }

    public synchronized void trigger(String uid, String name, String unit, String communityId, Listener l) {
        long now = clock.now();
        if (lastTriggerAt >= 0 && now - lastTriggerAt < cooldownMs) {
            long wait = (cooldownMs - (now - lastTriggerAt) + 999) / 1000;
            l.onState(SendState.BLOCKED, lastAlert, "Ya enviaste una alerta. Espera " + wait + " s para enviar otra.");
            return;
        }
        CommunityAlert alert = AlertFactory.create(uid, name, unit, communityId, now);
        lastAlert = alert;
        lastTriggerAt = now;
        send(alert, l);
    }

    /** Reintenta la última alerta fallida con el MISMO identificador (no genera duplicados). */
    public synchronized void retry(Listener l) {
        if (lastAlert == null) return;
        send(lastAlert, l);
    }

    private void send(final CommunityAlert alert, final Listener l) {
        l.onState(SendState.SENDING, alert, "Enviando alerta…");
        if (!transport.isConnected()) {
            fail(alert, l, "Sin conexión con el servidor de alertas. La alerta NO fue enviada.");
            return;
        }
        transport.publish(alert, new AlertTransport.Callback() {
            @Override public void onSuccess() {
                store.save(alert);
                l.onState(SendState.SENT, alert, "Alerta enviada a los vecinos conectados.");
            }
            @Override public void onFailure(String reason) {
                fail(alert, l, "No se pudo enviar la alerta (" + reason + "). Reintenta.");
            }
        });
    }

    private synchronized void fail(CommunityAlert alert, Listener l, String msg) {
        lastTriggerAt = -1; // permite reintentar de inmediato
        l.onState(SendState.FAILED, alert, msg);
    }

    public synchronized CommunityAlert getLastAlert() { return lastAlert; }
}
