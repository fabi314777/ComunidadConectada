package com.comunidadconectada.app.data;

import com.comunidadconectada.app.Constants;
import com.comunidadconectada.app.model.Announcement;
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

/** Anuncios oficiales: /announcements/{comunidad}/{id}. Solo el administrador escribe (reglas de RTDB). */
public class AnnouncementRepository {

    public interface Listener { void onAnnouncement(Announcement a); void onError(String error); }
    public interface GetCallback { void onResult(Announcement a); void onError(String error); }
    public interface PublishCallback { void onDone(String errorOrNull); }

    private DatabaseReference ref() {
        return FirebaseDatabase.getInstance().getReference("announcements").child(Constants.COMMUNITY_ID);
    }

    public ListenerHandle listen(int limit, final Listener l) {
        final Query q = ref().orderByKey().limitToLast(limit);
        final ChildEventListener cel = new ChildEventListener() {
            @Override public void onChildAdded(DataSnapshot s, String prev) {
                Announcement a = parse(s);
                if (a != null) l.onAnnouncement(a);
            }
            @Override public void onChildChanged(DataSnapshot s, String prev) { }
            @Override public void onChildRemoved(DataSnapshot s) { }
            @Override public void onChildMoved(DataSnapshot s, String prev) { }
            @Override public void onCancelled(DatabaseError e) { l.onError("No se pudieron leer los anuncios: " + e.getMessage()); }
        };
        q.addChildEventListener(cel);
        return () -> q.removeEventListener(cel);
    }

    public void get(String id, final GetCallback cb) {
        ref().child(id).get().addOnCompleteListener(t -> {
            if (!t.isSuccessful()) { cb.onError("No se pudo cargar el anuncio. Revisa tu conexión."); return; }
            cb.onResult(t.getResult().exists() ? parse(t.getResult()) : null);
        });
    }

    /** Si un vecino lo invoca, las reglas rechazan la escritura (PERMISSION_DENIED), aunque evite la interfaz. */
    public void publish(UserProfile author, String title, String body, final PublishCallback cb) {
        Map<String, Object> v = new HashMap<>();
        v.put("title", title.trim());
        v.put("body", body.trim());
        v.put("authorUid", author.getUid());
        v.put("authorName", author.getName());
        v.put("timestamp", ServerValue.TIMESTAMP);
        ref().push().setValue(v).addOnCompleteListener(t ->
                cb.onDone(t.isSuccessful() ? null : "No tienes permiso para publicar o no hay conexión."));
    }

    private static Announcement parse(DataSnapshot s) {
        String title = s.child("title").getValue(String.class), body = s.child("body").getValue(String.class);
        Long ts = s.child("timestamp").getValue(Long.class);
        if (title == null || body == null) return null;
        return new Announcement(s.getKey(), title, body, s.child("authorUid").getValue(String.class),
                s.child("authorName").getValue(String.class), ts == null ? 0L : ts);
    }
}
