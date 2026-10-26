package com.planificador.ui;

import com.planificador.model.*;
import com.planificador.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StudyTimePanelTest {

    static final LocalDate HOY = LocalDate.of(2026, 10, 25);

    @TempDir
    Path tmp;

    @Test
    void tablaDeLaSemana() throws Exception {
        UiDatos.cargar(tmp);
        StudyTimePanel p = new StudyTimePanel(null);
        var m = p.buildWeekModel(LocalDate.of(2026, 10, 19));
        assertEquals(2, m.getRowCount());
        assertEquals("Mecánica de Fluidos", m.getValueAt(0, 0));
        assertEquals("2 h", m.getValueAt(0, 1));
        assertEquals("50 min", m.getValueAt(0, 2));
        assertEquals("41 %", m.getValueAt(0, 3));
    }

    @Test
    void tablaDeLasUltimasSemanas() throws Exception {
        UiDatos.cargar(tmp);
        StudyTimePanel p = new StudyTimePanel(null);
        var m = p.buildHistoryModel(LocalDate.of(2026, 10, 19), 2);
        assertEquals(4, m.getColumnCount());
        assertEquals("Total", m.getColumnName(3));
        assertEquals("12/10", m.getValueAt(0, 0));
        assertEquals("50 min", m.getValueAt(0, 2));
        assertEquals("50 min", m.getValueAt(1, 3));
    }
}
