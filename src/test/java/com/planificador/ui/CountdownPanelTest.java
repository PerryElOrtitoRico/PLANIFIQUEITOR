package com.planificador.ui;

import com.planificador.model.*;
import com.planificador.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CountdownPanelTest {

    static final LocalDate HOY = LocalDate.of(2026, 10, 25);

    @TempDir
    Path tmp;

    @Test
    void muestraLasProximas() throws Exception {
        UiDatos.cargar(tmp);
        CountdownPanel p = new CountdownPanel();
        p.refresh(HOY);
        assertEquals(4, p.labels().size());
        assertEquals("⏳ Charla es hoy", p.labels().get(0).getText());
        assertEquals("⏳ Primer parcial (Mecánica de Fluidos) en 6 días", p.labels().get(2).getText());
        assertEquals(CountdownPanel.NORMAL, p.labels().get(3).getForeground());
    }

    @Test
    void sinFechas() throws Exception {
        UiDatos.cargar(tmp);
        CountdownPanel p = new CountdownPanel();
        p.refresh(LocalDate.of(2027, 6, 1));
        assertEquals(1, p.labels().size());
        assertEquals("Sin fechas importantes en los próximos 30 días", p.labels().get(0).getText());
    }
}
