package com.comunidadconectada.app.alert;

import com.comunidadconectada.app.model.CommunityAlert;

/** Canal de envío en tiempo real (MQTT). Interfaz para simularlo con Mockito. */
public interface AlertTransport {
    interface Callback { void onSuccess(); void onFailure(String reason); }
    boolean isConnected();
    void publish(CommunityAlert alert, Callback callback);
}
