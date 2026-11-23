package com.comunidadconectada.app.data;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    private Prefs() { }
    private static SharedPreferences sp(Context c) { return c.getSharedPreferences("settings", Context.MODE_PRIVATE); }
    public static boolean isBackgroundAlerts(Context c) { return sp(c).getBoolean("bg_alerts", true); }
    public static void setBackgroundAlerts(Context c, boolean v) { sp(c).edit().putBoolean("bg_alerts", v).apply(); }
}
