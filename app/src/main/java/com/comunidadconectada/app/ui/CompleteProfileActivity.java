package com.comunidadconectada.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.comunidadconectada.app.R;
import com.comunidadconectada.app.data.UserRepository;
import com.comunidadconectada.app.util.Validators;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/** Perfil pendiente (típico de Google): pide nombre y vivienda para integrarlo al perfil del residente. */
public class CompleteProfileActivity extends AppCompatActivity {

    private TextInputLayout tilName, tilUnit;
    private TextInputEditText etName, etUnit;
    private ProgressBar progress;
    private View btnSave;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_profile);
        tilName = findViewById(R.id.til_name); tilUnit = findViewById(R.id.til_unit);
        etName = findViewById(R.id.et_name); etUnit = findViewById(R.id.et_unit);
        progress = findViewById(R.id.progress); btnSave = findViewById(R.id.btn_save);

        final FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) { finish(); return; }
        if (u.getDisplayName() != null) etName.setText(u.getDisplayName());
        btnSave.setOnClickListener(v -> save(u));
        findViewById(R.id.btn_cancel).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        });
    }

    private void save(FirebaseUser u) {
        String name = text(etName).trim();
        String unit = Validators.normalizeUnit(text(etUnit));
        String eName = Validators.name(name), eUnit = Validators.unit(unit);
        tilName.setError(eName); tilUnit.setError(eUnit);
        if (eName != null || eUnit != null) return;
        progress.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);
        new UserRepository().create(u.getUid(), name, unit, u.getEmail() == null ? "" : u.getEmail(), err -> {
            progress.setVisibility(View.GONE);
            btnSave.setEnabled(true);
            if (err != null) { Toast.makeText(this, err, Toast.LENGTH_LONG).show(); return; }
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        });
    }

    private static String text(TextInputEditText et) { return et.getText() == null ? "" : et.getText().toString(); }
}
