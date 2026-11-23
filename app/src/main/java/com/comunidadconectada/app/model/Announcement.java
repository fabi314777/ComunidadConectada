package com.comunidadconectada.app.model;

public class Announcement {
    private final String id, title, body, authorUid, authorName;
    private final long timestamp;

    public Announcement(String id, String title, String body, String authorUid, String authorName, long timestamp) {
        this.id = id; this.title = title; this.body = body;
        this.authorUid = authorUid; this.authorName = authorName; this.timestamp = timestamp;
    }
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getAuthorUid() { return authorUid; }
    public String getAuthorName() { return authorName; }
    public long getTimestamp() { return timestamp; }
}
