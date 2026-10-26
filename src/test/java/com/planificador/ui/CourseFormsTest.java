package com.planificador.ui;

import com.planificador.model.*;
import com.planificador.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CourseFormsTest {

    static final LocalDate HOY = LocalDate.of(2026, 10, 25);

    @TempDir
    Path tmp;

    @Test
    void reglasDelFormulario() throws Exception {
        CourseRulesForm f = new CourseRulesForm(new CourseRules(80, 6, 8, 3, List.of(true, false, true), 14));
        CourseRules r = f.toRules();
        assertEquals(80, r.getMinAttendancePercent(), 1e-9);
        assertEquals(3, r.getExamCount());
        assertEquals(List.of(true, false, true), r.getRetakes());
        assertEquals(14, r.getPlannedClasses());
    }

    @Test
    void notaDelFormulario() throws Exception {
        EvaluationForm f = new EvaluationForm();
        f.numberSpinner.setValue(2);
        f.retakeCheck.setSelected(true);
        f.gradeSpinner.setValue(7.5);
        f.dateField.setText("2026-10-01");
        Evaluation e = f.toEvaluation("e1");
        assertEquals(Evaluation.PARCIAL, e.getKind());
        assertEquals(2, e.getNumber());
        assertTrue(e.isRetake());
        assertEquals(7.5, e.getGrade(), 1e-9);
        assertEquals("2026-10-01", e.getDate());
        f.dateField.setText("1/10");
        assertThrows(IllegalArgumentException.class, () -> f.toEvaluation("e2"));
    }
}
