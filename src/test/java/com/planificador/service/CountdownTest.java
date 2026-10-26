package com.planificador.service;

import com.planificador.model.ImportantDate;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CountdownTest {

    static final LocalDate HOY = LocalDate.of(2026, 10, 25);

    static ImportantDate fecha(String id, String titulo, String iso) {
        return new ImportantDate(id, titulo, "s1", iso, "parcial", "");
    }

    @Test
    void cuentaLosDias() {
        // HOY = 2026-10-25
        // 2026-10-31 -> 6 días
        assertEquals(6L, Countdown.daysUntil("2026-10-31", HOY));
        // Formato incorrecto
        assertNull(Countdown.daysUntil("31/10/2026", HOY));
        // Fecha pasada (debería ser negativo, pero el test solo verifica el parse/null)
        assertEquals(-1, Countdown.daysUntil("2026-10-24", HOY));
    }

    @Test
    void proximasOrdenadasYSinLasViejas() {
        // HOY = 2026-10-25
        // vieja: 2026-10-20 (Pasada, n=-5)
        // lejos: 2026-12-31 (n=67)
        // b: 2026-10-28 (n=3)
        // a: 2026-10-26 (n=1)
        // maxDays = 30
        // Esperados: a (1 día), b (3 días). Orden: a, b.
        ImportantDate vieja = fecha("1", "Vieja", "2026-10-20");
        ImportantDate lejos = fecha("2", "Lejos", "2026-12-31");
        ImportantDate b = fecha("3", "B", "2026-10-28");
        ImportantDate a = fecha("4", "A", "2026-10-26");

        List<ImportantDate> input = List.of(vieja, lejos, b, a);
        List<ImportantDate> expected = List.of(a, b);

        List<ImportantDate> result = Countdown.upcoming(input, HOY, 30);
        assertEquals(expected, result);
    }

    @Test
    void textoSegunLosDias() {
        // HOY = 2026-10-25
        // 2026-10-31 -> 6 días
        ImportantDate p6 = fecha("1", "Parcial", "2026-10-31");
        assertEquals("Parcial en 6 días", Countdown.text(p6, null, HOY));

        // 2026-10-26 -> 1 día (mañana)
        ImportantDate p1 = fecha("1", "Parcial", "2026-10-26");
        assertEquals("Parcial es mañana", Countdown.text(p1, null, HOY));

        // 2026-10-25 -> 0 días (hoy)
        ImportantDate p0 = fecha("1", "Parcial", "2026-10-25");
        assertEquals("Parcial es hoy", Countdown.text(p0, null, HOY));

        // Con sujeto
        ImportantDate p6_subj = fecha("1", "Parcial", "2026-10-31");
        assertEquals("Parcial (Física) en 6 días", Countdown.text(p6_subj, "Física", HOY));

        // Sin sujeto (base solo)
        ImportantDate p0_no_subj = fecha("1", "Parcial", "2026-10-25");
        assertEquals("Parcial es hoy", Countdown.text(p0_no_subj, null, HOY));
    }
}
