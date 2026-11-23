package com.comunidadconectada.app.notify;

import androidx.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;

/**
 * Recibe los mensajes FCM que envía la Cloud Function cuando el administrador publica un anuncio.
 * Son mensajes de solo datos: la app arma la notificación (así controla qué se abre al tocarla).
 */
public class AppMessagingService extends FirebaseMessagingService {
    @Override public void onMessageReceived(@NonNull RemoteMessage message) {
        Map<String, String> d = message.getData();
        if ("announcement".equals(d.get("type"))) {
            NotificationHelper.showAnnouncement(this, d.get("announcementId"),
                    d.get("title"), d.get("body"), d.get("author"));
        }
    }
    @Override public void onNewToken(@NonNull String token) {
        // Se usan tópicos FCM: el token no se guarda ni se escribe en logs.
    }
}
