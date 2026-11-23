package com.comunidadconectada.app.ui;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class TimeFmt {
    private TimeFmt() { }
    static String dateTime(long ts) {
        if (ts <= 0) return "";
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(ts));
    }
}
