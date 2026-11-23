package com.comunidadconectada.app;

import android.app.Application;

import com.comunidadconectada.app.alert.AlertDispatcher;
import com.comunidadconectada.app.data.AlertRepository;
import com.comunidadconectada.app.mqtt.MqttAlertManager;
import com.comunidadconectada.app.notify.NotificationHelper;
import com.google.firebase.database.FirebaseDatabase;

public class ComunidadApp extends Application {
    private static AlertDispatcher dispatcher;

    @Override public void onCreate() {
        super.onCreate();
        try { FirebaseDatabase.getInstance().setPersistenceEnabled(true); } catch (Exception ignored) { }
        NotificationHelper.createChannels(this);
    }

    /** Un solo despachador para toda la app: conserva el cooldown al cambiar de pestaña. */
    public static synchronized AlertDispatcher dispatcher() {
        if (dispatcher == null) {
            dispatcher = new AlertDispatcher(MqttAlertManager.getInstance(), new AlertRepository(),
                    System::currentTimeMillis, Constants.ALERT_COOLDOWN_MS);
        }
        return dispatcher;
    }
}
