package com.planificador.service;

import com.planificador.model.StudyLog;
import com.planificador.model.StudySession;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class T02aStudyTimeOcultoTest {

    static StudyLog log(String materia, String fecha, int minutos) {
        return new StudyLog("l", materia, null, null, fecha, "10:00", minutos);
    }

    @Test
    void lunes() {
        assertEquals(LocalDate.of(2026, 10, 19), StudyTimeStats.weekStart(LocalDate.of(2026, 10, 25)));
        assertEquals(LocalDate.of(2026, 10, 19), StudyTimeStats.weekStart(LocalDate.of(2026, 10, 19)));
        assertEquals(LocalDate.of(2026, 10, 19), StudyTimeStats.weekStart(LocalDate.of(2026, 10, 22)));
    }

    @Test
    void soloLaSemanaPedida() {
        List<StudyLog> logs = List.of(
                log("s1", "2026-10-18", 100),  // domingo anterior
                log("s1", "2026-10-19", 25),
                log("s2", "2026-10-21", 50),
                log("s1", "2026-10-25", 25),
                log("s1", "2026-10-26", 100),  // lunes siguiente
                log("s2", "roto", 100),
                log("s2", null, 100));
        assertEquals(Map.of("s1", 50, "s2", 50), StudyTimeStats.minutesBySubject(logs, LocalDate.of(2026, 10, 19)));
        assertEquals(Map.of(), StudyTimeStats.minutesBySubject(List.of(), LocalDate.of(2026, 10, 19)));
    }

    @Test
    void planeado() {
        List<StudySession> guia = List.of(
                new StudySession("1", "lunes", "s1", "a", 60, false),
                new StudySession("2", "martes", "s2", "b", 45, true),
                new StudySession("3", "jueves", "s1", "c", 90, false),
                new StudySession("4", "jueves", null, "d", 30, false));
        assertEquals(Map.of("s1", 150, "s2", 45), StudyTimeStats.plannedMinutesBySubject(guia));
    }
}
