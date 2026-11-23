package com.comunidadconectada.app;

import static org.junit.Assert.*;

import com.comunidadconectada.app.util.DuplicateDetector;
import org.junit.Test;

/** UT-08..UT-10: detección de duplicados (recepción repetida del mismo evento MQTT). */
public class DuplicateDetectorTest {
    @Test public void mismoId_soloSeAceptaUnaVez() {
        DuplicateDetector d = new DuplicateDetector(10);
        assertTrue(d.markIfNew("a1"));
        assertFalse(d.markIfNew("a1"));
        assertFalse(d.markIfNew("a1"));
        assertTrue(d.markIfNew("a2"));
    }
    @Test public void idNuloOVacio_sePerdona() {
        DuplicateDetector d = new DuplicateDetector(10);
        assertFalse(d.markIfNew(null));
        assertFalse(d.markIfNew(""));
    }
    @Test public void capacidadAcotada_noCreceSinLimite() {
        DuplicateDetector d = new DuplicateDetector(3);
        for (int i = 0; i < 100; i++) d.markIfNew("id" + i);
        assertEquals(3, d.size());
    }
}
