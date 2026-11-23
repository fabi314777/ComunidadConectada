#!/usr/bin/env node
// Asigna el rol de administrador de forma CONTROLADA (Admin SDK: no pasa por las reglas de la app).
// Uso: GOOGLE_APPLICATION_CREDENTIALS=../service-account.json DB_URL=... node set-admin.js <UID>
const admin = require("firebase-admin");
const uid = process.argv[2];
if (!uid) { console.error("Falta el UID"); process.exit(1); }
admin.initializeApp({ credential: admin.credential.applicationDefault(), databaseURL: process.env.DB_URL });
admin.database().ref(`users/${uid}/role`).set("admin").then(() => { console.log("OK: rol admin asignado a", uid); process.exit(0); });
