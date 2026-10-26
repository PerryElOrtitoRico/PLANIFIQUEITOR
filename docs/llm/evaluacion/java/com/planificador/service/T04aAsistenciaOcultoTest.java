package com.planificador.service;

import com.planificador.model.CourseRules;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.planificador.service.CursadaDatos.*;
import static org.junit.jupiter.api.Assertions.*;

class T04aAsistenciaOcultoTest {

    @Test
    void conClasesConocidas() {
        assertEquals(new AttendanceSummary(10, 8, 2, 80.0, false, true, 2),
                CourseStatusCalculator.attendance(reglas(), asist(8, 2)));
        assertEquals(new AttendanceSummary(9, 4, 5, 400.0 / 9, true, true, -1),
                CourseStatusCalculator.attendance(reglas(), asist(4, 5)));
        assertEquals(new AttendanceSummary(0, 0, 0, 100.0, false, true, 4),
                CourseStatusCalculator.attendance(reglas(), List.of()));
    }

    @Test
    void redondeaLasFaltasPermitidasParaAbajo() {
        CourseRules r = new CourseRules(80, 6, 8, 2, List.of(true, true), 18); // 18 * 0.2 = 3.6 → 3
        assertEquals(3, CourseStatusCalculator.attendance(r, List.of()).remainingAbsences());
        CourseRules r2 = new CourseRules(70, 6, 8, 2, List.of(true, true), 10); // 3 justas
        assertEquals(0, CourseStatusCalculator.attendance(r2, asist(5, 3)).remainingAbsences());
    }

    @Test
    void sinClasesConocidas() {
        CourseRules r = new CourseRules(75, 6, 8, 2, List.of(true, true), 0);
        AttendanceSummary justo = CourseStatusCalculator.attendance(r, asist(3, 1));
        assertFalse(justo.failed());
        assertFalse(justo.remainingKnown());
        assertEquals(0, justo.remainingAbsences());
        assertTrue(CourseStatusCalculator.attendance(r, asist(2, 1)).failed());
        assertFalse(CourseStatusCalculator.attendance(r, List.of()).failed());
    }
}
