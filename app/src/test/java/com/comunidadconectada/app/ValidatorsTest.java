package com.comunidadconectada.app;

import static org.junit.Assert.*;

import com.comunidadconectada.app.util.Validators;
import org.junit.Test;

/** UT-01..UT-07: validación del registro (campos vacíos, vivienda 311A, correo, clave, nombre, mensajes). */
public class ValidatorsTest {
    @Test public void camposVacios_devuelvenError() {
        assertNotNull(Validators.name(""));
        assertNotNull(Validators.name(null));
        assertNotNull(Validators.unit("   "));
        assertNotNull(Validators.email(""));
        assertNotNull(Validators.password(""));
    }
    @Test public void vivienda311A_seAcepta() {
        assertNull(Validators.unit("311A"));
        assertNull(Validators.unit("12"));
        assertNull(Validators.unit("b4"));
    }
    @Test public void vivienda_conCaracteresInvalidos_seRechaza() {
        assertNotNull(Validators.unit("31 1A"));
        assertNotNull(Validators.unit("311-A"));
        assertNotNull(Validators.unit("123456789"));
    }
    @Test public void normalizarVivienda_pasaAMayusculas() {
        assertEquals("311A", Validators.normalizeUnit(" 311a "));
    }
    @Test public void correoYClave() {
        assertNull(Validators.email("benja@correo.cl"));
        assertNotNull(Validators.email("benja@"));
        assertNull(Validators.password("123456"));
        assertNotNull(Validators.password("123"));
        assertNull(Validators.passwordConfirm("abc123", "abc123"));
        assertNotNull(Validators.passwordConfirm("abc123", "abc124"));
    }
    @Test public void nombre_valido_e_invalido() {
        assertNull(Validators.name("Profe Benja"));
        assertNull(Validators.name("María José"));
        assertNotNull(Validators.name("1"));
        assertNotNull(Validators.name("<script>"));
    }
    @Test public void mensajeYAnuncio() {
        assertNull(Validators.chatMessage("Hola vecinos"));
        assertNotNull(Validators.chatMessage("   "));
        assertNotNull(Validators.chatMessage(new String(new char[501]).replace('\0', 'a')));
        assertNull(Validators.announcementTitle("Corte de agua"));
        assertNotNull(Validators.announcementTitle("ab"));
        assertNotNull(Validators.announcementBody(""));
    }
}
