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
        // lo completa otra subtarea (T04a): no lo cambies
    }

    @Test
    void asistenciaSinClasesConocidas() {
        // lo completa otra subtarea (T04a): no lo cambies
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
        // lo completa otra subtarea (T04c): no lo cambies
    }

    @Test
    void todaviaPuedePromocionar() {
        // lo completa otra subtarea (T04c): no lo cambies
    }

    @Test
    void textos() {
        // lo completa otra subtarea (T04d): no lo cambies
    }
}
