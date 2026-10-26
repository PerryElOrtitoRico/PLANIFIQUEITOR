package com.planificador.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.planificador.service.CursadaDatos.*;
import static org.junit.jupiter.api.Assertions.*;

class T04dTextosOcultoTest {

    static ExamResult ex(int n, Double nota, ExamState e) {
        return new ExamResult(n, nota, null, nota, e);
    }

    @Test
    void notas() {
        assertEquals("7", CourseStatusCalculator.formatGrade(7));
        assertEquals("10", CourseStatusCalculator.formatGrade(10.0));
        assertEquals("6,5", CourseStatusCalculator.formatGrade(6.5));
    }

    @Test
    void enCursoConFaltasConocidas() {
        CourseStatus s = new CourseStatus(CourseCondition.EN_CURSO, new AttendanceSummary(10, 8, 2, 80.0, false, true, 2),
                List.of(ex(1, 7.0, ExamState.APROBADO), ex(2, null, ExamState.PENDIENTE)), false, true);
        assertEquals(List.of("Condición: En curso", "Podés faltar 2 clases más", "Parcial 1: 7 (aprobado)",
                        "Parcial 2: falta rendir (necesitás 6 para regularizar y 8 para promocionar)",
                        "Ya no podés promocionar; todavía podés regularizar"),
                CourseStatusCalculator.describe(s, reglas()));
    }

    @Test
    void faltasYRecuperatorio() {
        CourseStatus una = new CourseStatus(CourseCondition.EN_CURSO, new AttendanceSummary(12, 9, 3, 75.0, false, true, 1),
                List.of(ex(1, 4.5, ExamState.A_RECUPERAR)), true, true);
        assertEquals(List.of("Condición: En curso", "Podés faltar 1 clase más",
                        "Parcial 1: 4,5, tenés que recuperarlo (necesitás 6)", "Todavía podés promocionar"),
                CourseStatusCalculator.describe(una, reglas()));
        CourseStatus cero = new CourseStatus(CourseCondition.REGULAR, new AttendanceSummary(16, 12, 4, 75.0, false, true, 0),
                List.of(), false, true);
        assertEquals(List.of("Condición: Regular", "No podés faltar más"), CourseStatusCalculator.describe(cero, reglas()));
        CourseStatus libre = new CourseStatus(CourseCondition.LIBRE, new AttendanceSummary(16, 10, 6, 62.5, true, true, -2),
                List.of(ex(1, 2.0, ExamState.DESAPROBADO)), false, false);
        assertEquals(List.of("Condición: Libre", "Superaste el límite de faltas por 2", "Parcial 1: 2 (desaprobado)"),
                CourseStatusCalculator.describe(libre, reglas()));
    }

    @Test
    void sinClasesConocidas() {
        CourseStatus s = new CourseStatus(CourseCondition.PROMOCION, new AttendanceSummary(8, 7, 1, 87.5, false, false, 0),
                List.of(), true, true);
        assertEquals(List.of("Condición: Promoción", "Asistencia: 87,5 % (mínimo 75 %)"),
                CourseStatusCalculator.describe(s, reglas()));
    }
}
