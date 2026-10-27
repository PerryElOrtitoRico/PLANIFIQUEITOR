package com.planificador.model;

/**
 * Tiempo de estudio que de verdad se hizo (ciclo de Pomodoro, cronómetro o sesión de la guía marcada como hecha).
 * Es la base del historial (calendario) y del tiempo por materia.
 * No es lo mismo que {@link StudySession}, que es lo planeado en la guía semanal.
 */
public class StudyLog {
    private String id;
    private String subjectId;
    private String topicId;    // puede ser null (sin tema)
    private String topicTitle; // puede ser null
    private String date;       // YYYY-MM-DD: día en que terminó el ciclo
    private String startTime;  // HH:mm
    private int minutes;
    private String source;     // null = Pomodoro; "guia" = sesión de la guía marcada como hecha; "cronometro" = cronómetro suelto

    public StudyLog(String id, String subjectId, String topicId, String topicTitle,
                    String date, String startTime, int minutes) {
        this.id = id;
        this.subjectId = subjectId;
        this.topicId = topicId;
        this.topicTitle = topicTitle;
        this.date = date;
        this.startTime = startTime;
        this.minutes = minutes;
    }

    public String getId() { return id; }
    public String getSubjectId() { return subjectId; }
    public String getTopicId() { return topicId; }
    public String getTopicTitle() { return topicTitle; }
    public String getDate() { return date; }
    public String getStartTime() { return startTime; }
    public int getMinutes() { return minutes; }
    public String getSource() { return source; }
    public boolean isFromGuide() { return "guia".equals(source); }
    public void setSource(String source) { this.source = source; }
}
