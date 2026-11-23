# Comunidad Conectada

App Android (Java) para residentes de un condominio: chat general, ventas entre vecinos, anuncios oficiales del
administrador y alertas comunitarias en tiempo real.

| Necesidad | Tecnología | Por qué |
|---|---|---|
| Datos persistentes y sincronizados (usuarios, chat, ventas, anuncios, historial de alertas) | **Firebase Realtime Database** | Sincroniza en vivo, guarda historial y aplica reglas de seguridad por usuario/rol |
| Aviso de anuncios aunque la app esté cerrada | **Firebase Cloud Messaging (FCM)** | Es el canal oficial de Google para despertar la app; lo envía una Cloud Function (entorno de confianza) |
| Alertas inmediatas entre dispositivos conectados | **MQTT (EMQX, QoS 1)** | Publicación/suscripción liviana y de baja latencia; separa comunidades por tópico |
| Identidad | **Firebase Authentication** (correo/clave + Google) | Cada usuario = UID único |

## Estructura
```
app/src/main/java/com/comunidadconectada/app/
  Constants.java            ← comunidad, tópicos, límites
  ComunidadApp.java         ← cache offline de RTDB, canales, despachador de alertas único
  model/                    ← UserProfile, ChatMessage, Product, Announcement, CommunityAlert
  util/                     ← Validators, DuplicateDetector, Identity ("Nombre · #311A"), Clock
  alert/                    ← AlertFactory, AlertCodec (JSON), AlertPolicy (duplicados/antiguas), AlertDispatcher
  mqtt/MqttAlertManager.java← conexión, suscripción, publicación, reconexión (Paho / EMQX)
  data/                     ← UserRepository, ChatRepository, ProductRepository, AnnouncementRepository, AlertRepository
  notify/                   ← NotificationHelper, AppMessagingService (FCM), AlertListenerService (primer plano)
  ui/                       ← Login, Register, CompleteProfile, Main, fragments Chat/Ventas/Anuncios/Alertas/Perfil
database.rules.json         ← reglas de seguridad de Realtime Database
functions/index.js          ← Cloud Function: anuncio nuevo → FCM
broker/                     ← ACL y configuración (Mosquitto local, guía EMQX Cloud)
rules-tests/                ← pruebas de reglas (Emulator Suite)
tools/it-mqtt/              ← prueba de integración MQTT; tools/set-admin.js; tools/fcm-watcher.js
app/src/test, androidTest   ← JUnit y Espresso
```

## Manual de ejecución (resumen; el detalle paso a paso está en el documento "Pasos para completar")
1. **Firebase**: crea el proyecto → agrega app Android `com.comunidadconectada.app` → descarga `google-services.json` a `app/`.
   Habilita **Authentication** (Correo/Contraseña y Google), **Realtime Database** (modo bloqueado) y pega `database.rules.json` en *Reglas*.
2. **Broker MQTT**: crea un deployment en EMQX Cloud, usuario `app_comunidad` y las reglas ACL de `broker/EMQX_CLOUD.md`.
3. Copia `local.properties.example` → `local.properties` y completa `sdk.dir` y las credenciales MQTT (no se versiona).
4. Abre en Android Studio → Sync → Run en **dos emuladores/dispositivos**.
5. **Administrador**: registra un usuario, copia su UID (Auth) y en Realtime Database pon `users/<UID>/role = "admin"` (o `node tools/set-admin.js <UID>`).
6. **Anuncios → notificación**: `firebase deploy --only functions` (plan Blaze) **o** `node tools/fcm-watcher.js` (sin Blaze).

## Pruebas
- Unitarias (JVM): `./gradlew testDebugUnitTest`
- Reglas (emulador): `firebase emulators:exec --only database "node --test rules-tests/rules.test.js"`
- Interfaz: `./gradlew connectedDebugAndroidTest` (emulador encendido)
- MQTT (laboratorio): `pip install amqtt paho-mqtt` y `python3 tools/it-mqtt/mqtt_integration_test.py <clases-java>`

## Seguridad: qué NO está en el repositorio
`google-services.json`, `local.properties`, claves de cuenta de servicio, contraseñas del broker. Ver `.gitignore`.
