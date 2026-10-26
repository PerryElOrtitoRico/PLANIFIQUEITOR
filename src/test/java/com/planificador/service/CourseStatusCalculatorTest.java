package com.planificador.service;

import com.planificador.model.AttendanceRecord;
import com.planificador.model.CourseRules;
import com.planificador.model.CourseTracking;
import com.planificador.model.Evaluation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CourseStatusCalculatorTest {

    /** 75 % de asistencia, 6 regulariza, 8 promociona, 2 parciales con recuperatorio, 16 clases. */
    static CourseRules reglas() {
        return new CourseRules(75, 6, 8, 2, List.of(true, true), 16);
    }

    static Evaluation parcial(int numero, double nota) {
        return new Evaluation("e" + numero, Evaluation.PARCIAL, numero, false, nota, null);
    }

    static Evaluation recuperatorio(int numero, double nota) {
        return new Evaluation("r" + numero, Evaluation.PARCIAL, numero, true, nota, null);
    }

    /** presentes clases presentes y ausentes clases ausentes. */
    static List<AttendanceRecord> asistencias(int presentes, int ausentes) {
        List<AttendanceRecord> lista = new ArrayList<>();
        for (int i = 0; i < presentes; i++) lista.add(new AttendanceRecord("2026-08-" + (10 + i), true));
        for (int i = 0; i < ausentes; i++) lista.add(new AttendanceRecord("2026-09-" + (10 + i), false));
        return lista;
    }

    @Test
    void asistenciaConClasesConocidas() {
        // Test 1: 8 presentes, 2 ausentes. Total 10. 10 * 100 / 10 = 100%.
        // Planned = 16. MinAttendancePercent = 75.
        // Allowed = floor(16 * (100 - 75) / 100) = floor(16 * 0.25) = floor(4.0) = 4.
        // Remaining = 4 - 2 = 2.
        AttendanceSummary a1 = CourseStatusCalculator.attendance(reglas(), asistencias(8, 2));
        assertEquals(80.0, a1.percent(), 1e-9);
        assertEquals(2, a1.remainingAbsences());
        assertFalse(a1.failed());

        // Test 2: 4 presentes, 5 ausentes. Total 9. 9 * 100 / 9 = 100%.
        // Allowed = 4.
        // Remaining = 4 - 5 = -1. Failed = true.
        AttendanceSummary a2 = CourseStatusCalculator.attendance(reglas(), asistencias(4, 5));
        assertTrue(a2.failed());
    }

    @Test
    void asistenciaSinClasesConocidas() {
        // Reglas: MinAttendancePercent = 75.
        // Test 1: 3 presentes, 1 ausente. Total 4. Percent = 3/4 * 100 = 75.0%.
        // Planned = 0. Failed = (4 > 0 && 75.0 < 75) -> false.
        CourseRules r1 = new CourseRules(75, 6, 8, 2, List.of(true, true), 0);
        AttendanceSummary a1 = CourseStatusCalculator.attendance(r1, asistencias(3, 1));
        assertFalse(a1.failed());
        assertFalse(a1.remainingKnown());

        // Test 2: 2 presentes, 1 ausente. Total 3. Percent = 2/3 * 100 = 66.66...%.
        // Planned = 0. Failed = (3 > 0 && 66.66... < 75) -> true.
        CourseRules r2 = new CourseRules(75, 6, 8, 2, List.of(true, true), 0);
        AttendanceSummary a2 = CourseStatusCalculator.attendance(r2, asistencias(2, 1));
        assertTrue(a2.failed());
    }

    @Test
    void estadosDeLosParciales() {
        List<ExamResult> r = CourseStatusCalculator.exams(reglas(), List.of(parcial(1, 7), parcial(2, 4)));
        assertEquals(ExamState.APROBADO, r.get(0).state());
        assertEquals(ExamState.A_RECUPERAR, r.get(1).state());

        r = CourseStatusCalculator.exams(reglas(), List.of());
        assertEquals(ExamState.PENDIENTE, r.get(0).state());
    }

    @Test
    void elRecuperatorioReemplazaLaNota() {
        List<ExamResult> r = CourseStatusCalculator.exams(reglas(), List.of(parcial(2, 4), recuperatorio(2, 7)));
        assertEquals(ExamState.APROBADO, r.get(1).state());
        assertEquals(7.0, r.get(1).effective());

        r = CourseStatusCalculator.exams(reglas(), List.of(parcial(2, 4), recuperatorio(2, 3)));
        assertEquals(ExamState.DESAPROBADO, r.get(1).state());
    }

    @Test
    void condiciones() {
        assertEquals(CourseCondition.PROMOCION, CourseStatusCalculator.evaluate(new CourseTracking(reglas(), List.of(parcial(1, 8), parcial(2, 9)), asistencias(14, 2))).condition());
        assertEquals(CourseCondition.REGULAR, CourseStatusCalculator.evaluate(new CourseTracking(reglas(), List.of(parcial(1, 7), parcial(2, 9)), asistencias(16, 0))).condition());
        assertEquals(CourseCondition.LIBRE, CourseStatusCalculator.evaluate(new CourseTracking(reglas(), List.of(parcial(1, 8), parcial(2, 9)), asistencias(11, 5))).condition());
        assertEquals(CourseCondition.EN_CURSO, CourseStatusCalculator.evaluate(new CourseTracking(reglas(), List.of(parcial(1, 8), parcial(2, 9)), asistencias(10, 0))).condition());
    }

    @Test
    void todaviaPuedePromocionar() {
        assertTrue(CourseStatusCalculator.evaluate(new CourseTracking(reglas(), List.of(parcial(1, 8), parcial(2, 4)), asistencias(16, 0))).canPromote());
        assertFalse(CourseStatusCalculator.evaluate(new CourseTracking(reglas(), List.of(parcial(1, 6)), asistencias(5, 0))).canPromote());
        assertTrue(CourseStatusCalculator.evaluate(new CourseTracking(reglas(), List.of(parcial(1, 6)), asistencias(5, 0))).canRegularize());
    }

    @Test
    void textos() {
        CourseStatus s = new CourseStatus(CourseCondition.EN_CURSO, new AttendanceSummary(10, 8, 2, 80.0, false, true, 2),
                List.of(new ExamResult(1, 6.5, null, 6.5, ExamState.APROBADO)), false, true);
        assertEquals(List.of("Condición: En curso", "Podés faltar 2 clases más", "Parcial 1: 6,5 (aprobado)", "Ya no podés promocionar; todavía podés regularizar"), CourseStatusCalculator.describe(s, reglas()));
        assertEquals("7", CourseStatusCalculator.formatGrade(7));
    }
}
