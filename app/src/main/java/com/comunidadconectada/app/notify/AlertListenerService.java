package com.comunidadconectada.app.notify;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.comunidadconectada.app.mqtt.MqttAlertManager;

/** Servicio en primer plano: mantiene viva la conexión MQTT con la app en segundo plano. */
public class AlertListenerService extends Service {
    @Override public void onCreate() {
        super.onCreate();
        Notification n = NotificationHelper.buildServiceNotification(this);
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NotificationHelper.SERVICE_NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NotificationHelper.SERVICE_NOTIFICATION_ID, n);
        }
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        MqttAlertManager.getInstance().start(getApplicationContext()); // idempotente
        return START_STICKY;
    }
    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
