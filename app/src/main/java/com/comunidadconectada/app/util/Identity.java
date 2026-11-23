package com.comunidadconectada.app.util;

/** Identificación visible en mensajes y alertas: "Profe Benja · #311A". */
public final class Identity {
    private Identity() { }
    public static String format(String name, String unit) { return name + " · #" + unit; }
    public static String alertText(String name, String unit) {
        return name + " · Vivienda #" + unit + " ha solicitado ayuda.";
    }
}
