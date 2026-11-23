#!/usr/bin/env python3
"""
Pruebas de integración MQTT contra un broker REAL de laboratorio (amqtt, en 127.0.0.1).
Los payloads viajan por el broker y luego los evalúa el código Java REAL de la app
(AlertCodec + AlertPolicy) mediante PolicyHarness. NO prueba el cliente Paho de Android.
Uso: python3 mqtt_integration_test.py <ruta_clases_java> <ruta_junit_cp>
"""
import asyncio, json, subprocess, sys, threading, time, uuid
import paho.mqtt.client as mqtt
from amqtt.broker import Broker

PORT = 18830
TOPIC = "comunidades/comunidad01/alertas"
CLASSES = sys.argv[1]
results = []

def rec(case, ok, detail):
    results.append((case, ok, detail))
    print(f"[{'PASS' if ok else 'FAIL'}] {case}: {detail}", flush=True)

def alert(name="Profe Benja", unit="311A", age_ms=0, aid=None, comm="comunidad01"):
    return json.dumps({"id": aid or str(uuid.uuid4()), "communityId": comm, "userId": "u-" + name,
                       "userName": name, "unit": unit, "timestamp": int(time.time() * 1000) - age_ms, "status": "active"})

def harness(payloads):
    p = subprocess.run(["java", "-cp", CLASSES, "PolicyHarness"], input="\n".join(payloads) + "\n",
                       capture_output=True, text=True)
    return [l for l in p.stdout.splitlines() if l and not l.startswith("Picked up")]

def client(cid, clean=True):
    c = mqtt.Client(mqtt.CallbackAPIVersion.VERSION1, client_id=cid, clean_session=clean)
    c.inbox = []
    c.on_message = lambda cl, ud, m: cl.inbox.append((m.topic, m.payload.decode(), m.qos, m.retain))
    return c

def run_tests():
    time.sleep(2)
    # IT-MQTT-01 publicar y recibir con identidad + vivienda, QoS 1
    b = client("rx-b"); b.connect("127.0.0.1", PORT); b.loop_start()
    r = b.subscribe(TOPIC, qos=1); time.sleep(0.5)
    a = client("tx-a"); a.connect("127.0.0.1", PORT); a.loop_start()
    p1 = alert(); info = a.publish(TOPIC, p1, qos=1, retain=False); info.wait_for_publish(); time.sleep(0.8)
    got = [m for m in b.inbox if m[1] == p1]
    dec = harness([got[0][1]]) if got else []
    rec("IT-MQTT-01 publica/recibe alerta con identidad y vivienda (QoS1)",
        bool(got) and got[0][2] == 1 and dec == ["ACCEPT|Profe Benja · Vivienda #311A ha solicitado ayuda."],
        f"recibido={bool(got)} qos={got[0][2] if got else None} decision_app={dec}")

    # IT-MQTT-02 alerta repetida (QoS1 puede duplicar): mismo id dos veces -> la app acepta una sola
    p2 = alert(aid="dup-1"); b.inbox.clear()
    a.publish(TOPIC, p2, qos=1).wait_for_publish(); a.publish(TOPIC, p2, qos=1).wait_for_publish(); time.sleep(0.8)
    recv = [m[1] for m in b.inbox if json.loads(m[1])["id"] == "dup-1"]
    dec = harness(recv)
    rec("IT-MQTT-02 mismo evento recibido 2 veces -> 1 sola alerta en la app",
        len(recv) == 2 and dec[0].startswith("ACCEPT") and dec[1].split("|")[0] == "DUPLICATE",
        f"mensajes_en_broker={len(recv)} decisiones_app={[d.split('|')[0] for d in dec]}")

    # IT-MQTT-03 sin retain: suscriptor tardío NO recibe alarmas viejas
    p3 = alert(aid="old-1"); a.publish(TOPIC, p3, qos=1, retain=False).wait_for_publish(); time.sleep(0.5)
    late = client("rx-late"); late.connect("127.0.0.1", PORT); late.loop_start(); late.subscribe(TOPIC, qos=1); time.sleep(1.0)
    rec("IT-MQTT-03 suscriptor tardío no recibe alarmas antiguas (sin retain)", len(late.inbox) == 0,
        f"mensajes_recibidos_al_suscribirse={len(late.inbox)}")

    # IT-MQTT-04 aislamiento por comunidad (tópicos)
    other = client("rx-other"); other.connect("127.0.0.1", PORT); other.loop_start()
    other.subscribe("comunidades/comunidad02/alertas", qos=1); time.sleep(0.5)
    a.publish(TOPIC, alert(aid="c1-only"), qos=1).wait_for_publish(); time.sleep(0.8)
    rec("IT-MQTT-04 un suscriptor de otra comunidad no recibe la alerta", len(other.inbox) == 0,
        f"recibidos_en_comunidad02={len(other.inbox)}")

    # IT-MQTT-05 alerta antigua (>5 min) llega por la red pero la app la descarta
    old = alert(aid="stale-1", age_ms=10 * 60 * 1000); dec = harness([old])
    rec("IT-MQTT-05 alerta de hace 10 min se descarta como STALE", dec == ["STALE|" + "Profe Benja · Vivienda #311A ha solicitado ayuda."], f"decision_app={dec}")

    # IT-MQTT-06 payload corrupto / otra comunidad -> INVALID
    dec = harness(["esto no es json", alert(aid="x-1", comm="OTRA")])
    rec("IT-MQTT-06 payload corrupto u otra comunidad -> INVALID", [d.split("|")[0] for d in dec] == ["INVALID", "INVALID"], f"decisiones_app={dec}")

    # IT-MQTT-07 reconexión: cae la conexión, se reconecta, re-suscribe y recibe SOLO lo nuevo (sin backlog duplicado)
    rx = client("rx-recon"); rx.connect("127.0.0.1", PORT); rx.loop_start(); rx.subscribe(TOPIC, qos=1); time.sleep(0.5)
    a.publish(TOPIC, alert(aid="before-drop"), qos=1).wait_for_publish(); time.sleep(0.6)
    antes = len(rx.inbox)
    rx.disconnect(); rx.loop_stop(); time.sleep(0.5)
    a.publish(TOPIC, alert(aid="while-offline"), qos=1).wait_for_publish(); time.sleep(0.5)
    rx2 = client("rx-recon", clean=True); rx2.connect("127.0.0.1", PORT); rx2.loop_start(); rx2.subscribe(TOPIC, qos=1); time.sleep(0.8)
    a.publish(TOPIC, alert(aid="after-reconnect"), qos=1).wait_for_publish(); time.sleep(0.8)
    ids = [json.loads(m[1])["id"] for m in rx2.inbox]
    rec("IT-MQTT-07 tras reconectar y re-suscribir solo llega lo nuevo (sin duplicar)",
        antes == 1 and ids == ["after-reconnect"], f"antes_de_caer={antes} tras_reconexion={ids}")
    for c in (a, b, late, other, rx2): c.loop_stop()

async def main():
    broker = Broker({
        "listeners": {"default": {"type": "tcp", "bind": f"127.0.0.1:{PORT}"}},
        "sys_interval": 0,
        "auth": {"allow-anonymous": True, "plugins": ["auth_anonymous"]},
        "topic-check": {"enabled": False},
    })
    await broker.start()
    t = threading.Thread(target=run_tests); t.start()
    while t.is_alive(): await asyncio.sleep(0.2)
    await broker.shutdown()

asyncio.run(main())
fails = [r for r in results if not r[1]]
print(f"\nRESUMEN: {len(results) - len(fails)}/{len(results)} PASS")
sys.exit(1 if fails else 0)
