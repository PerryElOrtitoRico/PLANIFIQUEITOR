package com.planificador.model;

import java.util.ArrayList;
import java.util.List;

/** Reglamento de cursada de una materia. Lo carga el usuario. */
public class CourseRules {
    private double minAttendancePercent = 75;
    private double passGrade = 6;       // nota mínima para regularizar
    private double promotionGrade = 8;  // nota mínima para promocionar; 0 = la materia no tiene promoción
    private int examCount = 2;          // cantidad de parciales
    private List<Boolean> retakes = new ArrayList<>(List.of(true, true)); // retakes.get(i): el parcial i+1 tiene recuperatorio
    private int plannedClasses = 0;     // clases del cuatrimestre; 0 = no se sabe

    public CourseRules() { }

    public CourseRules(double minAttendancePercent, double passGrade, double promotionGrade,
                       int examCount, List<Boolean> retakes, int plannedClasses) {
        this.minAttendancePercent = minAttendancePercent;
        this.passGrade = passGrade;
        this.promotionGrade = promotionGrade;
        this.examCount = examCount;
        this.retakes = new ArrayList<>(retakes);
        this.plannedClasses = plannedClasses;
    }

    public double getMinAttendancePercent() { return minAttendancePercent; }
    public double getPassGrade() { return passGrade; }
    public double getPromotionGrade() { return promotionGrade; }
    public int getExamCount() { return examCount; }
    public List<Boolean> getRetakes() { return retakes == null ? List.of() : retakes; }
    public int getPlannedClasses() { return plannedClasses; }

    /** true si el parcial número examNumber (1, 2, …) tiene recuperatorio. */
    public boolean hasRetake(int examNumber) {
        int i = examNumber - 1;
        return retakes != null && i >= 0 && i < retakes.size() && Boolean.TRUE.equals(retakes.get(i));
    }
}
