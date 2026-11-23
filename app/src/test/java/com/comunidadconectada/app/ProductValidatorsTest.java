package com.comunidadconectada.app;

import static org.junit.Assert.*;

import com.comunidadconectada.app.util.Validators;
import org.junit.Test;

/** UT-19..UT-22: validación de campos de ventas. */
public class ProductValidatorsTest {
    @Test public void nombreDeProducto() {
        assertNull(Validators.productName("Bicicleta aro 26"));
        assertNotNull(Validators.productName(""));
        assertNotNull(Validators.productName("ab"));
    }
    @Test public void precio_validoEInvalido() {
        assertNull(Validators.price("15000"));
        assertNull(Validators.price("12,5"));
        assertNotNull(Validators.price(""));
        assertNotNull(Validators.price("0"));
        assertNotNull(Validators.price("-3"));
        assertNotNull(Validators.price("abc"));
        assertEquals(12.5, Validators.parsePrice("12,5"), 0.0001);
    }
    @Test public void categoria_debeSerDeLaLista() {
        assertNull(Validators.category("Hogar"));
        assertNotNull(Validators.category("Armas"));
        assertNotNull(Validators.category(null));
    }
    @Test public void descripcionLarga_seRechaza() {
        assertNull(Validators.productDescription(""));
        assertNotNull(Validators.productDescription(new String(new char[301]).replace('\0', 'x')));
    }
}
