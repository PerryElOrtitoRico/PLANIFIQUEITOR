package com.planificador.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.table.DefaultTableModel;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class T08StudyTimePanelOcultoTest {

    @TempDir
    Path tmp;

    @Test
    void semana() throws Exception {
        UiDatos.cargar(tmp);
        StudyTimePanel p = new StudyTimePanel(null);
        DefaultTableModel m = p.buildWeekModel(LocalDate.of(2026, 10, 19));
        assertEquals(4, m.getColumnCount());
        assertEquals("Materia", m.getColumnName(0));
        assertEquals("Avance", m.getColumnName(3));
        assertEquals(2, m.getRowCount());
        assertArrayEquals(new Object[]{"Mecánica de Fluidos", "2 h", "50 min", "41 %"}, fila(m, 0));
        assertArrayEquals(new Object[]{"Física II", "1 h 30 min", "0 min", "0 %"}, fila(m, 1));
        assertFalse(m.isCellEditable(0, 0));
    }

    @Test
    void historial() throws Exception {
        UiDatos.cargar(tmp);
        StudyTimePanel p = new StudyTimePanel(null);
        DefaultTableModel m = p.buildHistoryModel(LocalDate.of(2026, 10, 19), 3);
        assertEquals(3, m.getRowCount());
        assertEquals("Semana", m.getColumnName(0));
        assertEquals("Física II", m.getColumnName(2));
        assertArrayEquals(new Object[]{"05/10", "0 min", "0 min", "0 min"}, fila(m, 0));
        assertArrayEquals(new Object[]{"12/10", "0 min", "50 min", "50 min"}, fila(m, 1));
        assertArrayEquals(new Object[]{"19/10", "50 min", "0 min", "50 min"}, fila(m, 2));
    }

    static Object[] fila(DefaultTableModel m, int r) {
        Object[] f = new Object[m.getColumnCount()];
        for (int c = 0; c < f.length; c++) f[c] = m.getValueAt(r, c);
        return f;
    }
}
