package com.comunidadconectada.app.data;

import com.comunidadconectada.app.model.UserProfile;

/** Perfil del usuario autenticado, cargado desde Realtime Database. */
public final class SessionManager {
    private SessionManager() { }
    private static volatile UserProfile current;
    public static UserProfile getCurrent() { return current; }
    public static void setCurrent(UserProfile p) { current = p; }
    public static void clear() { current = null; }
}
