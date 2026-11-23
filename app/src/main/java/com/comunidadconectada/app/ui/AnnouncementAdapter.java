package com.comunidadconectada.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.comunidadconectada.app.R;
import com.comunidadconectada.app.model.Announcement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.VH> {
    public interface OnClick { void onClick(Announcement a); }
    private final List<Announcement> items = new ArrayList<>();
    private final Set<String> ids = new HashSet<>();
    private final OnClick onClick;

    public AnnouncementAdapter(OnClick onClick) { this.onClick = onClick; }

    public void addFirst(Announcement a) {
        if (!ids.add(a.getId())) return;
        items.add(0, a);
        notifyItemInserted(0);
    }
    public int size() { return items.size(); }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_announcement, p, false));
    }
    @Override public void onBindViewHolder(@NonNull VH h, int i) {
        final Announcement a = items.get(i);
        h.title.setText(a.getTitle());
        h.preview.setText(a.getBody());
        h.meta.setText(a.getAuthorName() + " · " + TimeFmt.dateTime(a.getTimestamp()));
        h.itemView.setOnClickListener(v -> onClick.onClick(a));
    }
    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView title, preview, meta;
        VH(View v) {
            super(v);
            title = v.findViewById(R.id.tv_title);
            preview = v.findViewById(R.id.tv_preview);
            meta = v.findViewById(R.id.tv_meta);
        }
    }
}
