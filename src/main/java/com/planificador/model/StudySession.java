package com.planificador.model;

public class StudySession {
    private String id;
    private String dayOfWeek; // lunes, martes...
    private String subjectId;
    private String topicDescription;
    private int durationMinutes;
    private boolean completed;

    public StudySession(String id, String dayOfWeek, String subjectId, String topicDescription, int durationMinutes, boolean completed) {
        this.id = id;
        this.dayOfWeek = dayOfWeek;
        this.subjectId = subjectId;
        this.topicDescription = topicDescription;
        this.durationMinutes = durationMinutes;
        this.completed = completed;
    }

    public String getId() { return id; }
    public String getDayOfWeek() { return dayOfWeek; }
    public String getSubjectId() { return subjectId; }
    public String getTopicDescription() { return topicDescription; }
    public int getDurationMinutes() { return durationMinutes; }
    public boolean isCompleted() { return completed; }

    public void setCompleted(boolean completed) { this.completed = completed; }
    public void setTopicDescription(String topicDescription) { this.topicDescription = topicDescription; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }
}