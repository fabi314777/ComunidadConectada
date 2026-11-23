// Pruebas de integración de las REGLAS con Firebase Local Emulator Suite (IT-01..IT-08).
// Ejecutar desde la raíz del proyecto:  firebase emulators:exec --only database "node --test rules-tests/rules.test.js"
const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after } = require("node:test");
const fs = require("node:fs");
const { ref, set, get, update, serverTimestamp } = require("firebase/database");

const rules = fs.readFileSync(__dirname + "/../database.rules.json", "utf8");
let env;
const C = "comunidad01";
const profile = (name, unit, role = "vecino") => ({ name, unit, email: name + "@x.cl", role, communityId: C, createdAt: Date.now() });

before(async () => {
  env = await initializeTestEnvironment({ projectId: "demo-comunidad", database: { rules, host: "127.0.0.1", port: 9000 } });
  await env.withSecurityRulesDisabled(async (ctx) => {          // datos semilla: 1 admin y 1 vecino
    const db = ctx.database();
    await set(ref(db, "users/admin1"), profile("Admin", "1A", "admin"));
    await set(ref(db, "users/vec1"), profile("Ana", "311A"));
  });
});
after(async () => env.cleanup());

const vecino = () => env.authenticatedContext("vec1").database();
const admin = () => env.authenticatedContext("admin1").database();
const anon = () => env.unauthenticatedContext().database();

test("IT-01 anónimo no lee datos de la comunidad", async () => {
  await assertFails(get(ref(anon(), `chat/${C}/messages`)));
  await assertFails(get(ref(anon(), `announcements/${C}`)));
});
test("IT-02 vecino NO puede publicar anuncio (aunque evite la UI)", async () => {
  await assertFails(set(ref(vecino(), `announcements/${C}/a1`),
    { title: "Hack", body: "x", authorUid: "vec1", authorName: "Ana", timestamp: serverTimestamp() }));
});
test("IT-03 administrador SÍ publica anuncio", async () => {
  await assertSucceeds(set(ref(admin(), `announcements/${C}/a2`),
    { title: "Corte de agua", body: "Mañana 9:00", authorUid: "admin1", authorName: "Admin", timestamp: serverTimestamp() }));
});
test("IT-04 vecino NO puede asignarse rol admin al registrarse", async () => {
  await assertFails(set(ref(env.authenticatedContext("nuevo").database(), "users/nuevo"), profile("Nuevo", "5B", "admin")));
});
test("IT-05 vecino NO puede modificar su propio rol", async () => {
  await assertFails(set(ref(vecino(), "users/vec1/role"), "admin"));
});
test("IT-06 registro con vivienda 311A válido; con vivienda inválida se rechaza", async () => {
  await assertSucceeds(set(ref(env.authenticatedContext("u311").database(), "users/u311"), profile("Luis", "311A")));
  await assertFails(set(ref(env.authenticatedContext("ubad").database(), "users/ubad"), profile("Mal", "31 1-A")));
});
test("IT-07 vecino escribe en el chat con su identidad real; no puede suplantar a otro", async () => {
  await assertSucceeds(set(ref(vecino(), `chat/${C}/messages/m1`),
    { uid: "vec1", name: "Ana", unit: "311A", text: "Hola", timestamp: serverTimestamp() }));
  await assertFails(set(ref(vecino(), `chat/${C}/messages/m2`),
    { uid: "admin1", name: "Admin", unit: "1A", text: "suplanto", timestamp: serverTimestamp() }));
});
test("IT-08 solo el admin cambia el estado de una alerta; el vecino sí puede crearla y marcar 'Enterado'", async () => {
  await assertSucceeds(set(ref(vecino(), `alerts/${C}/al1`),
    { id: "al1", communityId: C, userId: "vec1", userName: "Ana", unit: "311A", timestamp: Date.now(), status: "active" }));
  await assertFails(set(ref(vecino(), `alerts/${C}/al1/status`), "closed"));
  await assertSucceeds(set(ref(admin(), `alerts/${C}/al1/status`), "attended"));
  await assertSucceeds(set(ref(vecino(), `alerts/${C}/al1/acks/vec1`), true));
});

test("IT-09 vecino publica producto propio; solo el dueño lo marca vendido", async () => {
  await assertSucceeds(set(ref(vecino(), `products/${C}/p1`),
    { name: "Bicicleta", price: 50000, category: "Hogar", description: "", sellerUid: "vec1", sellerName: "Ana", unit: "311A", status: "available", timestamp: serverTimestamp() }));
  await assertFails(set(ref(admin(), `products/${C}/p1/status`), "sold"));      // otro usuario no puede
  await assertSucceeds(set(ref(vecino(), `products/${C}/p1/status`), "sold"));   // el dueño sí
});
test("IT-10 producto con precio negativo o categoría inválida se rechaza", async () => {
  await assertFails(set(ref(vecino(), `products/${C}/p2`),
    { name: "Malo", price: -5, category: "Hogar", sellerUid: "vec1", sellerName: "Ana", unit: "311A", status: "available", timestamp: serverTimestamp() }));
  await assertFails(set(ref(vecino(), `products/${C}/p3`),
    { name: "Malo", price: 5, category: "Armas", sellerUid: "vec1", sellerName: "Ana", unit: "311A", status: "available", timestamp: serverTimestamp() }));
});
