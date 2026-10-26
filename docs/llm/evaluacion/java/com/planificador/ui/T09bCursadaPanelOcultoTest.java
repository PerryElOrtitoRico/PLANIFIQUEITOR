package com.planificador.ui;

import com.planificador.model.AttendanceRecord;
import com.planificador.model.CourseRules;
import com.planificador.model.CourseTracking;
import com.planificador.model.Evaluation;
import com.planificador.model.Subject;
import com.planificador.service.CourseStatusCalculator;
import com.planificador.service.DataManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.table.DefaultTableModel;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class T09bCursadaPanelOcultoTest {

    @TempDir
    Path tmp;

    @Test
    void textos() {
        Subject s = new Subject("s9", "Química", "#000000", "", "");
        assertEquals(List.of("Todavía no cargaste las reglas de cursada: tocá «Reglas de cursada»."), CoursePanel.statusLines(s));
        CourseTracking t = s.getOrCreateCourse();
        t.setRules(new CourseRules(75, 6, 8, 1, List.of(true), 2));
        t.getEvaluations().add(new Evaluation("e", Evaluation.PARCIAL, 1, false, 9, null));
        t.getAttendance().add(new AttendanceRecord("2026-08-10", true));
        t.getAttendance().add(new AttendanceRecord("2026-08-17", true));
        List<String> lineas = CoursePanel.statusLines(s);
        assertEquals("Condición: Promoción", lineas.get(0));
        assertEquals(CourseStatusCalculator.describe(CourseStatusCalculator.evaluate(t), t.getRules()), lineas);
    }

    @Test
    void tablas() {
        CourseTracking t = new CourseTracking();
        t.getEvaluations().add(new Evaluation("a", Evaluation.PARCIAL, 1, false, 4, "2026-09-01"));
        t.getEvaluations().add(new Evaluation("b", Evaluation.PARCIAL, 1, true, 6.5, "2026-09-20"));
        t.getEvaluations().add(new Evaluation("c", Evaluation.TP, 2, false, 10, null));
        DefaultTableModel m = CoursePanel.evaluationsModel(t);
        assertEquals(List.of("Tipo", "N.º", "Recuperatorio", "Nota", "Fecha"),
                List.of(m.getColumnName(0), m.getColumnName(1), m.getColumnName(2), m.getColumnName(3), m.getColumnName(4)));
        assertEquals(3, m.getRowCount());
        assertEquals(1, m.getValueAt(0, 1));
        assertEquals("Sí", m.getValueAt(1, 2));
        assertEquals("6,5", m.getValueAt(1, 3));
        assertEquals("TP", m.getValueAt(2, 0));
        assertEquals("—", m.getValueAt(2, 2));
        assertEquals("10", m.getValueAt(2, 3));
        assertEquals("", m.getValueAt(2, 4));
        t.getAttendance().add(new AttendanceRecord("2026-08-10", true));
        t.getAttendance().add(new AttendanceRecord("2026-08-17", false));
        DefaultTableModel a = CoursePanel.attendanceModel(t);
        assertEquals("Fecha", a.getColumnName(0));
        assertEquals("2026-08-17", a.getValueAt(1, 0));
        assertEquals("Presente", a.getValueAt(0, 1));
        assertEquals("Ausente", a.getValueAt(1, 1));
    }

    @Test
    void asistenciaYPanelEntero() throws Exception {
        AttendanceRecord r = CoursePanel.attendanceFrom("2026-10-25", false);
        assertEquals("2026-10-25", r.getDate());
        assertFalse(r.isPresent());
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> CoursePanel.attendanceFrom("ayer", true));
        assertEquals("Fecha inválida: usá AAAA-MM-DD", e.getMessage());
        DataManager dm = UiDatos.cargar(tmp);
        CoursePanel p = new CoursePanel(null);
        assertEquals(2, p.subjectNames.size());
        assertEquals("Mecánica de Fluidos", p.conditionLabel.getText());
        assertEquals(1, p.statusPanel.getComponentCount());
        dm.getSubjects().get(0).getOrCreateCourse().getEvaluations().add(new Evaluation("e", Evaluation.PARCIAL, 1, false, 8, null));
        p.refresh();
        assertTrue(p.statusPanel.getComponentCount() > 1);
        assertEquals(1, p.evaluationsTable.getModel().getRowCount());
    }
}
