package com.planificador.service;

import com.planificador.model.StudyLog;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class StudyLogFactoryTest {

    @Test
    void armaElRegistroDelCiclo() {
        StudyLog l = StudyLogFactory.fromWorkCycle("x", "s1", null, null, LocalDateTime.of(2026, 10, 25, 10, 25), 25);
        assertEquals("2026-10-25", l.getDate());
        assertEquals("10:00", l.getStartTime());
        assertEquals(25, l.getMinutes());
    }
}
