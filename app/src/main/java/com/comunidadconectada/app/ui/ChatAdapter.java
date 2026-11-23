package com.comunidadconectada.app.ui;

import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.comunidadconectada.app.R;
import com.comunidadconectada.app.model.ChatMessage;
import com.comunidadconectada.app.util.Identity;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Lista de mensajes sin duplicados (un mismo id no se agrega dos veces). */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {
    private final List<ChatMessage> items = new ArrayList<>();
    private final Set<String> ids = new HashSet<>();
    private final String myUid;

    public ChatAdapter(String myUid) { this.myUid = myUid; }

    public boolean addNewer(ChatMessage m) {
        if (!ids.add(m.getId())) return false;
        items.add(m);
        notifyItemInserted(items.size() - 1);
        return true;
    }
    public void prepend(List<ChatMessage> older) {
        List<ChatMessage> fresh = new ArrayList<>();
        for (ChatMessage m : older) if (ids.add(m.getId())) fresh.add(m);
        if (fresh.isEmpty()) return;
        items.addAll(0, fresh);
        notifyItemRangeInserted(0, fresh.size());
    }
    public String oldestKey() { return items.isEmpty() ? null : items.get(0).getId(); }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        ChatMessage m = items.get(position);
        boolean mine = m.getUid().equals(myUid);
        h.author.setText(Identity.format(m.getName(), m.getUnit()));
        h.text.setText(m.getText());
        h.time.setText(TimeFmt.dateTime(m.getTimestamp()));
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) h.card.getLayoutParams();
        lp.gravity = mine ? Gravity.END : Gravity.START;
        h.card.setLayoutParams(lp);
        h.card.setCardBackgroundColor(Color.parseColor(mine ? "#DCEBFF" : "#F1F1F1"));
    }
    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView author, text, time;
        VH(View v) {
            super(v);
            card = v.findViewById(R.id.card);
            author = v.findViewById(R.id.tv_author);
            text = v.findViewById(R.id.tv_text);
            time = v.findViewById(R.id.tv_time);
        }
    }
}
