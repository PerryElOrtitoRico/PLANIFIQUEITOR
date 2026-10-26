package com.planificador.ui;

import com.planificador.model.CourseRules;
import com.planificador.model.Evaluation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class T09aFormulariosOcultoTest {

    @Test
    void reglas() {
        CourseRules r = new CourseRulesForm(new CourseRules(80, 4, 0, 3, List.of(true, false, true), 14)).toRules();
        assertEquals(80, r.getMinAttendancePercent(), 1e-9);
        assertEquals(4, r.getPassGrade(), 1e-9);
        assertEquals(0, r.getPromotionGrade(), 1e-9);
        assertEquals(3, r.getExamCount());
        assertEquals(List.of(true, false, true), r.getRetakes());
        assertEquals(14, r.getPlannedClasses());
    }

    @Test
    void cambiarCantidadDeParciales() {
        CourseRulesForm f = new CourseRulesForm(new CourseRules());
        f.examCount.setValue(3);
        f.retakeBoxes.get(1).setSelected(false);
        f.passGrade.setValue(6.5);
        CourseRules r = f.toRules();
        assertEquals(List.of(true, false, true), r.getRetakes());
        assertEquals(6.5, r.getPassGrade(), 1e-9);
        f.examCount.setValue(1);
        assertEquals(List.of(true), f.toRules().getRetakes());
    }

    @Test
    void notas() {
        EvaluationForm f = new EvaluationForm();
        f.numberSpinner.setValue(2);
        f.retakeCheck.setSelected(true);
        f.gradeSpinner.setValue(4.25);
        f.dateField.setText(" 2026-10-01 ");
        Evaluation e = f.toEvaluation("x");
        assertEquals("x", e.getId());
        assertEquals(Evaluation.PARCIAL, e.getKind());
        assertEquals(2, e.getNumber());
        assertTrue(e.isRetake());
        assertEquals(4.25, e.getGrade(), 1e-9);
        assertEquals("2026-10-01", e.getDate());
        f.kindCombo.setSelectedIndex(1);
        f.retakeCheck.setSelected(true);
        f.dateField.setText("");
        Evaluation tp = f.toEvaluation("y");
        assertEquals(Evaluation.TP, tp.getKind());
        assertFalse(tp.isRetake());
        assertNull(tp.getDate());
        f.dateField.setText("2026-13-01");
        assertThrows(IllegalArgumentException.class, () -> f.toEvaluation("z"));
    }
}
