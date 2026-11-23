package com.comunidadconectada.app.alert;

import com.comunidadconectada.app.model.CommunityAlert;

/** Registro persistente de alertas (Realtime Database). */
public interface AlertStore { void save(CommunityAlert alert); }
