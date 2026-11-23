package com.comunidadconectada.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.comunidadconectada.app.Constants;
import com.comunidadconectada.app.R;
import com.comunidadconectada.app.data.AnnouncementRepository;
import com.comunidadconectada.app.model.Announcement;
import com.google.firebase.auth.FirebaseAuth;

/** Muestra un anuncio; es el destino al tocar la notificación FCM. */
public class AnnouncementDetailActivity extends AppCompatActivity {

    private ProgressBar progress;
    private TextView tvError, tvTitle, tvMeta, tvBody;
    private View btnRetry;
    private String announcementId;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }
        setContentView(R.layout.activity_announcement_detail);
        setTitle("Anuncio");
        progress = findViewById(R.id.progress);
        tvError = findViewById(R.id.tv_error);
        tvTitle = findViewById(R.id.tv_title);
        tvMeta = findViewById(R.id.tv_meta);
        tvBody = findViewById(R.id.tv_body);
        btnRetry = findViewById(R.id.btn_retry);
        announcementId = getIntent().getStringExtra(Constants.EXTRA_ANNOUNCEMENT_ID);
        btnRetry.setOnClickListener(v -> load());
        load();
    }

    private void load() {
        if (announcementId == null) { showError("Anuncio no encontrado.", false); return; }
        progress.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);
        btnRetry.setVisibility(View.GONE);
        new AnnouncementRepository().get(announcementId, new AnnouncementRepository.GetCallback() {
            @Override public void onResult(Announcement a) {
                progress.setVisibility(View.GONE);
                if (a == null) { showError("Este anuncio ya no existe.", false); return; }
                tvTitle.setText(a.getTitle());
                tvMeta.setText("Publicado por " + a.getAuthorName() + " · " + TimeFmt.dateTime(a.getTimestamp()));
                tvBody.setText(a.getBody());
            }
            @Override public void onError(String message) { showError(message, true); }
        });
    }

    private void showError(String msg, boolean retry) {
        progress.setVisibility(View.GONE);
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
        btnRetry.setVisibility(retry ? View.VISIBLE : View.GONE);
    }
}
