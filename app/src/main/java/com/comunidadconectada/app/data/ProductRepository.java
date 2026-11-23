package com.comunidadconectada.app.data;

import com.comunidadconectada.app.Constants;
import com.comunidadconectada.app.model.Product;
import com.comunidadconectada.app.model.UserProfile;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

/** Ventas entre vecinos: /products/{comunidad}/{id}. Solo el dueño marca "vendido" (reglas de RTDB). */
public class ProductRepository {

    public interface Listener { void onProduct(Product p); void onError(String error); }
    public interface DoneCallback { void onDone(String errorOrNull); }

    private DatabaseReference ref() {
        return FirebaseDatabase.getInstance().getReference("products").child(Constants.COMMUNITY_ID);
    }

    public ListenerHandle listen(int limit, final Listener l) {
        final Query q = ref().orderByKey().limitToLast(limit);
        final ChildEventListener cel = new ChildEventListener() {
            @Override public void onChildAdded(DataSnapshot s, String prev) { emit(s, l); }
            @Override public void onChildChanged(DataSnapshot s, String prev) { emit(s, l); }
            @Override public void onChildRemoved(DataSnapshot s) { }
            @Override public void onChildMoved(DataSnapshot s, String prev) { }
            @Override public void onCancelled(DatabaseError e) { l.onError("No se pudieron leer los productos: " + e.getMessage()); }
        };
        q.addChildEventListener(cel);
        return () -> q.removeEventListener(cel);
    }

    public void publish(UserProfile seller, String name, double price, String category, String description,
                        final DoneCallback cb) {
        Map<String, Object> v = new HashMap<>();
        v.put("name", name.trim());
        v.put("price", price);
        v.put("category", category);
        v.put("description", description == null ? "" : description.trim());
        v.put("sellerUid", seller.getUid());
        v.put("sellerName", seller.getName());
        v.put("unit", seller.getUnit());
        v.put("status", Product.AVAILABLE);
        v.put("timestamp", ServerValue.TIMESTAMP);
        ref().push().setValue(v).addOnCompleteListener(t ->
                cb.onDone(t.isSuccessful() ? null : "No se pudo publicar el producto."));
    }

    /** Las reglas solo permiten este cambio al dueño del producto. */
    public void setSold(String productId, boolean sold, final DoneCallback cb) {
        ref().child(productId).child("status").setValue(sold ? Product.SOLD : Product.AVAILABLE)
                .addOnCompleteListener(t -> cb.onDone(t.isSuccessful() ? null : "No se pudo actualizar el estado."));
    }

    private static void emit(DataSnapshot s, Listener l) {
        String name = s.child("name").getValue(String.class), cat = s.child("category").getValue(String.class);
        Double price = s.child("price").getValue(Double.class);
        String status = s.child("status").getValue(String.class);
        String sellerUid = s.child("sellerUid").getValue(String.class);
        if (name == null || cat == null || price == null || status == null || sellerUid == null) return;
        Long ts = s.child("timestamp").getValue(Long.class);
        String desc = s.child("description").getValue(String.class);
        l.onProduct(new Product(s.getKey(), name, price, cat, desc == null ? "" : desc, sellerUid,
                s.child("sellerName").getValue(String.class), s.child("unit").getValue(String.class),
                status, ts == null ? 0L : ts));
    }
}
