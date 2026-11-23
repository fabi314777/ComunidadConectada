package com.comunidadconectada.app.mqtt;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.comunidadconectada.app.BuildConfig;
import com.comunidadconectada.app.Constants;
import com.comunidadconectada.app.alert.AlertCodec;
import com.comunidadconectada.app.alert.AlertPolicy;
import com.comunidadconectada.app.alert.AlertTransport;
import com.comunidadconectada.app.model.CommunityAlert;
import com.comunidadconectada.app.notify.NotificationHelper;
import com.comunidadconectada.app.util.DuplicateDetector;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import info.mqtt.android.service.Ack;
import info.mqtt.android.service.MqttAndroidClient;

/**
 * Único punto de acceso a MQTT (singleton: UNA conexión y UNA suscripción, sin listeners duplicados).
 *  - Conecta al broker (TLS + usuario/clave) y reconecta ante cortes de red.
 *  - Se suscribe a comunidades/{id}/alertas (QoS 1) en CADA conexión (connectComplete).
 *  - Publica alertas (QoS 1, sin retain) y confirma el envío real.
 *  - Filtra duplicados / alertas antiguas / propias antes de avisar al usuario.
 */
public class MqttAlertManager implements AlertTransport {

    private static final String TAG = "MqttAlertManager";
    private static final int QOS = 1;
    private static final long RETRY_BASE_MS = 2_000L;
    private static final long RETRY_MAX_MS = 60_000L;

    private static MqttAlertManager instance;
    public static synchronized MqttAlertManager getInstance() {
        if (instance == null) instance = new MqttAlertManager();
        return instance;
    }

    private final MutableLiveData<ConnectionState> state = new MutableLiveData<>(ConnectionState.DISCONNECTED);
    private final MutableLiveData<Event<CommunityAlert>> incoming = new MutableLiveData<>();
    private final DuplicateDetector detector = new DuplicateDetector(Constants.DEDUP_CAPACITY);
    private final Handler handler = new Handler(Looper.getMainLooper());

    private Context appContext;
    private MqttAndroidClient client;
    private MqttConnectOptions options;
    private String myUid;
    private boolean started = false;
    private boolean connecting = false;
    private int attempts = 0;
    private ConnectivityManager.NetworkCallback networkCallback;

    private MqttAlertManager() { }

    public LiveData<ConnectionState> getState() { return state; }
    public LiveData<Event<CommunityAlert>> getIncomingAlerts() { return incoming; }

    /** Idempotente: llamarlo varias veces no crea más clientes ni suscripciones. */
    public synchronized void start(Context context) {
        if (started) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        appContext = context.getApplicationContext();
        myUid = user.getUid();

        String scheme = BuildConfig.MQTT_TLS ? "ssl://" : "tcp://";
        String serverUri = scheme + BuildConfig.MQTT_HOST + ":" + BuildConfig.MQTT_PORT;
        String clientId = "cc-" + myUid + "-" + UUID.randomUUID().toString().substring(0, 6);

        client = new MqttAndroidClient(appContext, serverUri, clientId, Ack.AUTO_ACK);
        client.setCallback(new MqttCallbackExtended() {
            @Override public void connectComplete(boolean reconnect, String serverURI) {
                attempts = 0;
                state.postValue(ConnectionState.CONNECTED);
                subscribe(); // conexión inicial Y cada reconexión automática
            }
            @Override public void connectionLost(Throwable cause) {
                Log.w(TAG, "Conexión MQTT perdida: " + (cause == null ? "?" : cause.getClass().getSimpleName()));
                if (started) state.postValue(ConnectionState.RECONNECTING);
            }
            @Override public void messageArrived(String topic, MqttMessage message) {
                onMessage(new String(message.getPayload(), StandardCharsets.UTF_8));
            }
            @Override public void deliveryComplete(IMqttDeliveryToken token) { }
        });

        options = new MqttConnectOptions();
        options.setUserName(BuildConfig.MQTT_USER);
        options.setPassword(BuildConfig.MQTT_PASS.toCharArray());
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setKeepAliveInterval(30);
        options.setConnectionTimeout(15);

        started = true;
        registerNetworkCallback();
        connectNow();
    }

    /** Cierra la conexión (cerrar sesión): libera callbacks, reintentos y cliente. */
    public synchronized void stop() {
        started = false;
        connecting = false;
        handler.removeCallbacksAndMessages(null);
        unregisterNetworkCallback();
        if (client != null) {
            try { client.disconnect(); } catch (Exception ignored) { }
            try { client.unregisterResources(); } catch (Exception ignored) { }
            try { client.close(); } catch (Exception ignored) { }
            client = null;
        }
        state.postValue(ConnectionState.DISCONNECTED);
    }

    private synchronized void connectNow() {
        if (!started || client == null || connecting) return;
        if (client.isConnected()) return;
        connecting = true;
        state.postValue(attempts > 0 ? ConnectionState.RECONNECTING : ConnectionState.CONNECTING);
        try {
            client.connect(options, null, new IMqttActionListener() {
                @Override public void onSuccess(IMqttToken token) { connecting = false; }
                @Override public void onFailure(IMqttToken token, Throwable e) {
                    connecting = false;
                    Log.w(TAG, "Fallo al conectar: " + (e == null ? "?" : e.getClass().getSimpleName()));
                    state.postValue(ConnectionState.RECONNECTING);
                    scheduleRetry();
                }
            });
        } catch (Exception e) {
            connecting = false;
            if (e instanceof MqttException) {
                int rc = ((MqttException) e).getReasonCode();
                if (rc == MqttException.REASON_CODE_CONNECT_IN_PROGRESS
                        || rc == MqttException.REASON_CODE_CLIENT_CONNECTED) return;
            }
            scheduleRetry();
        }
    }

    /** Reintento con espera exponencial (2, 4, 8 … hasta 60 s) para la conexión inicial. */
    private void scheduleRetry() {
        if (!started) return;
        long delay = Math.min(RETRY_MAX_MS, RETRY_BASE_MS * (1L << Math.min(attempts, 5)));
        attempts++;
        handler.postDelayed(this::connectNow, delay);
    }

    private void subscribe() {
        MqttAndroidClient c = client;
        if (c == null) return;
        final String topic = Constants.topicAlerts(Constants.COMMUNITY_ID);
        try {
            c.subscribe(topic, QOS, null, new IMqttActionListener() {
                @Override public void onSuccess(IMqttToken token) { Log.d(TAG, "Suscrito a " + topic); }
                @Override public void onFailure(IMqttToken token, Throwable e) {
                    Log.e(TAG, "No se pudo suscribir (¿permisos/ACL del broker?)");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error al suscribir: " + e.getClass().getSimpleName());
        }
    }

    /** Valida y descarta duplicados/antiguos/propios antes de avisar. */
    void onMessage(String payload) {
        CommunityAlert alert = AlertCodec.fromJson(payload);
        AlertPolicy.Decision d = AlertPolicy.evaluate(alert, Constants.COMMUNITY_ID, detector,
                System.currentTimeMillis(), Constants.ALERT_MAX_AGE_MS);
        if (d != AlertPolicy.Decision.ACCEPT) {
            Log.d(TAG, "Alerta descartada: " + d);
            return;
        }
        if (alert.getUserId().equals(myUid)) return; // la propia alerta no se muestra como alarma
        NotificationHelper.showAlert(appContext, alert);
        incoming.postValue(new Event<>(alert));
    }

    // ---- AlertTransport ----
    @Override public boolean isConnected() {
        MqttAndroidClient c = client;
        return c != null && c.isConnected();
    }

    @Override public void publish(CommunityAlert alert, final Callback callback) {
        MqttAndroidClient c = client;
        if (c == null || !c.isConnected()) { callback.onFailure("sin conexión"); return; }
        try {
            MqttMessage msg = new MqttMessage(AlertCodec.toJson(alert).getBytes(StandardCharsets.UTF_8));
            msg.setQos(QOS);         // QoS 1: al menos una vez (puede repetirse -> deduplicamos por id)
            msg.setRetained(false);  // sin retain: una alarma vieja no reaparece al suscribirse
            c.publish(Constants.topicAlerts(alert.getCommunityId()), msg, null, new IMqttActionListener() {
                @Override public void onSuccess(IMqttToken token) { callback.onSuccess(); }
                @Override public void onFailure(IMqttToken token, Throwable e) {
                    callback.onFailure(e == null ? "error" : e.getClass().getSimpleName());
                }
            });
        } catch (Exception e) { callback.onFailure(e.getClass().getSimpleName()); }
    }

    // ---- Red ----
    private void registerNetworkCallback() {
        try {
            ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override public void onAvailable(Network network) {
                    handler.post(() -> { attempts = 0; connectNow(); }); // reconectar sin esperar el temporizador
                }
            };
            cm.registerDefaultNetworkCallback(networkCallback);
        } catch (Exception e) { Log.w(TAG, "No se pudo registrar el callback de red"); }
    }

    private void unregisterNetworkCallback() {
        if (networkCallback == null || appContext == null) return;
        try {
            ((ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE))
                    .unregisterNetworkCallback(networkCallback);
        } catch (Exception ignored) { }
        networkCallback = null;
    }
}
