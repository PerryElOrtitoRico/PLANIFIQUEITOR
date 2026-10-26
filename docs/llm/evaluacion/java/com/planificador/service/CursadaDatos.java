package com.planificador.service;

import com.planificador.model.AttendanceRecord;
import com.planificador.model.CourseRules;
import com.planificador.model.Evaluation;

import java.util.ArrayList;
import java.util.List;

/** Datos de prueba compartidos por los ocultos de T04. */
final class CursadaDatos {
    private CursadaDatos() { }

    static CourseRules reglas() {
        return new CourseRules(75, 6, 8, 2, List.of(true, true), 16);
    }

    static Evaluation p(int n, double nota) {
        return new Evaluation("p" + n, Evaluation.PARCIAL, n, false, nota, null);
    }

    static Evaluation r(int n, double nota) {
        return new Evaluation("r" + n, Evaluation.PARCIAL, n, true, nota, null);
    }

    static List<AttendanceRecord> asist(int presentes, int ausentes) {
        List<AttendanceRecord> l = new ArrayList<>();
        for (int i = 0; i < presentes; i++) l.add(new AttendanceRecord("2026-08-" + (10 + i), true));
        for (int i = 0; i < ausentes; i++) l.add(new AttendanceRecord("2026-09-" + (10 + i), false));
        return l;
    }
}
