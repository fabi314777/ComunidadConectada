package com.comunidadconectada.app.model;

import com.comunidadconectada.app.util.Identity;

/** Alerta comunitaria. ackCount/acknowledgedByMe se leen de RTDB (mejora "Enterado"). */
public class CommunityAlert {
    private final String id, communityId, userId, userName, unit, status;
    private final long timestamp;
    private int ackCount;
    private boolean acknowledgedByMe;

    public CommunityAlert(String id, String communityId, String userId, String userName,
                          String unit, long timestamp, String status) {
        this.id = id; this.communityId = communityId; this.userId = userId;
        this.userName = userName; this.unit = unit; this.timestamp = timestamp; this.status = status;
    }
    public String getId() { return id; }
    public String getCommunityId() { return communityId; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getUnit() { return unit; }
    public long getTimestamp() { return timestamp; }
    public String getStatus() { return status; }
    public int getAckCount() { return ackCount; }
    public boolean isAcknowledgedByMe() { return acknowledgedByMe; }
    public void setAckInfo(int count, boolean byMe) { this.ackCount = count; this.acknowledgedByMe = byMe; }
    public String identity() { return Identity.format(userName, unit); }
    public String message() { return Identity.alertText(userName, unit); }
}
