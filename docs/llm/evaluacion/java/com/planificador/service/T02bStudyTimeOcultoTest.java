package com.planificador.service;

import com.planificador.model.StudyLog;
import com.planificador.model.StudySession;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class T02bStudyTimeOcultoTest {

    static StudyLog log(String materia, String fecha, int minutos) {
        return new StudyLog("l", materia, null, null, fecha, "10:00", minutos);
    }

    @Test
    void comparaEnElOrdenPedido() {
        List<StudyLog> logs = List.of(log("s1", "2026-10-20", 50), log("s3", "2026-10-20", 25));
        List<StudySession> guia = List.of(new StudySession("1", "lunes", "s1", "a", 120, false),
                new StudySession("2", "lunes", "s2", "b", 60, false));
        assertEquals(List.of(new SubjectWeekTime("s2", 60, 0), new SubjectWeekTime("s1", 120, 50),
                        new SubjectWeekTime("s3", 0, 25)),
                StudyTimeStats.compareWeek(List.of("s2", "s1", "s3"), logs, guia, LocalDate.of(2026, 10, 19)));
    }

    @Test
    void porSemana() {
        List<StudyLog> logs = List.of(log("s1", "2026-10-12", 25), log("s1", "2026-10-18", 25),
                log("s2", "2026-10-19", 50), log("s1", "2026-10-25", 25), log("s1", "x", 25));
        Map<LocalDate, Integer> s1 = new TreeMap<>(Map.of(LocalDate.of(2026, 10, 12), 50, LocalDate.of(2026, 10, 19), 25));
        assertEquals(s1, StudyTimeStats.minutesByWeek(logs, "s1"));
        Map<LocalDate, Integer> todas = new TreeMap<>(Map.of(LocalDate.of(2026, 10, 12), 50, LocalDate.of(2026, 10, 19), 75));
        assertEquals(todas, StudyTimeStats.minutesByWeek(logs, null));
        assertEquals(LocalDate.of(2026, 10, 12), StudyTimeStats.minutesByWeek(logs, null).firstKey());
    }

    @Test
    void formato() {
        assertEquals("0 min", StudyTimeStats.formatMinutes(0));
        assertEquals("45 min", StudyTimeStats.formatMinutes(45));
        assertEquals("2 h", StudyTimeStats.formatMinutes(120));
        assertEquals("1 h 30 min", StudyTimeStats.formatMinutes(90));
    }
}
