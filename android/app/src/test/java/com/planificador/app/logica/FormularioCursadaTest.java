package com.planificador.app.logica;

import com.planificador.model.AttendanceRecord;
import com.planificador.model.CourseRules;
import com.planificador.model.Evaluation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FormularioCursadaTest {

    @Test
    void reglasConLosValoresDelFormulario() {
        CourseRules r = FormularioCursada.reglas("80", "6", "8,5", "3", List.of(true, false), "16");
        assertEquals(80, r.getMinAttendancePercent(), 1e-9);
        assertEquals(8.5, r.getPromotionGrade(), 1e-9);
        assertEquals(3, r.getExamCount());
        assertEquals(List.of(true, false, true), r.getRetakes(), "el parcial nuevo arranca con recuperatorio, como la PC");
        assertEquals(16, r.getPlannedClasses());
    }

    @Test
    void recuperatoriosSeAjustanALaCantidadDeParciales() {
        assertEquals(List.of(true), FormularioCursada.recuperatorios(1, List.of(true, false)));
        assertEquals(List.of(false, true), FormularioCursada.recuperatorios(2, List.of(false)));
    }

    @Test
    void reglasFueraDeRangoSeRechazan() {
        assertThrows(IllegalArgumentException.class, () -> FormularioCursada.reglas("120", "6", "8", "2", List.of(), "0"));
        assertThrows(IllegalArgumentException.class, () -> FormularioCursada.reglas("75", "11", "8", "2", List.of(), "0"));
        assertThrows(IllegalArgumentException.class, () -> FormularioCursada.reglas("75", "6", "8", "x", List.of(), "0"));
    }

    @Test
    void notaDeUnParcialYDeUnTp() {
        Evaluation e = FormularioCursada.nota("ev1", true, "2", true, "7,25", "2026-11-09");
        assertEquals(Evaluation.PARCIAL, e.getKind());
        assertEquals(2, e.getNumber());
        assertTrue(e.isRetake());
        assertEquals(7.25, e.getGrade(), 1e-9);
        assertEquals("2026-11-09", e.getDate());
        Evaluation tp = FormularioCursada.nota("ev2", false, "1", true, "9", "");
        assertEquals(Evaluation.TP, tp.getKind());
        assertFalse(tp.isRetake(), "un TP no tiene recuperatorio");
        assertNull(tp.getDate());
    }

    @Test
    void notaInvalidaSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> FormularioCursada.nota("e", true, "1", false, "10,5", ""));
        assertThrows(IllegalArgumentException.class, () -> FormularioCursada.nota("e", true, "1", false, "7", "9/11/2026"));
        assertThrows(IllegalArgumentException.class, () -> FormularioCursada.nota("e", true, "0", false, "7", ""));
    }

    @Test
    void asistenciaConFechaValida() {
        AttendanceRecord a = FormularioCursada.asistencia("2026-11-09", false);
        assertEquals("2026-11-09", a.getDate());
        assertFalse(a.isPresent());
        assertThrows(IllegalArgumentException.class, () -> FormularioCursada.asistencia("ayer", true));
    }
}
