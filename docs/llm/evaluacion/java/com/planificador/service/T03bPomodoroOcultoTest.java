package com.planificador.service;

import com.planificador.model.PomodoroSettings;
import com.planificador.model.StudyLog;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class T03bPomodoroOcultoTest {

    @Test
    void saltear() {
        PomodoroEngine e = new PomodoroEngine(new PomodoroSettings(2, 1, 3, 2));
        e.tick();
        e.skip();
        assertEquals(PomodoroPhase.SHORT_BREAK, e.phase());
        assertEquals(60, e.secondsLeft());
        assertEquals(0, e.completedWorkCycles());
        e.skip();
        assertEquals(PomodoroPhase.WORK, e.phase());
        assertEquals(120, e.secondsLeft());
    }

    @Test
    void formato() {
        assertEquals("25:00", PomodoroEngine.format(1500));
        assertEquals("01:05", PomodoroEngine.format(65));
        assertEquals("00:00", PomodoroEngine.format(0));
    }

    @Test
    void registroDelCiclo() {
        StudyLog l = StudyLogFactory.fromWorkCycle("x1", "s1", "t1", "Bernoulli",
                LocalDateTime.of(2026, 10, 25, 10, 25), 25);
        assertEquals("x1", l.getId());
        assertEquals("s1", l.getSubjectId());
        assertEquals("t1", l.getTopicId());
        assertEquals("Bernoulli", l.getTopicTitle());
        assertEquals("2026-10-25", l.getDate());
        assertEquals("10:00", l.getStartTime());
        assertEquals(25, l.getMinutes());
        StudyLog medianoche = StudyLogFactory.fromWorkCycle("x2", "s1", null, null,
                LocalDateTime.of(2026, 10, 26, 0, 10), 25);
        assertEquals("2026-10-26", medianoche.getDate());
        assertEquals("23:45", medianoche.getStartTime());
        assertNull(medianoche.getTopicId());
    }
}
