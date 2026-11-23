package com.comunidadconectada.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.comunidadconectada.app.R;
import com.comunidadconectada.app.data.SessionManager;
import com.comunidadconectada.app.data.UserRepository;
import com.comunidadconectada.app.model.UserProfile;
import com.comunidadconectada.app.util.Validators;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

/** Login: correo/contraseña, recuperación de contraseña y Google (bonificación). */
public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private ProgressBar progress;
    private View btnLogin;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        auth = FirebaseAuth.getInstance();
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        progress = findViewById(R.id.progress);
        btnLogin = findViewById(R.id.btn_login);

        btnLogin.setOnClickListener(v -> doLogin());
        findViewById(R.id.btn_register).setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        findViewById(R.id.btn_forgot).setOnClickListener(v -> showForgotDialog());
        findViewById(R.id.btn_google).setOnClickListener(v -> signInWithGoogle());
    }

    @Override protected void onStart() {
        super.onStart();
        if (auth.getCurrentUser() != null) route(); // sesión persistente
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }

    private void doLogin() {
        String email = text(etEmail).trim(), pass = text(etPassword);
        String eErr = Validators.email(email), pErr = Validators.password(pass);
        tilEmail.setError(eErr);
        tilPassword.setError(pErr);
        if (eErr != null || pErr != null) return;
        setLoading(true);
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) route();
            else { setLoading(false); tilPassword.setError(AuthErrors.friendly(task.getException())); }
        });
    }

    /** Con perfil -> chat; sin perfil (p. ej. Google nuevo) -> completar vivienda. */
    private void route() {
        FirebaseUser u = auth.getCurrentUser();
        if (u == null) return;
        setLoading(true);
        new UserRepository().load(u.getUid(), new UserRepository.ProfileCallback() {
            @Override public void onResult(UserProfile profile) {
                setLoading(false);
                if (profile == null) {
                    startActivity(new Intent(LoginActivity.this, CompleteProfileActivity.class));
                } else {
                    SessionManager.setCurrent(profile);
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                }
                finish();
            }
            @Override public void onError(String message) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showForgotDialog() {
        final EditText input = new EditText(this);
        input.setHint("Correo registrado");
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setText(text(etEmail));
        new MaterialAlertDialogBuilder(this)
                .setTitle("Recuperar contraseña")
                .setMessage("Te enviaremos un correo para restablecerla.")
                .setView(input)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Enviar", (d, w) -> {
                    String email = input.getText().toString().trim();
                    String err = Validators.email(email);
                    if (err != null) { Toast.makeText(this, err, Toast.LENGTH_SHORT).show(); return; }
                    auth.sendPasswordResetEmail(email).addOnCompleteListener(t -> Toast.makeText(this,
                            t.isSuccessful() ? "Revisa tu correo para restablecer la contraseña."
                                    : AuthErrors.friendly(t.getException()), Toast.LENGTH_LONG).show());
                }).show();
    }

    // ---- Google (Credential Manager) ----
    private void signInWithGoogle() {
        // default_web_client_id lo genera google-services cuando el proveedor Google está habilitado en Firebase.
        int id = getResources().getIdentifier("default_web_client_id", "string", getPackageName());
        if (id == 0) {
            Toast.makeText(this, "Google no está configurado: habilita el proveedor en Firebase, agrega tu SHA-1 "
                    + "y vuelve a descargar google-services.json", Toast.LENGTH_LONG).show();
            return;
        }
        GetGoogleIdOption option = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false).setServerClientId(getString(id)).build();
        GetCredentialRequest request = new GetCredentialRequest.Builder().addCredentialOption(option).build();
        setLoading(true);
        CredentialManager.create(this).getCredentialAsync(this, request, new CancellationSignal(),
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override public void onResult(GetCredentialResponse result) { handleGoogle(result.getCredential()); }
                    @Override public void onError(@NonNull GetCredentialException e) {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, "No se pudo iniciar con Google.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleGoogle(Credential credential) {
        try {
            if (credential instanceof CustomCredential && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    .equals(credential.getType())) {
                GoogleIdTokenCredential g = GoogleIdTokenCredential.createFrom(credential.getData());
                AuthCredential ac = GoogleAuthProvider.getCredential(g.getIdToken(), null);
                auth.signInWithCredential(ac).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) route();
                    else { setLoading(false); Toast.makeText(this, AuthErrors.friendly(task.getException()), Toast.LENGTH_LONG).show(); }
                });
                return;
            }
        } catch (Exception ignored) { }
        setLoading(false);
        Toast.makeText(this, "Credencial de Google no válida.", Toast.LENGTH_LONG).show();
    }

    private static String text(TextInputEditText et) { return et.getText() == null ? "" : et.getText().toString(); }
}
