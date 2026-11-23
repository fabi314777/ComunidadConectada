package com.comunidadconectada.app.ui;

import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.comunidadconectada.app.R;
import com.comunidadconectada.app.model.Product;
import com.comunidadconectada.app.util.Identity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Lista de productos con filtro por categoría; el dueño ve "Marcar vendido / Reactivar". */
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.VH> {

    public interface OnToggleSold { void onToggle(Product p); }

    private final Map<String, Product> all = new HashMap<>();
    private final List<Product> shown = new ArrayList<>();
    private final String myUid;
    private final OnToggleSold listener;
    private String filter = null; // null = todas

    public ProductAdapter(String myUid, OnToggleSold listener) { this.myUid = myUid; this.listener = listener; }

    public void upsert(Product p) { all.put(p.getId(), p); refresh(); }
    public void setFilter(String category) { filter = category; refresh(); }
    public int shownCount() { return shown.size(); }
    public int totalCount() { return all.size(); }

    private void refresh() {
        shown.clear();
        for (Product p : all.values()) if (filter == null || filter.equals(p.getCategory())) shown.add(p);
        Collections.sort(shown, (a, b) -> {
            if (a.isSold() != b.isSold()) return a.isSold() ? 1 : -1; // disponibles primero
            return Long.compare(b.getTimestamp(), a.getTimestamp());
        });
        notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_product, p, false));
    }
    @Override public void onBindViewHolder(@NonNull VH h, int i) {
        final Product p = shown.get(i);
        h.name.setText(p.getName());
        h.name.setPaintFlags(p.isSold() ? h.name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                : h.name.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        h.price.setText(NumberFormat.getNumberInstance(Locale.getDefault()).format(p.getPrice()) + " · " + p.getCategory());
        h.desc.setText(p.getDescription());
        h.desc.setVisibility(p.getDescription().isEmpty() ? View.GONE : View.VISIBLE);
        h.seller.setText("Vende: " + Identity.format(p.getSellerName(), p.getUnit()));
        h.status.setText(p.isSold() ? "VENDIDO" : "DISPONIBLE");
        h.status.setTextColor(Color.parseColor(p.isSold() ? "#757575" : "#2E7D32"));
        boolean mine = p.getSellerUid().equals(myUid);
        h.toggle.setVisibility(mine ? View.VISIBLE : View.GONE);
        h.toggle.setText(p.isSold() ? "Reactivar" : "Marcar vendido");
        h.toggle.setOnClickListener(v -> listener.onToggle(p));
    }
    @Override public int getItemCount() { return shown.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name, price, desc, seller, status;
        final Button toggle;
        VH(View v) {
            super(v);
            name = v.findViewById(R.id.tv_name);
            price = v.findViewById(R.id.tv_price);
            desc = v.findViewById(R.id.tv_desc);
            seller = v.findViewById(R.id.tv_seller);
            status = v.findViewById(R.id.tv_status);
            toggle = v.findViewById(R.id.btn_toggle);
        }
    }
}
