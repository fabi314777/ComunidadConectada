#!/usr/bin/env python3
"""
Prueba de carga MQTT en ENTORNO PROPIO (broker de laboratorio amqtt en 127.0.0.1).
N suscriptores + P publicadores envían M alertas QoS 1. Mide entrega, duplicados y latencia.
NO mide el rendimiento del broker EMQX Cloud ni del cliente Android: es un método reproducible.
Uso: python3 mqtt_load_test.py [suscriptores=20] [publicadores=5] [mensajes_por_publicador=40]
"""
import asyncio, json, statistics, sys, threading, time, uuid
import paho.mqtt.client as mqtt
from amqtt.broker import Broker

SUBS = int(sys.argv[1]) if len(sys.argv) > 1 else 20
PUBS = int(sys.argv[2]) if len(sys.argv) > 2 else 5
MSGS = int(sys.argv[3]) if len(sys.argv) > 3 else 40
PORT, TOPIC = 18831, "comunidades/comunidad01/alertas"

def run():
    time.sleep(2)
    lat, seen = [], [dict() for _ in range(SUBS)]
    lock = threading.Lock()
    subs = []
    for i in range(SUBS):
        c = mqtt.Client(mqtt.CallbackAPIVersion.VERSION1, client_id=f"sub{i}", clean_session=True)
        def on_msg(cl, ud, m, idx=i):
            now = time.time()
            d = json.loads(m.payload)
            with lock:
                seen[idx][d["id"]] = seen[idx].get(d["id"], 0) + 1
                lat.append((now - d["t"]) * 1000)
        c.on_message = on_msg; c.connect("127.0.0.1", PORT); c.loop_start(); c.subscribe(TOPIC, qos=1); subs.append(c)
    time.sleep(1.0)
    pubs = []
    for j in range(PUBS):
        c = mqtt.Client(mqtt.CallbackAPIVersion.VERSION1, client_id=f"pub{j}"); c.connect("127.0.0.1", PORT); c.loop_start(); pubs.append(c)
    ids = []; t0 = time.time()
    def publish(j):
        for k in range(MSGS):
            aid = str(uuid.uuid4()); ids.append(aid)
            pubs[j].publish(TOPIC, json.dumps({"id": aid, "t": time.time(), "userName": f"P{j}", "unit": "311A"}), qos=1).wait_for_publish()
            time.sleep(0.01)
    ths = [threading.Thread(target=publish, args=(j,)) for j in range(PUBS)]
    [t.start() for t in ths]; [t.join() for t in ths]
    time.sleep(3.0); dur = time.time() - t0
    total_exp = SUBS * len(ids)
    delivered = sum(len(s) for s in seen)
    dups = sum(sum(v - 1 for v in s.values()) for s in seen)
    lat.sort()
    p95 = lat[int(len(lat) * 0.95) - 1] if lat else 0
    print(f"CLIENTES: {SUBS} suscriptores + {PUBS} publicadores | MENSAJES: {len(ids)} alertas QoS1 | DURACION: {dur:.1f} s")
    print(f"ENTREGA: {delivered}/{total_exp} ({100*delivered/total_exp:.2f}%) | DUPLICADOS en broker: {dups}")
    print(f"LATENCIA ms: media={statistics.mean(lat):.1f} mediana={statistics.median(lat):.1f} p95={p95:.1f} max={max(lat):.1f}")
    for c in subs + pubs: c.loop_stop()

async def main():
    b = Broker({"listeners": {"default": {"type": "tcp", "bind": f"127.0.0.1:{PORT}"}}, "sys_interval": 0,
                "auth": {"allow-anonymous": True, "plugins": ["auth_anonymous"]}, "topic-check": {"enabled": False}})
    await b.start()
    th = threading.Thread(target=run); th.start()
    while th.is_alive(): await asyncio.sleep(0.2)
    await b.shutdown()
asyncio.run(main())
