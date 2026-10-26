package com.planificador.service;

import com.planificador.model.CourseRules;
import com.planificador.model.CourseTracking;
import com.planificador.model.Evaluation;
import com.planificador.model.AttendanceRecord;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.planificador.service.CursadaDatos.*;
import static org.junit.jupiter.api.Assertions.*;

class T04cCondicionOcultoTest {

    static CourseStatus ev(CourseRules r, List<Evaluation> notas, List<AttendanceRecord> asist) {
        return CourseStatusCalculator.evaluate(new CourseTracking(r, notas, asist));
    }

    @Test
    void promocionYRegular() {
        CourseStatus promo = ev(reglas(), List.of(p(1, 8), p(2, 9)), asist(14, 2));
        assertEquals(CourseCondition.PROMOCION, promo.condition());
        assertTrue(promo.canPromote());
        assertTrue(promo.canRegularize());
        assertEquals(2, promo.exams().size());
        assertEquals(2, promo.attendance().remainingAbsences());
        CourseStatus regular = ev(reglas(), List.of(p(1, 7), p(2, 9)), asist(16, 0));
        assertEquals(CourseCondition.REGULAR, regular.condition());
        assertFalse(regular.canPromote());
        CourseStatus conRecu = ev(reglas(), List.of(p(1, 5), r(1, 8), p(2, 9)), asist(16, 0));
        assertEquals(CourseCondition.PROMOCION, conRecu.condition());
    }

    @Test
    void libre() {
        CourseStatus faltas = ev(reglas(), List.of(p(1, 8), p(2, 9)), asist(11, 5));
        assertEquals(CourseCondition.LIBRE, faltas.condition());
        assertFalse(faltas.canRegularize());
        assertFalse(faltas.canPromote());
        assertEquals(CourseCondition.LIBRE, ev(reglas(), List.of(p(1, 8), p(2, 3), r(2, 4)), asist(3, 0)).condition());
    }

    @Test
    void enCurso() {
        CourseStatus faltanClases = ev(reglas(), List.of(p(1, 8), p(2, 9)), asist(10, 0));
        assertEquals(CourseCondition.EN_CURSO, faltanClases.condition());
        assertTrue(faltanClases.canPromote());
        CourseStatus aRecuperar = ev(reglas(), List.of(p(1, 8), p(2, 4)), asist(16, 0));
        assertEquals(CourseCondition.EN_CURSO, aRecuperar.condition());
        assertTrue(aRecuperar.canPromote());
        CourseStatus regularSegura = ev(reglas(), List.of(p(1, 6)), asist(5, 0));
        assertEquals(CourseCondition.EN_CURSO, regularSegura.condition());
        assertFalse(regularSegura.canPromote());
        assertTrue(regularSegura.canRegularize());
    }

    @Test
    void sinPromocionOSinClasesConocidas() {
        CourseRules sinPromo = new CourseRules(75, 4, 0, 2, List.of(true, true), 16);
        CourseStatus s = ev(sinPromo, List.of(p(1, 10), p(2, 10)), asist(16, 0));
        assertEquals(CourseCondition.REGULAR, s.condition());
        assertFalse(s.canPromote());
        CourseRules sinClases = new CourseRules(75, 6, 8, 1, List.of(true), 0);
        assertEquals(CourseCondition.PROMOCION, ev(sinClases, List.of(p(1, 9)), asist(3, 1)).condition());
    }
}
