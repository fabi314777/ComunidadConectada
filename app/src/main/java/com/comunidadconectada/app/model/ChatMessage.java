package com.comunidadconectada.app.model;

public class ChatMessage {
    private final String id, uid, name, unit, text;
    private final long timestamp;

    public ChatMessage(String id, String uid, String name, String unit, String text, long timestamp) {
        this.id = id; this.uid = uid; this.name = name; this.unit = unit;
        this.text = text; this.timestamp = timestamp;
    }
    public String getId() { return id; }
    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getUnit() { return unit; }
    public String getText() { return text; }
    public long getTimestamp() { return timestamp; }
}
