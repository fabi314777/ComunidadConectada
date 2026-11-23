package com.comunidadconectada.app.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.comunidadconectada.app.Constants;
import com.comunidadconectada.app.R;
import com.comunidadconectada.app.data.AlertRepository;
import com.comunidadconectada.app.data.Prefs;
import com.comunidadconectada.app.data.SessionManager;
import com.comunidadconectada.app.data.UserRepository;
import com.comunidadconectada.app.model.CommunityAlert;
import com.comunidadconectada.app.model.UserProfile;
import com.comunidadconectada.app.mqtt.ConnectionState;
import com.comunidadconectada.app.mqtt.Event;
import com.comunidadconectada.app.mqtt.MqttAlertManager;
import com.comunidadconectada.app.notify.AlertListenerService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

/** Pestañas Chat/Anuncios/Alertas/Perfil, indicador de conexión y diálogo de alerta entrante. */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView nav;
    private TextView banner;
    private boolean rtdbConnected = false;
    private ConnectionState mqttState = ConnectionState.DISCONNECTED;
    private DatabaseReference connectedRef;
    private ValueEventListener connectedListener;
    private AlertDialog alertDialog;
    private boolean uiReady = false;

    private final ActivityResultLauncher<String> notifPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nav = findViewById(R.id.bottom_nav);
        banner = findViewById(R.id.tv_banner);

        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { goToLogin(); return; }

        if (SessionManager.getCurrent() != null) {
            initUi(savedInstanceState);
        } else {
            new UserRepository().load(user.getUid(), new UserRepository.ProfileCallback() {
                @Override public void onResult(UserProfile p) {
                    if (p == null) {
                        startActivity(new Intent(MainActivity.this, CompleteProfileActivity.class));
                        finish();
                        return;
                    }
                    SessionManager.setCurrent(p);
                    initUi(savedInstanceState);
                }
                @Override public void onError(String message) { banner.setText(message); }
            });
        }
    }

    private void initUi(Bundle savedInstanceState) {
        uiReady = true;
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment f;
            if (id == R.id.nav_chat) f = new ChatFragment();
            else if (id == R.id.nav_products) f = new ProductsFragment();
            else if (id == R.id.nav_announcements) f = new AnnouncementsFragment();
            else if (id == R.id.nav_alerts) f = new AlertsFragment();
            else f = new ProfileFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, f).commit();
            return true;
        });
        if (savedInstanceState == null) nav.setSelectedItemId(R.id.nav_chat);
        handleIntent(getIntent());

        requestNotificationPermission();
        FirebaseMessaging.getInstance().subscribeToTopic(Constants.FCM_TOPIC);

        MqttAlertManager mqtt = MqttAlertManager.getInstance();
        mqtt.start(this);
        if (Prefs.isBackgroundAlerts(this)) {
            ContextCompat.startForegroundService(this, new Intent(this, AlertListenerService.class));
        }
        mqtt.getState().observe(this, s -> { mqttState = s; updateBanner(); });
        mqtt.getIncomingAlerts().observe(this, this::onIncomingAlert);
        watchDatabaseConnection();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (uiReady) handleIntent(intent);
    }

    private void handleIntent(Intent i) {
        if (i != null && Constants.TAB_ALERTS.equals(i.getStringExtra(Constants.EXTRA_TAB))) {
            nav.setSelectedItemId(R.id.nav_alerts);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    // ---- Indicador de conexión (mejora propia) ----
    private void watchDatabaseConnection() {
        connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedListener = new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot s) {
                rtdbConnected = Boolean.TRUE.equals(s.getValue(Boolean.class));
                updateBanner();
            }
            @Override public void onCancelled(DatabaseError e) { }
        };
        connectedRef.addValueEventListener(connectedListener);
    }

    private void updateBanner() {
        String data = rtdbConnected ? "Datos: en línea" : "Datos: sin conexión";
        String alerts;
        switch (mqttState) {
            case CONNECTED: alerts = "Alertas: conectado"; break;
            case CONNECTING: alerts = "Alertas: conectando…"; break;
            case RECONNECTING: alerts = "Alertas: reconectando…"; break;
            default: alerts = "Alertas: desconectado"; break;
        }
        banner.setText(data + "  ·  " + alerts);
        boolean ok = rtdbConnected && mqttState == ConnectionState.CONNECTED;
        banner.setBackgroundColor(ContextCompat.getColor(this, ok ? R.color.ok : R.color.warn));
    }

    // ---- Alerta entrante con la app abierta ----
    private void onIncomingAlert(Event<CommunityAlert> event) {
        if (event == null) return;
        final CommunityAlert a = event.getContentIfNotHandled();
        if (a == null) return;
        if (alertDialog != null && alertDialog.isShowing()) return;
        alertDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Alerta comunitaria")
                .setMessage(a.message())
                .setCancelable(false)
                .setPositiveButton("Enterado", (d, w) -> {
                    FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
                    if (u != null) new AlertRepository().acknowledge(a.getId(), u.getUid());
                })
                .setNegativeButton("Ver alertas", (d, w) -> nav.setSelectedItemId(R.id.nav_alerts))
                .show();
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (connectedRef != null && connectedListener != null) connectedRef.removeEventListener(connectedListener);
        if (alertDialog != null && alertDialog.isShowing()) alertDialog.dismiss();
    }
}
