package com.comunidadconectada.app.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Detecta identificadores ya procesados. Con QoS 1, MQTT puede entregar el mismo mensaje
 * más de una vez; esta caché LRU acotada evita mostrar la misma alerta dos veces.
 */
public class DuplicateDetector {
    private final int capacity;
    private final LinkedHashMap<String, Boolean> seen;

    public DuplicateDetector(int capacity) {
        this.capacity = capacity;
        this.seen = new LinkedHashMap<String, Boolean>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                return size() > DuplicateDetector.this.capacity;
            }
        };
    }

    /** @return true si el id es nuevo (lo registra); false si ya se vio o es nulo/vacío. */
    public synchronized boolean markIfNew(String id) {
        if (id == null || id.isEmpty()) return false;
        return seen.put(id, Boolean.TRUE) == null;
    }
    public synchronized int size() { return seen.size(); }
    public synchronized void clear() { seen.clear(); }
}
