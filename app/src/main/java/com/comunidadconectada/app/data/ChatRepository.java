package com.comunidadconectada.app.data;

import com.comunidadconectada.app.Constants;
import com.comunidadconectada.app.model.ChatMessage;
import com.comunidadconectada.app.model.UserProfile;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Chat general: /chat/{comunidad}/messages/{pushId}. Historial limitado y paginado. */
public class ChatRepository {

    public interface Listener { void onMessage(ChatMessage message); void onError(String error); }
    public interface OlderCallback { void onLoaded(List<ChatMessage> messages); void onError(String error); }
    public interface SendCallback { void onDone(String errorOrNull); }

    private DatabaseReference ref() {
        return FirebaseDatabase.getInstance().getReference("chat").child(Constants.COMMUNITY_ID).child("messages");
    }

    /** Escucha los últimos N mensajes y los nuevos en tiempo real (no descarga todo el historial). */
    public ListenerHandle listenLatest(int limit, final Listener l) {
        final Query q = ref().orderByKey().limitToLast(limit);
        final ChildEventListener cel = new ChildEventListener() {
            @Override public void onChildAdded(DataSnapshot s, String prev) {
                ChatMessage m = parse(s);
                if (m != null) l.onMessage(m);
            }
            @Override public void onChildChanged(DataSnapshot s, String prev) { }
            @Override public void onChildRemoved(DataSnapshot s) { }
            @Override public void onChildMoved(DataSnapshot s, String prev) { }
            @Override public void onCancelled(DatabaseError e) { l.onError("No se pudo leer el chat: " + e.getMessage()); }
        };
        q.addChildEventListener(cel);
        return () -> q.removeEventListener(cel);
    }

    /** Paginación: hasta N mensajes anteriores a la clave indicada. */
    public void loadOlder(String beforeKey, int limit, final OlderCallback cb) {
        ref().orderByKey().endBefore(beforeKey).limitToLast(limit).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) { cb.onError("No se pudo cargar el historial."); return; }
            List<ChatMessage> list = new ArrayList<>();
            for (DataSnapshot s : task.getResult().getChildren()) {
                ChatMessage m = parse(s);
                if (m != null) list.add(m);
            }
            cb.onLoaded(list);
        });
    }

    /** Nombre y vivienda salen del perfil; las reglas verifican que coincidan con /users/{uid}. */
    public void send(UserProfile p, String text, final SendCallback cb) {
        Map<String, Object> v = new HashMap<>();
        v.put("uid", p.getUid());
        v.put("name", p.getName());
        v.put("unit", p.getUnit());
        v.put("text", text.trim());
        v.put("timestamp", ServerValue.TIMESTAMP);
        ref().push().setValue(v).addOnCompleteListener(t ->
                cb.onDone(t.isSuccessful() ? null : "No se pudo enviar el mensaje."));
    }

    private static ChatMessage parse(DataSnapshot s) {
        String uid = s.child("uid").getValue(String.class), name = s.child("name").getValue(String.class);
        String unit = s.child("unit").getValue(String.class), text = s.child("text").getValue(String.class);
        Long ts = s.child("timestamp").getValue(Long.class);
        if (uid == null || name == null || unit == null || text == null) return null;
        return new ChatMessage(s.getKey(), uid, name, unit, text, ts == null ? 0L : ts);
    }
}
