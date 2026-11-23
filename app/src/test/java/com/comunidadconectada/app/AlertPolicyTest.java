package com.comunidadconectada.app;

import static org.junit.Assert.*;

import com.comunidadconectada.app.alert.AlertPolicy;
import com.comunidadconectada.app.model.CommunityAlert;
import com.comunidadconectada.app.util.DuplicateDetector;
import org.junit.Test;

/** UT-11..UT-14: política de recepción (duplicados, alertas antiguas, otra comunidad). */
public class AlertPolicyTest {
    private static final long NOW = 1_000_000L, MAX = 300_000L;
    private CommunityAlert alert(String id, String comm, long ts) {
        return new CommunityAlert(id, comm, "u1", "Ana", "12B", ts, "active");
    }
    @Test public void primeraRecepcion_seAcepta_laSegunda_esDuplicada() {
        DuplicateDetector d = new DuplicateDetector(10);
        assertEquals(AlertPolicy.Decision.ACCEPT, AlertPolicy.evaluate(alert("a", "c1", NOW), "c1", d, NOW, MAX));
        assertEquals(AlertPolicy.Decision.DUPLICATE, AlertPolicy.evaluate(alert("a", "c1", NOW), "c1", d, NOW, MAX));
    }
    @Test public void alertaAntigua_noReapareceComoNueva() {
        DuplicateDetector d = new DuplicateDetector(10);
        assertEquals(AlertPolicy.Decision.STALE, AlertPolicy.evaluate(alert("old", "c1", NOW - MAX - 1), "c1", d, NOW, MAX));
    }
    @Test public void otraComunidad_esInvalida() {
        DuplicateDetector d = new DuplicateDetector(10);
        assertEquals(AlertPolicy.Decision.INVALID, AlertPolicy.evaluate(alert("x", "OTRA", NOW), "c1", d, NOW, MAX));
        assertEquals(AlertPolicy.Decision.INVALID, AlertPolicy.evaluate(null, "c1", d, NOW, MAX));
    }
    @Test public void alertaDescartadaPorAntigua_noContaminaElDetector() {
        DuplicateDetector d = new DuplicateDetector(10);
        AlertPolicy.evaluate(alert("old", "c1", NOW - MAX - 1), "c1", d, NOW, MAX);
        assertEquals(0, d.size());
    }
}
