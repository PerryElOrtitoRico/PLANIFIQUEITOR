package com.planificador.model;

import java.util.ArrayList;
import java.util.List;

/** Seguimiento de cursada de una materia: reglas, notas y asistencias. Los getters nunca devuelven null. */
public class CourseTracking {
    private CourseRules rules = new CourseRules();
    private List<Evaluation> evaluations = new ArrayList<>();
    private List<AttendanceRecord> attendance = new ArrayList<>();

    public CourseTracking() { }

    public CourseTracking(CourseRules rules, List<Evaluation> evaluations, List<AttendanceRecord> attendance) {
        this.rules = rules;
        this.evaluations = new ArrayList<>(evaluations);
        this.attendance = new ArrayList<>(attendance);
    }

    public CourseRules getRules() {
        if (rules == null) rules = new CourseRules();
        return rules;
    }

    public void setRules(CourseRules rules) { this.rules = rules; }

    public List<Evaluation> getEvaluations() {
        if (evaluations == null) evaluations = new ArrayList<>();
        return evaluations;
    }

    public List<AttendanceRecord> getAttendance() {
        if (attendance == null) attendance = new ArrayList<>();
        return attendance;
    }
}
