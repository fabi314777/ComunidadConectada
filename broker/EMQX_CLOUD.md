# Configurar EMQX Cloud (igual que la guía del curso: ssl://xxxx.emqxsl.com:8883)

1. Deployment (Serverless/Dedicated) → anota **Connection Address** y **Port TLS 8883**.
2. **Access Control → Client Authentication → + Add**: usuario `app_comunidad`, contraseña propia (≠ la de la guía).
3. **Access Control → Authorization (ACL)** → agrega estas reglas (en este orden):
   - Allow · user `app_comunidad` · Publish & Subscribe · topic `comunidades/comunidad01/alertas`
   - Deny · All · Publish & Subscribe · topic `#`   (denegar todo lo demás)
4. En `local.properties` pon `MQTT_HOST`, `MQTT_PORT=8883`, `MQTT_TLS=true`, `MQTT_USER`, `MQTT_PASS`.
5. Para probar sin la app: EMQX → **Diagnose → WebSocket Client**, suscríbete a `comunidades/comunidad01/alertas`.
