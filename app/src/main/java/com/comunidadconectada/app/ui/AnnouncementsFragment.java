package com.comunidadconectada.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.comunidadconectada.app.Constants;
import com.comunidadconectada.app.R;
import com.comunidadconectada.app.data.AnnouncementRepository;
import com.comunidadconectada.app.data.ListenerHandle;
import com.comunidadconectada.app.data.SessionManager;
import com.comunidadconectada.app.model.Announcement;
import com.comunidadconectada.app.model.UserProfile;
import com.comunidadconectada.app.util.Validators;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Anuncios oficiales: todos leen; solo el admin ve el botón de publicar.
 * Ocultar el botón es solo UX: la restricción real la imponen las reglas de Realtime Database.
 */
public class AnnouncementsFragment extends Fragment {

    private final AnnouncementRepository repo = new AnnouncementRepository();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AnnouncementAdapter adapter;
    private ListenerHandle handle;
    private ProgressBar progress;
    private TextView tvEmpty, tvError;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        return i.inflate(R.layout.fragment_announcements, c, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        final UserProfile me = SessionManager.getCurrent();
        if (me == null) return;
        progress = v.findViewById(R.id.progress);
        tvEmpty = v.findViewById(R.id.tv_empty);
        tvError = v.findViewById(R.id.tv_error);
        RecyclerView recycler = v.findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AnnouncementAdapter(a -> startActivity(new Intent(requireContext(),
                AnnouncementDetailActivity.class).putExtra(Constants.EXTRA_ANNOUNCEMENT_ID, a.getId())));
        recycler.setAdapter(adapter);

        View fab = v.findViewById(R.id.fab_new);
        fab.setVisibility(me.isAdmin() ? View.VISIBLE : View.GONE);
        fab.setOnClickListener(x -> showPublishDialog(me));

        handle = repo.listen(Constants.ANNOUNCEMENT_LIMIT, new AnnouncementRepository.Listener() {
            @Override public void onAnnouncement(Announcement a) {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.GONE);
                tvError.setVisibility(View.GONE);
                adapter.addFirst(a);
            }
            @Override public void onError(String error) {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
                tvError.setText(error);
                tvError.setVisibility(View.VISIBLE);
            }
        });
        handler.postDelayed(() -> {
            if (!isAdded()) return;
            progress.setVisibility(View.GONE);
            if (adapter.size() == 0) tvEmpty.setVisibility(View.VISIBLE);
        }, 2500);
    }

    private void showPublishDialog(final UserProfile me) {
        View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_new_announcement, null);
        final TextInputLayout tilTitle = form.findViewById(R.id.til_title), tilBody = form.findViewById(R.id.til_body);
        final TextInputEditText etTitle = form.findViewById(R.id.et_title), etBody = form.findViewById(R.id.et_body);

        final AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Nuevo anuncio oficial").setView(form)
                .setNegativeButton("Cancelar", null).setPositiveButton("Publicar", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            String title = etTitle.getText() == null ? "" : etTitle.getText().toString();
            String body = etBody.getText() == null ? "" : etBody.getText().toString();
            String eT = Validators.announcementTitle(title), eB = Validators.announcementBody(body);
            tilTitle.setError(eT);
            tilBody.setError(eB);
            if (eT != null || eB != null) return;
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            repo.publish(me, title, body, err -> {
                if (err == null) { dialog.dismiss(); return; }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                if (isAdded()) Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
            });
        }));
        dialog.show();
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        if (handle != null) { handle.remove(); handle = null; }
    }
}
