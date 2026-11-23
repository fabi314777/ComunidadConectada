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

/** Registro: nombre, vivienda (311A), correo y contraseña. El rol siempre es "vecino". */
public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilName, tilUnit, tilEmail, tilPassword, tilConfirm;
    private TextInputEditText etName, etUnit, etEmail, etPassword, etConfirm;
    private ProgressBar progress;
    private View btnRegister;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        tilName = findViewById(R.id.til_name); tilUnit = findViewById(R.id.til_unit);
        tilEmail = findViewById(R.id.til_email); tilPassword = findViewById(R.id.til_password);
        tilConfirm = findViewById(R.id.til_confirm);
        etName = findViewById(R.id.et_name); etUnit = findViewById(R.id.et_unit);
        etEmail = findViewById(R.id.et_email); etPassword = findViewById(R.id.et_password);
        etConfirm = findViewById(R.id.et_confirm);
        progress = findViewById(R.id.progress); btnRegister = findViewById(R.id.btn_register);
        btnRegister.setOnClickListener(v -> doRegister());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
    }

    private void doRegister() {
        final String name = text(etName).trim();
        final String unit = Validators.normalizeUnit(text(etUnit));
        final String email = text(etEmail).trim();
        String pass = text(etPassword);

        String eName = Validators.name(name), eUnit = Validators.unit(unit), eEmail = Validators.email(email);
        String ePass = Validators.password(pass), eConf = Validators.passwordConfirm(pass, text(etConfirm));
        tilName.setError(eName); tilUnit.setError(eUnit); tilEmail.setError(eEmail);
        tilPassword.setError(ePass); tilConfirm.setError(eConf);
        if (eName != null || eUnit != null || eEmail != null || ePass != null || eConf != null) return;

        setLoading(true);
        final FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) { setLoading(false); tilEmail.setError(AuthErrors.friendly(task.getException())); return; }
            final FirebaseUser user = auth.getCurrentUser();
            if (user == null) { setLoading(false); return; }
            new UserRepository().create(user.getUid(), name, unit, email, err -> {
                if (err != null) {
                    user.delete(); // no dejar la cuenta a medias
                    auth.signOut();
                    setLoading(false);
                    Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                    return;
                }
                setLoading(false);
                startActivity(new Intent(this, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            });
        });
    }

    private static String text(TextInputEditText et) { return et.getText() == null ? "" : et.getText().toString(); }
}
