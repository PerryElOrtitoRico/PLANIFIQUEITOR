package com.planificador.service;

import com.planificador.model.StudyLog;
import com.planificador.model.StudySession;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/** Horas de estudio por materia y por semana, a partir de los StudyLog (Pomodoro) y la guía semanal (StudySession). */
public final class StudyTimeStats {

    private StudyTimeStats() { }

    /** Lunes de la semana de d (si d es lunes, d). */
    public static LocalDate weekStart(LocalDate d) {
        return d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** Minutos estudiados por subjectId en la semana que empieza en weekStart (de weekStart a weekStart + 6 días). */
    public static Map<String, Integer> minutesBySubject(List<StudyLog> logs, LocalDate weekStart) {
        Map<String, Integer> out = new HashMap<>();
        LocalDate end = weekStart.plusDays(7);
        for (StudyLog log : logs) {
            if (log.getDate() == null) continue;
            LocalDate f;
            try {
                f = LocalDate.parse(log.getDate());
            } catch (DateTimeParseException e) {
                continue;
            }
            // Check if f is within [weekStart, end)
            if (!f.isBefore(weekStart) && f.isBefore(end)) {
                out.merge(log.getSubjectId(), log.getMinutes(), Integer::sum);
            }
        }
        return out;
    }

    /** Minutos planeados por subjectId en la guía semanal: suma de durationMinutes. */
    public static Map<String, Integer> plannedMinutesBySubject(List<StudySession> guide) {
        Map<String, Integer> out = new HashMap<>();
        for (StudySession s : guide) {
            if (s.getSubjectId() == null) continue;
            out.merge(s.getSubjectId(), s.getDurationMinutes(), Integer::sum);
        }
        return out;
    }

    /** Planeado contra estudiado de cada materia de subjectIds, en ese orden, para la semana que empieza en weekStart. */
    public static List<SubjectWeekTime> compareWeek(List<String> subjectIds, List<StudyLog> logs,
                                                    List<StudySession> guide, LocalDate weekStart) {
        Map<String, Integer> planned = plannedMinutesBySubject(guide);
        Map<String, Integer> actual = minutesBySubject(logs, weekStart);
        List<SubjectWeekTime> out = new ArrayList<>();
        for (String id : subjectIds) {
            out.add(new SubjectWeekTime(id, planned.getOrDefault(id, 0), actual.getOrDefault(id, 0)));
        }
        return out;
    }

    /** Minutos por semana (clave: lunes de la semana) de una materia; subjectId null = todas las materias. */
    public static SortedMap<LocalDate, Integer> minutesByWeek(List<StudyLog> logs, String subjectId) {
        SortedMap<LocalDate, Integer> out = new TreeMap<>();
        for (StudyLog log : logs) {
            if (subjectId != null && !subjectId.equals(log.getSubjectId())) continue;
            if (log.getDate() == null) continue;
            LocalDate f;
            try {
                f = LocalDate.parse(log.getDate());
            } catch (DateTimeParseException e) {
                continue;
            }
            out.merge(weekStart(f), log.getMinutes(), Integer::sum);
        }
        return out;
    }

    /** "45 min", "2 h", "1 h 30 min". */
    public static String formatMinutes(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        if (h == 0) return m + " min";
        if (m == 0) return h + " h";
        return h + " h " + m + " min";
    }
}
