package com.comunidadconectada.app;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.comunidadconectada.app.alert.*;
import com.comunidadconectada.app.model.CommunityAlert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/** UT-24 (Mockito): verifica la INTERACCIÓN con las dependencias simuladas. Se ejecuta en Android Studio. */
public class AlertDispatcherMockitoTest {
    @Test public void publicaUnaVez_y_guardaSoloTrasConfirmacion() {
        AlertTransport transport = mock(AlertTransport.class);
        AlertStore store = mock(AlertStore.class);
        when(transport.isConnected()).thenReturn(true);
        AlertDispatcher d = new AlertDispatcher(transport, store, () -> 1000L, 30_000L);
        d.trigger("u1", "Ana", "12B", "c1", (s, a, m) -> { });
        ArgumentCaptor<AlertTransport.Callback> cb = ArgumentCaptor.forClass(AlertTransport.Callback.class);
        verify(transport, times(1)).publish(any(CommunityAlert.class), cb.capture());
        verify(store, never()).save(any());
        cb.getValue().onSuccess();
        verify(store, times(1)).save(any(CommunityAlert.class));
    }
}
