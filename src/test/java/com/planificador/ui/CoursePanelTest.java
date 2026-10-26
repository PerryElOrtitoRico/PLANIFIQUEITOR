package com.planificador.ui;

import com.planificador.model.*;
import com.planificador.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CoursePanelTest {

    static final LocalDate HOY = LocalDate.of(2026, 10, 25);

    @TempDir
    Path tmp;

    @Test
    void textosYTablas() throws Exception {
        Subject s = new Subject("s1", "Física II", "#000000", "", "");
        assertEquals(List.of("Todavía no cargaste las reglas de cursada: tocá «Reglas de cursada»."), CoursePanel.statusLines(s));

        CourseTracking t = s.getOrCreateCourse();
        t.getEvaluations().add(new Evaluation("e1", Evaluation.PARCIAL, 1, false, 7, "2026-09-01"));
        t.getEvaluations().add(new Evaluation("e2", Evaluation.TP, 1, false, 8.5, null));
        t.getAttendance().add(new AttendanceRecord("2026-08-10", true));

        assertEquals("Condición: En curso", CoursePanel.statusLines(s).get(0));

        var m = CoursePanel.evaluationsModel(t);
        assertEquals("Parcial", m.getValueAt(0, 0));
        assertEquals("No", m.getValueAt(0, 2));
        assertEquals("8,5", m.getValueAt(1, 3));
        assertEquals("", m.getValueAt(1, 4));

        assertEquals("Presente", CoursePanel.attendanceModel(t).getValueAt(0, 1));
    }

    @Test
    void asistenciaConFechaMala() throws Exception {
        assertEquals("2026-10-25", CoursePanel.attendanceFrom(" 2026-10-25 ", true).getDate());
        assertThrows(IllegalArgumentException.class, () -> CoursePanel.attendanceFrom("25/10", true));
    }
}
