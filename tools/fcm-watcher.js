#!/usr/bin/env node
// ALTERNATIVA sin plan Blaze: "entorno de confianza" = tu propio computador.
// Escucha anuncios nuevos en Realtime Database y envía el FCM al tópico de la comunidad.
// Uso:  cd tools && npm i firebase-admin && \
//       GOOGLE_APPLICATION_CREDENTIALS=../service-account.json \
//       DB_URL=https://TU-PROYECTO-default-rtdb.firebaseio.com COMMUNITY=comunidad01 node fcm-watcher.js
// La cuenta de servicio (service-account*.json) está en .gitignore: NUNCA se sube al repositorio.
const admin = require("firebase-admin");
const community = process.env.COMMUNITY || "comunidad01";
admin.initializeApp({ credential: admin.credential.applicationDefault(), databaseURL: process.env.DB_URL });
const start = Date.now();
admin.database().ref(`announcements/${community}`).on("child_added", async (snap) => {
  const a = snap.val();
  if (!a || (a.timestamp || 0) < start - 5000) return; // ignora el historial antiguo
  await admin.messaging().send({
    topic: `anuncios_${community}`,
    data: { type: "announcement", announcementId: snap.key, title: String(a.title), body: String(a.body).slice(0, 180), author: String(a.authorName || "") },
    android: { priority: "high" },
  });
  console.log("FCM enviado:", snap.key);
});
console.log("Escuchando anuncios de", community);
