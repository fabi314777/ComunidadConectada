package com.comunidadconectada.app.util;

import java.util.Locale;
import java.util.regex.Pattern;

/** Validaciones de formularios: devuelven el mensaje de error o null si es válido. Java puro (probable con JUnit). */
public final class Validators {
    private Validators() { }

    private static final Pattern NAME = Pattern.compile("^\\p{L}[\\p{L} .'\\-]{1,39}$");
    private static final Pattern UNIT = Pattern.compile("^[0-9A-Za-z]{1,8}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");

    private static boolean blank(String s) { return s == null || s.trim().isEmpty(); }

    public static String name(String v) {
        if (blank(v)) return "Ingresa tu nombre";
        if (!NAME.matcher(v.trim()).matches()) return "El nombre debe tener entre 2 y 40 letras";
        return null;
    }
    /** Acepta números y letras (311A). */
    public static String unit(String v) {
        if (blank(v)) return "Ingresa el número de tu vivienda";
        if (!UNIT.matcher(v.trim()).matches()) return "Vivienda inválida: usa letras y números (ej. 311A)";
        return null;
    }
    public static String email(String v) {
        if (blank(v)) return "Ingresa tu correo";
        if (!EMAIL.matcher(v.trim()).matches()) return "Correo no válido";
        return null;
    }
    public static String password(String v) {
        if (v == null || v.isEmpty()) return "Ingresa una contraseña";
        if (v.length() < 6) return "Mínimo 6 caracteres";
        return null;
    }
    public static String passwordConfirm(String p, String confirm) {
        if (confirm == null || !confirm.equals(p)) return "Las contraseñas no coinciden";
        return null;
    }
    public static String chatMessage(String v) {
        if (blank(v)) return "Escribe un mensaje";
        if (v.trim().length() > 500) return "Máximo 500 caracteres";
        return null;
    }
    public static String announcementTitle(String v) {
        if (blank(v)) return "Ingresa un título";
        if (v.trim().length() < 3) return "El título es muy corto";
        if (v.trim().length() > 100) return "Máximo 100 caracteres";
        return null;
    }
    public static String announcementBody(String v) {
        if (blank(v)) return "Ingresa el contenido";
        if (v.trim().length() > 2000) return "Máximo 2000 caracteres";
        return null;
    }
    public static final String[] CATEGORIES = {"Alimentos", "Hogar", "Servicios", "Otros"};

    public static String productName(String v) {
        if (blank(v)) return "Ingresa el nombre del producto";
        if (v.trim().length() < 3) return "El nombre es muy corto";
        if (v.trim().length() > 60) return "Máximo 60 caracteres";
        return null;
    }
    /** Precio: número positivo (acepta coma o punto decimal). */
    public static String price(String v) {
        if (blank(v)) return "Ingresa el precio";
        try {
            double d = Double.parseDouble(v.trim().replace(',', '.'));
            if (d <= 0) return "El precio debe ser mayor que 0";
            if (d > 100_000_000d) return "Precio demasiado alto";
            return null;
        } catch (NumberFormatException e) { return "Precio no válido"; }
    }
    public static double parsePrice(String v) { return Double.parseDouble(v.trim().replace(',', '.')); }
    public static String category(String v) {
        for (String c : CATEGORIES) if (c.equals(v)) return null;
        return "Elige una categoría";
    }
    public static String productDescription(String v) {
        if (v != null && v.trim().length() > 300) return "Máximo 300 caracteres";
        return null;
    }

    /** 311a -> 311A */
    public static String normalizeUnit(String v) { return v == null ? "" : v.trim().toUpperCase(Locale.ROOT); }
}
