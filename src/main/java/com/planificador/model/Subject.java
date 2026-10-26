package com.planificador.model;

import java.util.ArrayList;
import java.util.List;

public class Subject {
    private String id;
    private String name;
    private String color; // Ej: #4f46e5
    private String professor;
    private String schedule;
    private List<Unit> units;
    private CourseTracking course; // seguimiento de cursada; null si todavía no se configuró

    public Subject(String id, String name, String color, String professor, String schedule) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.professor = professor;
        this.schedule = schedule;
        this.units = new ArrayList<>();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public String getProfessor() { return professor; }
    public String getSchedule() { return schedule; }
    public List<Unit> getUnits() { return units; }

    public void setName(String name) { this.name = name; }
    public void setColor(String color) { this.color = color; }
    public void setProfessor(String professor) { this.professor = professor; }
    public void setSchedule(String schedule) { this.schedule = schedule; }

    /** Seguimiento de cursada, o null si la materia todavía no lo tiene. */
    public CourseTracking getCourse() { return course; }

    /** Seguimiento de cursada; si no existía, lo crea con las reglas por defecto. */
    public CourseTracking getOrCreateCourse() {
        if (course == null) course = new CourseTracking();
        return course;
    }

    public double getProgressPercentage() {
        int totalTopics = 0;
        int completedTopics = 0;
        for (Unit unit : units) {
            for (Topic topic : unit.getTopics()) {
                totalTopics++;
                if (topic.isCompleted()) completedTopics++;
            }
        }
        if (totalTopics == 0) return 0.0;
        return (completedTopics * 100.0) / totalTopics;
    }
}