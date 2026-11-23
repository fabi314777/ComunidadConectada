package com.comunidadconectada.app.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.comunidadconectada.app.Constants;
import com.comunidadconectada.app.R;
import com.comunidadconectada.app.data.ChatRepository;
import com.comunidadconectada.app.data.ListenerHandle;
import com.comunidadconectada.app.data.SessionManager;
import com.comunidadconectada.app.model.ChatMessage;
import com.comunidadconectada.app.model.UserProfile;
import com.comunidadconectada.app.util.Validators;

import java.util.List;

/** Chat general en tiempo real (Realtime Database), historial limitado y paginado. */
public class ChatFragment extends Fragment {

    private final ChatRepository repo = new ChatRepository();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ChatAdapter adapter;
    private ListenerHandle handle;
    private RecyclerView recycler;
    private ProgressBar progress;
    private TextView tvEmpty, tvError;
    private View btnOlder;
    private EditText etMessage;
    private boolean hasMoreHistory = true;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        return i.inflate(R.layout.fragment_chat, c, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        final UserProfile me = SessionManager.getCurrent();
        if (me == null) return;
        recycler = v.findViewById(R.id.recycler);
        progress = v.findViewById(R.id.progress);
        tvEmpty = v.findViewById(R.id.tv_empty);
        tvError = v.findViewById(R.id.tv_error);
        btnOlder = v.findViewById(R.id.btn_load_older);
        etMessage = v.findViewById(R.id.et_message);

        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        lm.setStackFromEnd(true);
        recycler.setLayoutManager(lm);
        adapter = new ChatAdapter(me.getUid());
        recycler.setAdapter(adapter);

        v.findViewById(R.id.btn_send).setOnClickListener(x -> send(me));
        btnOlder.setOnClickListener(x -> loadOlder());

        // Solo los últimos N mensajes + los nuevos en tiempo real.
        handle = repo.listenLatest(Constants.CHAT_PAGE_SIZE, new ChatRepository.Listener() {
            @Override public void onMessage(ChatMessage m) {
                if (!isAdded()) return;
                tvError.setVisibility(View.GONE);
                progress.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.GONE);
                if (adapter.addNewer(m)) {
                    recycler.scrollToPosition(adapter.getItemCount() - 1);
                    if (adapter.getItemCount() >= Constants.CHAT_PAGE_SIZE && hasMoreHistory)
                        btnOlder.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onError(String error) {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
                tvError.setText(error + " Revisa tu conexión.");
                tvError.setVisibility(View.VISIBLE);
            }
        });
        handler.postDelayed(() -> {
            if (!isAdded()) return;
            progress.setVisibility(View.GONE);
            if (adapter.getItemCount() == 0) tvEmpty.setVisibility(View.VISIBLE);
        }, 2500);
    }

    private void send(UserProfile me) {
        final String text = etMessage.getText().toString();
        String err = Validators.chatMessage(text);
        if (err != null) { etMessage.setError(err); return; }
        etMessage.setText("");
        repo.send(me, text, error -> {
            if (error != null && isAdded()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                etMessage.setText(text); // no se pierde lo escrito
            }
        });
    }

    private void loadOlder() {
        String key = adapter.oldestKey();
        if (key == null) return;
        btnOlder.setEnabled(false);
        repo.loadOlder(key, Constants.CHAT_PAGE_SIZE, new ChatRepository.OlderCallback() {
            @Override public void onLoaded(List<ChatMessage> messages) {
                if (!isAdded()) return;
                btnOlder.setEnabled(true);
                adapter.prepend(messages);
                if (messages.size() < Constants.CHAT_PAGE_SIZE) { hasMoreHistory = false; btnOlder.setVisibility(View.GONE); }
            }
            @Override public void onError(String error) {
                if (!isAdded()) return;
                btnOlder.setEnabled(true);
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        if (handle != null) { handle.remove(); handle = null; } // libera el listener
    }
}
