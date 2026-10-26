package com.planificador.service;

import com.planificador.model.ImportantDate;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class T01CountdownOcultoTest {

    static final LocalDate HOY = LocalDate.of(2026, 10, 25);

    static ImportantDate f(String id, String titulo, String iso) {
        return new ImportantDate(id, titulo, "s1", iso, "parcial", "");
    }

    @Test
    void diasHastaLaFecha() {
        assertEquals(6L, Countdown.daysUntil("2026-10-31", HOY));
        assertEquals(0L, Countdown.daysUntil("2026-10-25", HOY));
        assertEquals(-5L, Countdown.daysUntil("2026-10-20", HOY));
        assertEquals(68L, Countdown.daysUntil("2027-01-01", HOY));
    }

    @Test
    void fechaIlegibleONulaEsNull() {
        assertNull(Countdown.daysUntil("31/10/2026", HOY));
        assertNull(Countdown.daysUntil(null, HOY));
    }

    @Test
    void proximasFiltradasYOrdenadas() {
        ImportantDate vieja = f("1", "Vieja", "2026-10-24");
        ImportantDate hoy = f("2", "Hoy", "2026-10-25");
        ImportantDate lejos = f("3", "Lejos", "2026-11-25"); // 31 días
        ImportantDate justo = f("4", "Justo", "2026-11-24");  // 30 días
        ImportantDate rota = f("5", "Rota", "mañana");
        ImportantDate b = f("6", "B parcial", "2026-10-31");
        ImportantDate a = f("7", "A parcial", "2026-10-31");
        List<ImportantDate> r = Countdown.upcoming(List.of(vieja, hoy, lejos, justo, rota, b, a), HOY, 30);
        assertEquals(List.of(hoy, a, b, justo), r);
    }

    @Test
    void textos() {
        assertEquals("Primer parcial (Mecánica de Fluidos) en 6 días",
                Countdown.text(f("1", "Primer parcial", "2026-10-31"), "Mecánica de Fluidos", HOY));
        assertEquals("Entrega TP es mañana", Countdown.text(f("1", "Entrega TP", "2026-10-26"), null, HOY));
        assertEquals("Final es hoy", Countdown.text(f("1", "Final", "2026-10-25"), " ", HOY));
        assertEquals("Final (Física)", Countdown.text(f("1", "Final", "sin fecha"), "Física", HOY));
    }
}
