package com.comunidadconectada.app;

import static org.junit.Assert.*;

import com.comunidadconectada.app.alert.AlertCodec;
import com.comunidadconectada.app.alert.AlertFactory;
import com.comunidadconectada.app.model.CommunityAlert;
import org.junit.Test;

/** UT-10..UT-14: generación y (de)serialización de alertas. */
public class AlertCodecTest {
    @Test public void generaIdUnico() {
        CommunityAlert a = AlertFactory.create("u1", "Profe Benja", "311A", "comunidad01", 1000L);
        CommunityAlert b = AlertFactory.create("u1", "Profe Benja", "311A", "comunidad01", 1000L);
        assertNotEquals(a.getId(), b.getId());
        assertEquals("active", a.getStatus());
    }
    @Test public void ida_y_vuelta_conservaIdentidadYVivienda() {
        CommunityAlert a = AlertFactory.create("u1", "Profe Benja", "311A", "comunidad01", 1234567L);
        CommunityAlert r = AlertCodec.fromJson(AlertCodec.toJson(a));
        assertNotNull(r);
        assertEquals(a.getId(), r.getId());
        assertEquals("Profe Benja · #311A", r.identity());
        assertEquals("Profe Benja · Vivienda #311A ha solicitado ayuda.", r.message());
    }
    @Test public void jsonInvalido_devuelveNull() {
        assertNull(AlertCodec.fromJson("no es json"));
        assertNull(AlertCodec.fromJson(""));
        assertNull(AlertCodec.fromJson(null));
    }
    @Test public void faltanCamposMinimos_devuelveNull() {
        assertNull(AlertCodec.fromJson("{\"id\":\"x\"}"));
        assertNull(AlertCodec.fromJson("{\"id\":\"x\",\"communityId\":\"c\",\"userId\":\"u\",\"userName\":\"n\",\"unit\":\"1\",\"timestamp\":0,\"status\":\"active\"}"));
    }
}
