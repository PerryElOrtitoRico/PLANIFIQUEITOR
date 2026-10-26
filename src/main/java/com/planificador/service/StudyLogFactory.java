package com.planificador.service;

import com.planificador.model.StudyLog;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Arma el StudyLog de un ciclo de estudio terminado. */
public final class StudyLogFactory {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private StudyLogFactory() { }

    /**
     * @param end     cuándo terminó el ciclo
     * @param minutes duración del ciclo
     * @return StudyLog con date = día de end y startTime = end - minutes en formato HH:mm
     */
    public static StudyLog fromWorkCycle(String id, String subjectId, String topicId, String topicTitle,
                                         LocalDateTime end, int minutes) {
        LocalDateTime start = end.minusMinutes(minutes);
        String date = end.toLocalDate().toString();
        String startTime = start.format(HORA);
        return new StudyLog(id, subjectId, topicId, topicTitle, date, startTime, minutes);
    }
}
