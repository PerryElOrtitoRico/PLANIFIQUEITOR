package com.planificador.service;

import com.planificador.model.PomodoroSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class T03aPomodoroOcultoTest {

    static PomodoroPhase ticks(PomodoroEngine e, int n) {
        PomodoroPhase ultima = null;
        for (int i = 0; i < n; i++) {
            PomodoroPhase p = e.tick();
            if (p != null) {
                assertNull(ultima, "solo debería terminar una fase en este tramo");
                ultima = p;
            }
        }
        return ultima;
    }

    @Test
    void porDefecto() {
        PomodoroEngine e = new PomodoroEngine(new PomodoroSettings());
        assertEquals(PomodoroPhase.WORK, e.phase());
        assertEquals(1500, e.secondsLeft());
        assertEquals(5, e.minutesFor(PomodoroPhase.SHORT_BREAK));
        assertEquals(15, e.minutesFor(PomodoroPhase.LONG_BREAK));
    }

    @Test
    void terminaAlLlegarACero() {
        PomodoroEngine e = new PomodoroEngine(new PomodoroSettings(2, 1, 3, 2));
        assertNull(ticks(e, 119));
        assertEquals(1, e.secondsLeft());
        assertEquals(PomodoroPhase.WORK, e.tick());
        assertEquals(PomodoroPhase.SHORT_BREAK, e.phase());
        assertEquals(60, e.secondsLeft());
        assertEquals(1, e.completedWorkCycles());
    }

    @Test
    void descansoLargoCadaNCiclos() {
        PomodoroEngine e = new PomodoroEngine(new PomodoroSettings(2, 1, 3, 2));
        assertEquals(PomodoroPhase.WORK, ticks(e, 120));
        assertEquals(PomodoroPhase.SHORT_BREAK, ticks(e, 60));
        assertEquals(PomodoroPhase.WORK, e.phase());
        assertEquals(PomodoroPhase.WORK, ticks(e, 120));
        assertEquals(PomodoroPhase.LONG_BREAK, e.phase());
        assertEquals(180, e.secondsLeft());
        assertEquals(2, e.completedWorkCycles());
        assertEquals(PomodoroPhase.LONG_BREAK, ticks(e, 180));
        assertEquals(PomodoroPhase.WORK, e.phase());
        assertEquals(PomodoroPhase.WORK, ticks(e, 120));
        assertEquals(PomodoroPhase.SHORT_BREAK, e.phase());
    }
}
