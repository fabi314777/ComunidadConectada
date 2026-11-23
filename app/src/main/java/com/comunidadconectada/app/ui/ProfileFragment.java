package com.comunidadconectada.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.comunidadconectada.app.Constants;
import com.comunidadconectada.app.R;
import com.comunidadconectada.app.data.Prefs;
import com.comunidadconectada.app.data.SessionManager;
import com.comunidadconectada.app.data.UserRepository;
import com.comunidadconectada.app.model.UserProfile;
import com.comunidadconectada.app.mqtt.MqttAlertManager;
import com.comunidadconectada.app.notify.AlertListenerService;
import com.comunidadconectada.app.util.Validators;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;

/** Perfil: editar nombre mostrado, preferencia de segundo plano y cerrar sesión. El rol es solo lectura. */
public class ProfileFragment extends Fragment {

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        return i.inflate(R.layout.fragment_profile, c, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        final UserProfile me = SessionManager.getCurrent();
        if (me == null) return;
        final TextView tvIdentity = v.findViewById(R.id.tv_identity);
        final TextInputLayout tilName = v.findViewById(R.id.til_name);
        final TextInputEditText etName = v.findViewById(R.id.et_name);
        MaterialSwitch sw = v.findViewById(R.id.switch_bg);

        tvIdentity.setText(me.identity());
        ((TextView) v.findViewById(R.id.tv_role)).setText(me.isAdmin() ? "Perfil: Administrador" : "Perfil: Vecino");
        ((TextView) v.findViewById(R.id.tv_email)).setText(me.getEmail());
        etName.setText(me.getName());
        sw.setChecked(Prefs.isBackgroundAlerts(requireContext()));

        v.findViewById(R.id.btn_save_name).setOnClickListener(x -> {
            final String name = etName.getText() == null ? "" : etName.getText().toString().trim();
            String err = Validators.name(name);
            tilName.setError(err);
            if (err != null) return;
            new UserRepository().updateName(me.getUid(), name, error -> {
                if (!isAdded()) return;
                if (error != null) { Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show(); return; }
                UserProfile updated = me.withName(name);
                SessionManager.setCurrent(updated);
                tvIdentity.setText(updated.identity());
                Toast.makeText(requireContext(), "Nombre actualizado", Toast.LENGTH_SHORT).show();
            });
        });

        sw.setOnCheckedChangeListener((b, checked) -> {
            Prefs.setBackgroundAlerts(requireContext(), checked);
            Intent i = new Intent(requireContext(), AlertListenerService.class);
            if (checked) ContextCompat.startForegroundService(requireContext(), i);
            else requireContext().stopService(i);
        });
        v.findViewById(R.id.btn_logout).setOnClickListener(x -> logout());
    }

    private void logout() {
        // Se liberan TODAS las conexiones/suscripciones antes de salir.
        FirebaseMessaging.getInstance().unsubscribeFromTopic(Constants.FCM_TOPIC);
        requireContext().stopService(new Intent(requireContext(), AlertListenerService.class));
        MqttAlertManager.getInstance().stop();
        SessionManager.clear();
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(requireContext(), LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }
}
