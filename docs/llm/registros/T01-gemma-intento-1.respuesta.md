```java
// src/main/java/com/planificador/service/Countdown.java
package com.planificador.service;

import com.planificador.model.ImportantDate;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Cuenta regresiva de las fechas importantes para la pantalla de Hoy. La fecha de hoy se pasa por parámetro. */
public final class Countdown {

    private Countdown() { }

    /** Días desde today hasta la fecha ISO (YYYY-MM-DD); negativo si ya pasó; null si la fecha es null o no se puede leer. */
    public static Long daysUntil(String isoDate, LocalDate today) {
        if (isoDate == null) return null;
        try {
            LocalDate f = LocalDate.parse(isoDate);
            return ChronoUnit.DAYS.between(today, f);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Fechas con 0 <= días <= maxDays, ordenadas por días y, a igual días, por título. Las fechas ilegibles se saltean. */
    public static List<ImportantDate> upcoming(List<ImportantDate> dates, LocalDate today, int maxDays) {
        List<ImportantDate> out = new ArrayList<>();
        for (ImportantDate d : dates) {
            Long n = daysUntil(d.getDate(), today);
            if (n != null && n >= 0 && n <= maxDays) {
                out.add(d);
            }
        }
        out.sort(Comparator.comparing((ImportantDate d) -> daysUntil(d.getDate(), today)).thenComparing(ImportantDate::getTitle));
        return out;
    }

    /** Texto para mostrar: "Primer parcial (Mecánica de Fluidos) en 6 días", "… es mañana", "… es hoy". */
    public static String text(ImportantDate date, String subjectName, LocalDate today) {
        String base = date.getTitle();
        if (subjectName != null && !subjectName.isBlank()) {
            base = base + " (" + subjectName + ")";
        }
        Long n = daysUntil(date.getDate(), today);
        if (n == null) return base;
        if (n == 0) return base + " es hoy";
        if (n == 1) return base + " es mañana";
        return base + " en " + n + " días";
    }
}
```

```java
// src/test/java/com/planificador/service/CountdownTest.java
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
```

<!-- 109 s · entrada 2078 · salida 1611 · finish_reason stop -->
