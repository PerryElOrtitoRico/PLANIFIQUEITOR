<!-- modelo google/gemma-4-e4b-qat, temperatura 0.2, max_tokens 5908, razonamiento no -->

## system

Sos un programador Java 17 cuidadoso. Te paso archivos con métodos marcados `// TODO`: completá solo los cuerpos que nombra la tarea; los métodos marcados "lo completa otra subtarea" quedan exactamente como están.
Reglas:
- No cambies package, imports que ya están (aunque todavía no se usen), nombres de clases, firmas ni métodos que ya están hechos. Podés agregar imports y métodos private.
- Las clases de model se leen con getters: log.getMinutes(), rules.getPassGrade(). Los records de service se leen con x(): status.condition(), a.percent(). No existen getCondition() ni getPercent().
- No crees ni modifiques otros archivos.
- Devolvé cada archivo completo (nada de '...') en su propio bloque de código, con la ruta en la primera línea: un bloque por archivo, nunca dos archivos en el mismo bloque. Ejemplo:
```java
// src/main/java/com/planificador/service/Ejemplo.java
package com.planificador.service;

public final class Ejemplo { }
```
- Sin explicaciones fuera de los bloques.

## user

# Contrato del proyecto (extracto)

# PLANIFICADOR, etapa Pomodoro y cursada — Especificación (contrato)

## 1. Reglas de código
- Java 17 (el `pom.xml` compila con `source/target` 17), Maven. Paquete base `com.planificador`: `model` (datos), `service` (lógica y persistencia), `ui` (Swing). No toques `pom.xml` ni agregues dependencias.
- Nombres de clases y métodos en inglés, como el código que ya existe; comentarios en español.
- Las clases de `model` tienen getters: `log.getMinutes()`, `rules.getPassGrade()`, `ev.isRetake()`. Los **records** de `service` se leen con `x()`: `status.condition()`, `a.percent()`. No existen `getCondition()` ni `getPercent()`.
- La lógica no usa `LocalDate.now()`: la fecha de hoy se pasa por parámetro (así se testea).
- Tests con JUnit 5 (`org.junit.jupiter.api.*`); archivos con `@TempDir`. Ningún test lee ni escribe `~/.local/share/planificador-estudio`.

## 6. Cuenta regresiva
- En Hoy se muestran las fechas importantes de hoy a 30 días: "Primer parcial (Mecánica de Fluidos) en 6 días", "… es mañana", "… es hoy".

---

# T01 — Cuenta regresiva de las fechas importantes

## Objetivo
Completar `daysUntil`, `upcoming` y `text` de `Countdown` y los tres tests de `CountdownTest`.

## Código que ya existe
`ImportantDate` es una clase con getters: `d.getTitle()`, `d.getDate()` (texto `YYYY-MM-DD`), `d.getSubjectId()`. Constructor: `new ImportantDate(id, title, subjectId, date, type, notes)`.

## Métodos (pasos en orden)
- `daysUntil(isoDate, today)`:
  1. `if (isoDate == null) return null;`
  2. `try { LocalDate f = LocalDate.parse(isoDate); return ChronoUnit.DAYS.between(today, f); } catch (DateTimeParseException e) { return null; }`
- `upcoming(dates, today, maxDays)`:
  1. `List<ImportantDate> out = new ArrayList<>();`
  2. `for (ImportantDate d : dates) { Long n = daysUntil(d.getDate(), today); if (n != null && n >= 0 && n <= maxDays) out.add(d); }`
  3. `out.sort(Comparator.comparing((ImportantDate d) -> daysUntil(d.getDate(), today)).thenComparing(ImportantDate::getTitle));`
  4. `return out;`
- `text(date, subjectName, today)`:
  1. `String base = date.getTitle();`
  2. `if (subjectName != null && !subjectName.isBlank()) base = base + " (" + subjectName + ")";`
  3. `Long n = daysUntil(date.getDate(), today);`
  4. `if (n == null) return base;` · `if (n == 0) return base + " es hoy";` · `if (n == 1) return base + " es mañana";` · `return base + " en " + n + " días";`

## Tests (usan `HOY` = 2026-10-25 y `fecha(id, titulo, iso)` que ya están en el test)
- `cuentaLosDias`: `assertEquals(6L, Countdown.daysUntil("2026-10-31", HOY));` y `assertNull(Countdown.daysUntil("31/10/2026", HOY));`
- `proximasOrdenadasYSinLasViejas`: con `ImportantDate vieja = fecha("1", "Vieja", "2026-10-20"), lejos = fecha("2", "Lejos", "2026-12-31"), b = fecha("3", "B", "2026-10-28"), a = fecha("4", "A", "2026-10-26");` → `assertEquals(List.of(a, b), Countdown.upcoming(List.of(vieja, lejos, b, a), HOY, 30));`
- `textoSegunLosDias`: `assertEquals("Parcial (Física) en 6 días", Countdown.text(fecha("1", "Parcial", "2026-10-31"), "Física", HOY));`, `assertEquals("Parcial es mañana", Countdown.text(fecha("1", "Parcial", "2026-10-26"), null, HOY));` y `assertEquals("Parcial es hoy", Countdown.text(fecha("1", "Parcial", "2026-10-25"), null, HOY));`

---

# Archivos actuales

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
        // TODO T01
        throw new UnsupportedOperationException();
    }

    /** Fechas con 0 <= días <= maxDays, ordenadas por días y, a igual días, por título. Las fechas ilegibles se saltean. */
    public static List<ImportantDate> upcoming(List<ImportantDate> dates, LocalDate today, int maxDays) {
        // TODO T01
        throw new UnsupportedOperationException();
    }

    /** Texto para mostrar: "Primer parcial (Mecánica de Fluidos) en 6 días", "… es mañana", "… es hoy". */
    public static String text(ImportantDate date, String subjectName, LocalDate today) {
        // TODO T01
        throw new UnsupportedOperationException();
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
        // TODO T01
    }

    @Test
    void proximasOrdenadasYSinLasViejas() {
        // TODO T01
    }

    @Test
    void textoSegunLosDias() {
        // TODO T01
    }
}
```

Completá los `// TODO` que pide la tarea T01. Devolvé estos archivos completos, cada uno en su bloque: src/main/java/com/planificador/service/Countdown.java, src/test/java/com/planificador/service/CountdownTest.java.
