package com.planificador.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.*;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class T06CountdownPanelOcultoTest {

    static final LocalDate HOY = LocalDate.of(2026, 10, 25);

    @TempDir
    Path tmp;

    @Test
    void textosOrdenadosYColores() throws Exception {
        UiDatos.cargar(tmp);
        CountdownPanel p = new CountdownPanel();
        p.refresh(HOY);
        List<String> textos = p.labels().stream().map(JLabel::getText).toList();
        assertEquals(List.of("⏳ Charla es hoy", "⏳ Entrega TP (Física II) es mañana",
                "⏳ Primer parcial (Mecánica de Fluidos) en 6 días", "⏳ Coloquio (Física II) en 10 días"), textos);
        assertEquals(CountdownPanel.URGENTE, p.labels().get(0).getForeground());
        assertEquals(CountdownPanel.URGENTE, p.labels().get(2).getForeground());
        assertEquals(CountdownPanel.NORMAL, p.labels().get(3).getForeground());
    }

    @Test
    void refrescarNoDuplicaYSinFechasLoDice() throws Exception {
        UiDatos.cargar(tmp);
        CountdownPanel p = new CountdownPanel();
        p.refresh(HOY);
        p.refresh(HOY);
        assertEquals(4, p.labels().size());
        p.refresh(LocalDate.of(2027, 6, 1));
        assertEquals(List.of("Sin fechas importantes en los próximos 30 días"),
                p.labels().stream().map(JLabel::getText).toList());
    }
}
