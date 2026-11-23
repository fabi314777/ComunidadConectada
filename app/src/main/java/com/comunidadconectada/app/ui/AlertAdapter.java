package com.comunidadconectada.app.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.comunidadconectada.app.Constants;
import com.comunidadconectada.app.R;
import com.comunidadconectada.app.model.CommunityAlert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Historial de alertas con acciones: "Enterado" (todos) y atender/cerrar (solo admin). */
public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.VH> {

    public interface Actions { void onAck(CommunityAlert a); void onAttend(CommunityAlert a); void onClose(CommunityAlert a); }

    private final List<CommunityAlert> items = new ArrayList<>();
    private final boolean isAdmin;
    private final String myUid;
    private final Actions actions;

    public AlertAdapter(boolean isAdmin, String myUid, Actions actions) {
        this.isAdmin = isAdmin; this.myUid = myUid; this.actions = actions;
    }

    public void upsert(CommunityAlert a) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(a.getId())) { items.set(i, a); notifyItemChanged(i); return; }
        }
        items.add(a);
        Collections.sort(items, (x, y) -> Long.compare(y.getTimestamp(), x.getTimestamp()));
        notifyDataSetChanged();
    }
    public int size() { return items.size(); }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_alert, p, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int i) {
        final CommunityAlert a = items.get(i);
        boolean mine = a.getUserId().equals(myUid);
        String status = a.getStatus();
        h.who.setText(a.identity() + (mine ? " (tú)" : ""));
        h.meta.setText(TimeFmt.dateTime(a.getTimestamp()));
        if (Constants.STATUS_ATTENDED.equals(status)) { h.status.setText("ATENDIDA"); h.status.setTextColor(Color.parseColor("#EF6C00")); }
        else if (Constants.STATUS_CLOSED.equals(status)) { h.status.setText("CERRADA"); h.status.setTextColor(Color.parseColor("#2E7D32")); }
        else { h.status.setText("ACTIVA"); h.status.setTextColor(Color.parseColor("#D32F2F")); }

        // Mejora propia: confirmación "Enterado" con contador.
        h.ack.setVisibility(mine ? View.GONE : View.VISIBLE);
        h.ack.setEnabled(!a.isAcknowledgedByMe() && !Constants.STATUS_CLOSED.equals(status));
        h.ack.setText((a.isAcknowledgedByMe() ? "✓ Enterado" : "Enterado") + " (" + a.getAckCount() + ")");
        h.ack.setOnClickListener(v -> actions.onAck(a));

        h.attend.setVisibility(isAdmin && Constants.STATUS_ACTIVE.equals(status) ? View.VISIBLE : View.GONE);
        h.close.setVisibility(isAdmin && !Constants.STATUS_CLOSED.equals(status) ? View.VISIBLE : View.GONE);
        h.attend.setOnClickListener(v -> actions.onAttend(a));
        h.close.setOnClickListener(v -> actions.onClose(a));
    }
    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView who, meta, status;
        final Button ack, attend, close;
        VH(View v) {
            super(v);
            who = v.findViewById(R.id.tv_who);
            meta = v.findViewById(R.id.tv_meta);
            status = v.findViewById(R.id.tv_status);
            ack = v.findViewById(R.id.btn_ack);
            attend = v.findViewById(R.id.btn_attend);
            close = v.findViewById(R.id.btn_close);
        }
    }
}
