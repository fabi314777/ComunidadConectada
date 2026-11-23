package com.comunidadconectada.app.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.comunidadconectada.app.R;
import com.comunidadconectada.app.data.ListenerHandle;
import com.comunidadconectada.app.data.ProductRepository;
import com.comunidadconectada.app.data.SessionManager;
import com.comunidadconectada.app.model.Product;
import com.comunidadconectada.app.model.UserProfile;
import com.comunidadconectada.app.util.Validators;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Ventas entre vecinos: publicar, filtrar por categoría y marcar vendido (solo el dueño). */
public class ProductsFragment extends Fragment {

    private final ProductRepository repo = new ProductRepository();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ProductAdapter adapter;
    private ListenerHandle handle;
    private ProgressBar progress;
    private TextView tvEmpty, tvError;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        return i.inflate(R.layout.fragment_products, c, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        final UserProfile me = SessionManager.getCurrent();
        if (me == null) return;
        progress = v.findViewById(R.id.progress);
        tvEmpty = v.findViewById(R.id.tv_empty);
        tvError = v.findViewById(R.id.tv_error);
        RecyclerView recycler = v.findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ProductAdapter(me.getUid(), p -> repo.setSold(p.getId(), !p.isSold(), err -> {
            if (err != null && isAdded()) Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
        }));
        recycler.setAdapter(adapter);

        Spinner filter = v.findViewById(R.id.spinner_filter);
        List<String> opts = new ArrayList<>();
        opts.add("Todas las categorías");
        opts.addAll(Arrays.asList(Validators.CATEGORIES));
        filter.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, opts));
        filter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                adapter.setFilter(pos == 0 ? null : Validators.CATEGORIES[pos - 1]);
                updateEmpty();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        v.findViewById(R.id.fab_new).setOnClickListener(x -> showPublishDialog(me));

        handle = repo.listen(100, new ProductRepository.Listener() {
            @Override public void onProduct(Product p) {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
                tvError.setVisibility(View.GONE);
                adapter.upsert(p);
                updateEmpty();
            }
            @Override public void onError(String error) {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
                tvError.setText(error);
                tvError.setVisibility(View.VISIBLE);
            }
        });
        handler.postDelayed(() -> { if (isAdded()) { progress.setVisibility(View.GONE); updateEmpty(); } }, 2500);
    }

    private void updateEmpty() { tvEmpty.setVisibility(adapter.shownCount() == 0 ? View.VISIBLE : View.GONE); }

    private void showPublishDialog(final UserProfile me) {
        View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_new_product, null);
        final TextInputLayout tilName = form.findViewById(R.id.til_pname), tilPrice = form.findViewById(R.id.til_price),
                tilDesc = form.findViewById(R.id.til_pdesc);
        final TextInputEditText etName = form.findViewById(R.id.et_pname), etPrice = form.findViewById(R.id.et_price),
                etDesc = form.findViewById(R.id.et_pdesc);
        final Spinner spCat = form.findViewById(R.id.spinner_category);
        spCat.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, Validators.CATEGORIES));

        final AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Publicar producto").setView(form)
                .setNegativeButton("Cancelar", null).setPositiveButton("Publicar", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            String name = etName.getText() == null ? "" : etName.getText().toString();
            String price = etPrice.getText() == null ? "" : etPrice.getText().toString();
            String desc = etDesc.getText() == null ? "" : etDesc.getText().toString();
            String cat = (String) spCat.getSelectedItem();
            String eN = Validators.productName(name), eP = Validators.price(price), eD = Validators.productDescription(desc);
            tilName.setError(eN); tilPrice.setError(eP); tilDesc.setError(eD);
            if (eN != null || eP != null || eD != null || Validators.category(cat) != null) return;
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            repo.publish(me, name, Validators.parsePrice(price), cat, desc, err -> {
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
