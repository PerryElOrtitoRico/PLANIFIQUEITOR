package com.planificador.app.datos;

import com.planificador.app.logica.EstadoReloj;
import com.planificador.app.logica.Reloj;
import com.planificador.model.PomodoroSettings;
import com.planificador.service.PomodoroPhase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AlmacenRelojTest {

    @TempDir
    Path tmp;

    @Test
    void guardaYLeeElMismoEstado() throws IOException {
        AlmacenReloj a = new AlmacenReloj(tmp.resolve("reloj.json"));
        EstadoReloj e = Reloj.iniciar(Reloj.nuevoPomodoro(new PomodoroSettings(), "m1", "t1", "Bernoulli"), 1000L, 5L);
        e.note = "guía 3";
        a.guardar(e);
        EstadoReloj l = a.leer();
        assertEquals("m1", l.subjectId);
        assertEquals(PomodoroPhase.WORK, l.phase);
        assertTrue(l.corriendo);
        assertEquals(e.finMs, l.finMs);
        assertEquals(e.idSesion, l.idSesion);
        assertEquals("guía 3", l.note);
    }

    @Test
    void sinArchivoODanadoDevuelveNull() throws IOException {
        Path f = tmp.resolve("reloj.json");
        assertNull(new AlmacenReloj(f).leer());
        Files.write(f, "{no".getBytes());
        assertNull(new AlmacenReloj(f).leer());
    }

    @Test
    void borrarDejaSinEstado() throws IOException {
        AlmacenReloj a = new AlmacenReloj(tmp.resolve("reloj.json"));
        a.guardar(Reloj.nuevoCronometro(5, "m1", null));
        a.borrar();
        assertNull(a.leer());
    }
}
