package com.planificador.model;

public class StudySession {
    private String id;
    private String dayOfWeek; // lunes, martes...
    private String subjectId;
    private String topicDescription;
    private int durationMinutes;
    private boolean completed;
    private String completedWeek; // lunes (AAAA-MM-DD) de la semana en que se marcó; el "hecho" vale solo esa semana

    public StudySession(String id, String dayOfWeek, String subjectId, String topicDescription, int durationMinutes, boolean completed) {
        this.id = id;
        this.dayOfWeek = dayOfWeek;
        this.subjectId = subjectId;
        this.topicDescription = topicDescription;
        this.durationMinutes = durationMinutes;
        setCompleted(completed);
    }

    public String getId() { return id; }
    public String getDayOfWeek() { return dayOfWeek; }
    public String getSubjectId() { return subjectId; }
    public String getTopicDescription() { return topicDescription; }
    public int getDurationMinutes() { return durationMinutes; }
    public boolean isCompleted() { return completed && currentWeek().equals(completedWeek); }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        this.completedWeek = completed ? currentWeek() : null;
    }

    private static String currentWeek() {
        return java.time.LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).toString();
    }
    public void setTopicDescription(String topicDescription) { this.topicDescription = topicDescription; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }
}