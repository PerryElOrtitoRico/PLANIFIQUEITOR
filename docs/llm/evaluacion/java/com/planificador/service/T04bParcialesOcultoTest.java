package com.planificador.service;

import com.planificador.model.CourseRules;
import com.planificador.model.Evaluation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.planificador.service.CursadaDatos.*;
import static org.junit.jupiter.api.Assertions.*;

class T04bParcialesOcultoTest {

    @Test
    void sinNotasTodoPendiente() {
        assertEquals(List.of(new ExamResult(1, null, null, null, ExamState.PENDIENTE),
                        new ExamResult(2, null, null, null, ExamState.PENDIENTE)),
                CourseStatusCalculator.exams(reglas(), List.of()));
    }

    @Test
    void estados() {
        Evaluation tp = new Evaluation("t", Evaluation.TP, 1, false, 2, null);
        assertEquals(List.of(new ExamResult(1, 7.0, null, 7.0, ExamState.APROBADO),
                        new ExamResult(2, 4.0, null, 4.0, ExamState.A_RECUPERAR)),
                CourseStatusCalculator.exams(reglas(), List.of(p(1, 7), tp, p(2, 4))));
        assertEquals(new ExamResult(2, 4.0, 3.0, 3.0, ExamState.DESAPROBADO),
                CourseStatusCalculator.exams(reglas(), List.of(p(2, 4), r(2, 3))).get(1));
        CourseRules sinRecu = new CourseRules(75, 6, 8, 2, List.of(false, false), 16);
        assertEquals(ExamState.DESAPROBADO, CourseStatusCalculator.exams(sinRecu, List.of(p(1, 4))).get(0).state());
    }

    @Test
    void recuperatorioReemplazaYValeLaUltima() {
        assertEquals(new ExamResult(2, 4.0, 6.5, 6.5, ExamState.APROBADO),
                CourseStatusCalculator.exams(reglas(), List.of(r(2, 6.5), p(2, 4))).get(1));
        assertEquals(new ExamResult(1, 7.0, 5.0, 5.0, ExamState.DESAPROBADO),
                CourseStatusCalculator.exams(reglas(), List.of(p(1, 7), r(1, 5))).get(0));
        assertEquals(9.0, CourseStatusCalculator.exams(reglas(), List.of(p(1, 5), p(1, 9))).get(0).effective());
        assertEquals(new ExamResult(1, null, 7.0, 7.0, ExamState.APROBADO),
                CourseStatusCalculator.exams(reglas(), List.of(r(1, 7))).get(0));
    }
}
