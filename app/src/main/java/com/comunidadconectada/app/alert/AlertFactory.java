package com.comunidadconectada.app.alert;

import com.comunidadconectada.app.Constants;
import com.comunidadconectada.app.model.CommunityAlert;
import java.util.UUID;

public final class AlertFactory {
    private AlertFactory() { }
    /** Alerta nueva con identificador único (UUID v4) y estado "active". */
    public static CommunityAlert create(String userId, String userName, String unit, String communityId, long timestamp) {
        return new CommunityAlert(UUID.randomUUID().toString(), communityId, userId, userName, unit,
                timestamp, Constants.STATUS_ACTIVE);
    }
}
