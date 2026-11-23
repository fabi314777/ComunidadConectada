package com.comunidadconectada.app.data;

/** Permite liberar un listener de Firebase (evita duplicados y consumo innecesario). */
public interface ListenerHandle { void remove(); }
