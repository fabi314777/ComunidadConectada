// Cloud Function (entorno de confianza): cuando el ADMINISTRADOR guarda un anuncio en Realtime
// Database, envía una notificación FCM al tópico de la comunidad. La app cliente NO envía FCM:
// la clave de servidor jamás viaja en la app.
const { onValueCreated } = require("firebase-functions/v2/database");
const admin = require("firebase-admin");
admin.initializeApp();

exports.notifyAnnouncement = onValueCreated(
  { ref: "/announcements/{community}/{id}", region: "us-central1" },
  async (event) => {
    const a = event.data.val();
    if (!a) return;
    const { community, id } = event.params;
    const body = String(a.body || "").slice(0, 180);
    await admin.messaging().send({
      topic: `anuncios_${community}`,
      // Mensaje de solo datos: la app construye la notificación y decide qué abrir al tocarla.
      data: {
        type: "announcement",
        announcementId: id,
        title: String(a.title || "Nuevo anuncio"),
        body,
        author: String(a.authorName || "Administración"),
      },
      android: { priority: "high" },
    });
  }
);
