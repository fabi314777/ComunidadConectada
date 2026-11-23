package com.comunidadconectada.app.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.comunidadconectada.app.ComunidadApp;
import com.comunidadconectada.app.Constants;
import com.comunidadconectada.app.R;
import com.comunidadconectada.app.alert.AlertDispatcher;
import com.comunidadconectada.app.alert.SendState;
import com.comunidadconectada.app.data.AlertRepository;
import com.comunidadconectada.app.data.ListenerHandle;
import com.comunidadconectada.app.data.SessionManager;
import com.comunidadconectada.app.model.CommunityAlert;
import com.comunidadconectada.app.model.UserProfile;
import com.google.android.material.progressindicator.CircularProgressIndicator;

/** Botón de alarma (mantener 2 s), estado del envío (enviando/enviada/fallida/bloqueada) e historial. */
public class AlertsFragment extends Fragment {

    private static final long HOLD_MS = 2000;

    private final AlertRepository repo = new AlertRepository();
    private AlertAdapter adapter;
    private ListenerHandle handle;
    private ValueAnimator holdAnimator;
    private CircularProgressIndicator progressHold;
    private TextView tvStatus, tvEmpty;
    private View btnRetry;
    private UserProfile me;

    private final AlertDispatcher.Listener sendListener = (state, alert, msg) -> {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> { if (isAdded()) showSendState(state, msg); });
    };

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        return i.inflate(R.layout.fragment_alerts, c, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        me = SessionManager.getCurrent();
        if (me == null) return;
        progressHold = v.findViewById(R.id.progress_hold);
        tvStatus = v.findViewById(R.id.tv_send_status);
        tvEmpty = v.findViewById(R.id.tv_empty);
        btnRetry = v.findViewById(R.id.btn_retry_send);
        View btnAlarm = v.findViewById(R.id.btn_alarm);

        RecyclerView recycler = v.findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AlertAdapter(me.isAdmin(), me.getUid(), new AlertAdapter.Actions() {
            @Override public void onAck(CommunityAlert a) { repo.acknowledge(a.getId(), me.getUid()); }
            @Override public void onAttend(CommunityAlert a) { repo.updateStatus(a.getId(), Constants.STATUS_ATTENDED); }
            @Override public void onClose(CommunityAlert a) { repo.updateStatus(a.getId(), Constants.STATUS_CLOSED); }
        });
        recycler.setAdapter(adapter);

        // Mantener 2 s para activar (evita activaciones accidentales).
        btnAlarm.setOnTouchListener((view, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: startHold(view); return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    boolean wasHolding = holdAnimator != null && holdAnimator.isRunning();
                    cancelHold();
                    if (wasHolding) Toast.makeText(requireContext(), "Mantén presionado 2 segundos", Toast.LENGTH_SHORT).show();
                    return true;
                default: return false;
            }
        });
        btnRetry.setOnClickListener(x -> ComunidadApp.dispatcher().retry(sendListener));

        handle = repo.listenRecent(Constants.ALERT_HISTORY_LIMIT, new AlertRepository.Listener() {
            @Override public void onAlert(CommunityAlert a) {
                if (!isAdded()) return;
                tvEmpty.setVisibility(View.GONE);
                adapter.upsert(a);
            }
            @Override public void onError(String error) {
                if (!isAdded()) return;
                tvEmpty.setText(error);
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });
        v.postDelayed(() -> { if (isAdded() && adapter.size() == 0) tvEmpty.setVisibility(View.VISIBLE); }, 2500);
    }

    private void startHold(final View button) {
        cancelHold();
        button.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        holdAnimator = ValueAnimator.ofInt(0, 100);
        holdAnimator.setDuration(HOLD_MS);
        holdAnimator.addUpdateListener(a -> progressHold.setProgressCompat((int) a.getAnimatedValue(), false));
        holdAnimator.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled = false;
            @Override public void onAnimationCancel(Animator animation) { cancelled = true; }
            @Override public void onAnimationEnd(Animator animation) {
                progressHold.setProgressCompat(0, false);
                if (!cancelled && isAdded()) {
                    button.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                    triggerAlert();
                }
            }
        });
        holdAnimator.start();
    }

    private void cancelHold() {
        if (holdAnimator != null) { holdAnimator.cancel(); holdAnimator = null; }
        if (progressHold != null) progressHold.setProgressCompat(0, false);
    }

    private void triggerAlert() {
        ComunidadApp.dispatcher().trigger(me.getUid(), me.getName(), me.getUnit(), Constants.COMMUNITY_ID, sendListener);
    }

    private void showSendState(SendState state, String msg) {
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(msg);
        btnRetry.setVisibility(state == SendState.FAILED ? View.VISIBLE : View.GONE);
        int color;
        switch (state) {
            case SENT: color = Color.parseColor("#2E7D32"); break;
            case FAILED: color = Color.parseColor("#D32F2F"); break;
            case BLOCKED: color = Color.parseColor("#EF6C00"); break;
            default: color = Color.parseColor("#1E5AA8"); break;
        }
        tvStatus.setTextColor(color);
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        cancelHold();
        if (handle != null) { handle.remove(); handle = null; }
    }
}
