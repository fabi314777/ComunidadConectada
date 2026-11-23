package com.comunidadconectada.app.data;

import com.comunidadconectada.app.Constants;
import com.comunidadconectada.app.model.UserProfile;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

/** Perfiles en /users/{uid}. El rol NUNCA se elige en la app: se crea siempre como "vecino" (las reglas lo exigen). */
public class UserRepository {

    public interface ProfileCallback {
        void onResult(UserProfile profile); // null si aún no tiene perfil
        void onError(String message);
    }
    public interface DoneCallback { void onDone(String errorOrNull); }

    private DatabaseReference users() { return FirebaseDatabase.getInstance().getReference("users"); }

    public void load(String uid, final ProfileCallback cb) {
        users().child(uid).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) { cb.onError("No se pudo cargar tu perfil. Revisa tu conexión."); return; }
            DataSnapshot s = task.getResult();
            if (s == null || !s.exists()) { cb.onResult(null); return; }
            String role = s.child("role").getValue(String.class);
            cb.onResult(new UserProfile(uid,
                    s.child("name").getValue(String.class), s.child("unit").getValue(String.class),
                    s.child("email").getValue(String.class),
                    role == null ? Constants.ROLE_VECINO : role,
                    s.child("communityId").getValue(String.class)));
        });
    }

    public void create(String uid, String name, String unit, String email, final DoneCallback cb) {
        Map<String, Object> v = new HashMap<>();
        v.put("name", name);
        v.put("unit", unit);
        v.put("email", email);
        v.put("role", Constants.ROLE_VECINO);
        v.put("communityId", Constants.COMMUNITY_ID);
        v.put("createdAt", ServerValue.TIMESTAMP);
        users().child(uid).setValue(v).addOnCompleteListener(t ->
                cb.onDone(t.isSuccessful() ? null : "No se pudo guardar el perfil. Intenta de nuevo."));
    }

    public void updateName(String uid, String name, final DoneCallback cb) {
        users().child(uid).child("name").setValue(name).addOnCompleteListener(t ->
                cb.onDone(t.isSuccessful() ? null : "No se pudo actualizar el nombre."));
    }
}
