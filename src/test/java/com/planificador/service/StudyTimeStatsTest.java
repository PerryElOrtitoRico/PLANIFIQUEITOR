package com.planificador.service;

import com.planificador.model.StudyLog;
import com.planificador.model.StudySession;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StudyTimeStatsTest {

    static StudyLog log(String materia, String fecha, int minutos) {
        return new StudyLog("l", materia, null, null, fecha, "10:00", minutos);
    }

    static StudySession plan(String materia, int minutos) {
        return new StudySession("p", "lunes", materia, "algo", minutos, false);
    }

    @Test
    void lunesDeLaSemana() {
        // Test: LocalDate.of(2026, 10, 19) is a Monday.
        // Input: LocalDate.of(2026, 10, 25) is a Saturday.
        // Expected: The Monday of the week containing 2026-10-25, which is 2026-10-19.
        assertEquals(LocalDate.of(2026, 10, 19), StudyTimeStats.weekStart(LocalDate.of(2026, 10, 25)));
    }

    @Test
    void minutosPorMateriaSoloDeEsaSemana() {
        // Week starts on 2026-10-19 (Monday). Ends before 2026-10-26 (Sunday).
        // Logs:
        // 1. log("s1", "2026-10-18", 100) -> Outside the week [10-19, 10-26)
        // 2. log("s1", "2026-10-19", 25) -> Inside
        // 3. log("s2", "2026-10-25", 50) -> Inside
        List<StudyLog> logs = List.of(
                log("s1", "2026-10-18", 100),
                log("s1", "2026-10-19", 25),
                log("s2", "2026-10-25", 50)
        );
        LocalDate weekStart = LocalDate.of(2026, 10, 19);
        Map<String, Integer> expected = Map.of("s1", 25, "s2", 50);
        assertEquals(expected, StudyTimeStats.minutesBySubject(logs, weekStart));
    }

    @Test
    void planeadoSumaLaGuia() {
        // Guide:
        // 1. plan("s1", 60) -> s1: 60
        // 2. plan("s1", 90) -> s1: 60 + 90 = 150
        List<StudySession> guide = List.of(
                plan("s1", 60),
                plan("s1", 90)
        );
        Map<String, Integer> expected = Map.of("s1", 150);
        assertEquals(expected, StudyTimeStats.plannedMinutesBySubject(guide));
    }

    @Test
    void comparaPlaneadoYEstudiado() {
        // lo completa otra subtarea (T02b): no lo cambies
    }

    @Test
    void minutosPorSemana() {
        // lo completa otra subtarea (T02b): no lo cambies
    }

    @Test
    void formatoDeMinutos() {
        // lo completa otra subtarea (T02b): no lo cambies
    }
}
