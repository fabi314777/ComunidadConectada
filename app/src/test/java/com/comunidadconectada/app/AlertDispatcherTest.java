package com.comunidadconectada.app;

import static org.junit.Assert.*;

import com.comunidadconectada.app.alert.*;
import com.comunidadconectada.app.model.CommunityAlert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/** UT-19..UT-23: activación de alarma, sin conexión, reintento y cooldown (dobles de prueba manuales). */
public class AlertDispatcherTest {

    static class FakeTransport implements AlertTransport {
        boolean connected;
        final List<CommunityAlert> published = new ArrayList<>();
        Callback pending;
        @Override public boolean isConnected() { return connected; }
        @Override public void publish(CommunityAlert alert, Callback cb) { published.add(alert); pending = cb; }
    }
    static class FakeStore implements AlertStore {
        final List<CommunityAlert> saved = new ArrayList<>();
        @Override public void save(CommunityAlert a) { saved.add(a); }
    }

    private FakeTransport transport;
    private FakeStore store;
    private final long[] now = {10_000L};
    private AlertDispatcher dispatcher;
    private List<SendState> states;
    private AlertDispatcher.Listener listener;

    @Before public void setUp() {
        transport = new FakeTransport();
        store = new FakeStore();
        dispatcher = new AlertDispatcher(transport, store, () -> now[0], 30_000L);
        states = new ArrayList<>();
        listener = (s, a, m) -> states.add(s);
    }
    private SendState last() { return states.get(states.size() - 1); }

    @Test public void sinConexion_noMuestraExitoFalso_ni_guarda() {
        transport.connected = false;
        dispatcher.trigger("u1", "Ana", "12B", "c1", listener);
        assertEquals(SendState.FAILED, last());
        assertFalse(states.contains(SendState.SENT));
        assertTrue(transport.published.isEmpty());
        assertTrue(store.saved.isEmpty());
    }
    @Test public void conConexion_publica_y_soloTrasConfirmarGuarda() {
        transport.connected = true;
        dispatcher.trigger("u1", "Ana", "12B", "c1", listener);
        assertEquals(1, transport.published.size());
        assertTrue(store.saved.isEmpty());          // aún sin confirmación del broker
        transport.pending.onSuccess();
        assertEquals(1, store.saved.size());
        assertEquals(SendState.SENT, last());
    }
    @Test public void falloDePublicacion_quedaFailed_y_noGuarda() {
        transport.connected = true;
        dispatcher.trigger("u1", "Ana", "12B", "c1", listener);
        transport.pending.onFailure("timeout");
        assertEquals(SendState.FAILED, last());
        assertTrue(store.saved.isEmpty());
    }
    @Test public void reintento_usaElMismoIdentificador() {
        transport.connected = false;
        dispatcher.trigger("u1", "Ana", "12B", "c1", listener);
        String id = dispatcher.getLastAlert().getId();
        transport.connected = true;
        dispatcher.retry(listener);
        assertEquals(1, transport.published.size());
        assertEquals(id, transport.published.get(0).getId());
    }
    @Test public void cooldown_bloqueaActivacionesRepetidas() {
        transport.connected = true;
        dispatcher.trigger("u1", "Ana", "12B", "c1", listener);
        now[0] += 5_000L;
        dispatcher.trigger("u1", "Ana", "12B", "c1", listener);
        assertEquals(SendState.BLOCKED, last());
        assertEquals(1, transport.published.size());
        now[0] += 31_000L;
        dispatcher.trigger("u1", "Ana", "12B", "c1", listener);
        assertEquals(2, transport.published.size());
    }
}
