package com.comunidadconectada.app.notify;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

import com.comunidadconectada.app.Constants;
import com.comunidadconectada.app.R;
import com.comunidadconectada.app.model.CommunityAlert;
import com.comunidadconectada.app.ui.AnnouncementDetailActivity;
import com.comunidadconectada.app.ui.MainActivity;

/** Canales y notificaciones locales: anuncios (FCM), alertas (MQTT) y servicio en primer plano. */
public final class NotificationHelper {
    private NotificationHelper() { }
    public static final int SERVICE_NOTIFICATION_ID = 1001;

    public static void createChannels(Context c) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(Constants.CHANNEL_ANNOUNCEMENTS,
                "Anuncios del administrador", NotificationManager.IMPORTANCE_DEFAULT));
        NotificationChannel alerts = new NotificationChannel(Constants.CHANNEL_ALERTS,
                "Alertas comunitarias", NotificationManager.IMPORTANCE_HIGH);
        alerts.enableVibration(true);
        nm.createNotificationChannel(alerts);
        nm.createNotificationChannel(new NotificationChannel(Constants.CHANNEL_SERVICE,
                "Escucha de alertas", NotificationManager.IMPORTANCE_LOW));
    }

    private static boolean canNotify(Context c) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ActivityCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static Notification buildServiceNotification(Context c) {
        Intent i = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 0, i, PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(c, Constants.CHANNEL_SERVICE)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Comunidad Conectada")
                .setContentText("Escuchando alertas de la comunidad")
                .setOngoing(true).setContentIntent(pi).build();
    }

    /** Notificación local de una alerta MQTT (solo si hay permiso). Al tocarla abre la pestaña Alertas. */
    public static void showAlert(Context c, CommunityAlert a) {
        if (!canNotify(c)) return;
        Intent i = new Intent(c, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(Constants.EXTRA_TAB, Constants.TAB_ALERTS);
        PendingIntent pi = PendingIntent.getActivity(c, a.getId().hashCode(), i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new NotificationCompat.Builder(c, Constants.CHANNEL_ALERTS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Alerta comunitaria")
                .setContentText(a.message())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(a.message()))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true).setContentIntent(pi).build();
        NotificationManagerCompat.from(c).notify(a.getId().hashCode(), n);
    }

    /** Notificación de anuncio (FCM). Al tocarla se abre el anuncio correspondiente. */
    public static void showAnnouncement(Context c, String announcementId, String title, String body, String author) {
        if (!canNotify(c) || announcementId == null) return;
        Intent detail = new Intent(c, AnnouncementDetailActivity.class)
                .putExtra(Constants.EXTRA_ANNOUNCEMENT_ID, announcementId);
        PendingIntent pi = TaskStackBuilder.create(c).addNextIntentWithParentStack(detail)
                .getPendingIntent(announcementId.hashCode(), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        String text = body == null ? "" : body;
        Notification n = new NotificationCompat.Builder(c, Constants.CHANNEL_ANNOUNCEMENTS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title == null ? "Nuevo anuncio" : title)
                .setContentText(text).setSubText(author)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true).setContentIntent(pi).build();
        NotificationManagerCompat.from(c).notify(announcementId.hashCode(), n);
    }
}
