package com.planificador.service;

import com.planificador.model.PomodoroSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PomodoroEngineTest {

    /** Ajustes chicos para no tener que llamar a tick() miles de veces: 2 min de estudio, 1 de descanso, 3 largo, cada 2. */
    static PomodoroSettings chicos() {
        return new PomodoroSettings(2, 1, 3, 2);
    }

    /** Llama a tick() n veces y devuelve la última fase terminada que no fue null (o null). */
    static PomodoroPhase avanzar(PomodoroEngine e, int n) {
        PomodoroPhase ultima = null;
        for (int i = 0; i < n; i++) {
            PomodoroPhase p = e.tick();
            if (p != null) ultima = p;
        }
        return ultima;
    }

    @Test
    void minutosDeCadaFase() {
        PomodoroEngine e = new PomodoroEngine(new PomodoroSettings());
        assertEquals(25, e.minutesFor(PomodoroPhase.WORK));
        assertEquals(15, e.minutesFor(PomodoroPhase.LONG_BREAK));
        assertEquals(1500, e.secondsLeft());
    }

    @Test
    void cicloCompletoConDescansoLargo() {
        PomodoroEngine e = new PomodoroEngine(chicos());
        assertEquals(PomodoroPhase.WORK, avanzar(e, 120));
        assertEquals(PomodoroPhase.SHORT_BREAK, e.phase());
        assertEquals(1, e.completedWorkCycles());

        assertEquals(PomodoroPhase.SHORT_BREAK, avanzar(e, 60));
        assertEquals(PomodoroPhase.WORK, e.phase());

        assertEquals(PomodoroPhase.WORK, avanzar(e, 120));
        assertEquals(PomodoroPhase.LONG_BREAK, e.phase());
        assertEquals(180, e.secondsLeft());
    }

    @Test
    void saltearNoSumaCiclos() {
        PomodoroEngine e = new PomodoroEngine(chicos());
        e.skip();
        assertEquals(PomodoroPhase.SHORT_BREAK, e.phase());
        assertEquals(60, e.secondsLeft());
        assertEquals(0, e.completedWorkCycles());
        e.skip();
        assertEquals(PomodoroPhase.WORK, e.phase());
    }

    @Test
    void formatoMinutosSegundos() {
        assertEquals("25:00", PomodoroEngine.format(1500));
        assertEquals("01:05", PomodoroEngine.format(65));
    }
}
