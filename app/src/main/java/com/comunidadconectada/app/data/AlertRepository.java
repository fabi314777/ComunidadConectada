package com.comunidadconectada.app.data;

import com.comunidadconectada.app.Constants;
import com.comunidadconectada.app.alert.AlertStore;
import com.comunidadconectada.app.model.CommunityAlert;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.util.HashMap;
import java.util.Map;

/** Registro histórico de alertas: /alerts/{comunidad}/{alertId}. */
public class AlertRepository implements AlertStore {

    public interface Listener { void onAlert(CommunityAlert alert); void onError(String error); }

    private DatabaseReference ref() {
        return FirebaseDatabase.getInstance().getReference("alerts").child(Constants.COMMUNITY_ID);
    }

    @Override public void save(CommunityAlert a) {
        Map<String, Object> v = new HashMap<>();
        v.put("id", a.getId());
        v.put("communityId", a.getCommunityId());
        v.put("userId", a.getUserId());
        v.put("userName", a.getUserName());
        v.put("unit", a.getUnit());
        v.put("timestamp", a.getTimestamp());
        v.put("status", a.getStatus());
        ref().child(a.getId()).setValue(v);
    }

    public ListenerHandle listenRecent(int limit, final Listener l) {
        final Query q = ref().orderByChild("timestamp").limitToLast(limit);
        final ChildEventListener cel = new ChildEventListener() {
            @Override public void onChildAdded(DataSnapshot s, String prev) { emit(s, l); }
            @Override public void onChildChanged(DataSnapshot s, String prev) { emit(s, l); }
            @Override public void onChildRemoved(DataSnapshot s) { }
            @Override public void onChildMoved(DataSnapshot s, String prev) { }
            @Override public void onCancelled(DatabaseError e) { l.onError("No se pudo leer el historial de alertas: " + e.getMessage()); }
        };
        q.addChildEventListener(cel);
        return () -> q.removeEventListener(cel);
    }

    /** Solo el administrador (lo exigen las reglas). */
    public void updateStatus(String alertId, String status) { ref().child(alertId).child("status").setValue(status); }

    /** "Enterado": confirma que el vecino vio la alerta (mejora propia). */
    public void acknowledge(String alertId, String uid) { ref().child(alertId).child("acks").child(uid).setValue(true); }

    private static void emit(DataSnapshot s, Listener l) {
        String id = s.child("id").getValue(String.class), communityId = s.child("communityId").getValue(String.class);
        String userId = s.child("userId").getValue(String.class), userName = s.child("userName").getValue(String.class);
        String unit = s.child("unit").getValue(String.class), status = s.child("status").getValue(String.class);
        Long ts = s.child("timestamp").getValue(Long.class);
        if (id == null || userId == null || userName == null || unit == null || ts == null || status == null) return;
        CommunityAlert a = new CommunityAlert(id, communityId, userId, userName, unit, ts, status);
        String me = FirebaseAuth.getInstance().getUid();
        DataSnapshot acks = s.child("acks");
        a.setAckInfo((int) acks.getChildrenCount(), me != null && acks.hasChild(me));
        l.onAlert(a);
    }
}
