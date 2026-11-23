package com.comunidadconectada.app.mqtt;

/** Evento de LiveData que se consume una sola vez (evita mostrar la misma alerta al rotar pantalla). */
public class Event<T> {
    private final T content;
    private boolean handled = false;
    public Event(T content) { this.content = content; }
    public synchronized T getContentIfNotHandled() {
        if (handled) return null;
        handled = true;
        return content;
    }
}
