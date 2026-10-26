<!-- modelo qwen/qwen3.5-9b, temperatura 0.2, max_tokens 3435, razonamiento no -->

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

## 4. Tiempo por materia
- Semana = de lunes a domingo. Estudiado = suma de `StudyLog.minutes` con fecha en esa semana. Planeado = suma de `StudySession.durationMinutes` de la guía semanal (la guía es la misma todas las semanas).

---

# T02b — Tiempo por materia: comparar, por semana y formato

## Objetivo
Completar **solo** `compareWeek`, `minutesByWeek` y `formatMinutes` de `StudyTimeStats` y los tests `comparaPlaneadoYEstudiado`, `minutosPorSemana` y `formatoDeMinutos`. `weekStart`, `minutesBySubject` y `plannedMinutesBySubject` ya están hechos: usalos.

## Código que ya existe
- `StudyLog` (clase con getters): `log.getSubjectId()`, `log.getDate()` (texto `YYYY-MM-DD`, puede ser null), `log.getMinutes()`.
- `SubjectWeekTime` es un record: `new SubjectWeekTime(subjectId, plannedMinutes, actualMinutes)`; se lee con `t.plannedMinutes()`.

## Métodos (pasos en orden)
- `compareWeek(subjectIds, logs, guide, weekStart)`:
  1. `Map<String, Integer> planned = plannedMinutesBySubject(guide); Map<String, Integer> actual = minutesBySubject(logs, weekStart);`
  2. `List<SubjectWeekTime> out = new ArrayList<>();`
  3. `for (String id : subjectIds) out.add(new SubjectWeekTime(id, planned.getOrDefault(id, 0), actual.getOrDefault(id, 0)));`
  4. `return out;`
- `minutesByWeek(logs, subjectId)`:
  1. `SortedMap<LocalDate, Integer> out = new TreeMap<>();`
  2. `for (StudyLog log : logs) {`
  3. `  if (subjectId != null && !subjectId.equals(log.getSubjectId())) continue;`
  4. `  if (log.getDate() == null) continue;`
  5. `  LocalDate f; try { f = LocalDate.parse(log.getDate()); } catch (DateTimeParseException e) { continue; }`
  6. `  out.merge(weekStart(f), log.getMinutes(), Integer::sum);`
  7. `}` y `return out;`
- `formatMinutes(minutes)`:
  1. `int h = minutes / 60; int m = minutes % 60;`
  2. `if (h == 0) return m + " min";` · `if (m == 0) return h + " h";` · `return h + " h " + m + " min";`

## Tests (usan `log(materia, fecha, minutos)` y `plan(materia, minutos)` que ya están en el test)
- `comparaPlaneadoYEstudiado`: `assertEquals(List.of(new SubjectWeekTime("s1", 60, 25), new SubjectWeekTime("s2", 0, 0)), StudyTimeStats.compareWeek(List.of("s1", "s2"), List.of(log("s1", "2026-10-20", 25)), List.of(plan("s1", 60)), LocalDate.of(2026, 10, 19)));`
- `minutosPorSemana` (el test necesita agregar `import java.util.SortedMap;` debajo de `import java.util.Map;`; 2026-10-12 y 2026-10-19 son **lunes**): con `List.of(log("s1", "2026-10-12", 25), log("s1", "2026-10-20", 25), log("s2", "2026-10-20", 50))` → `StudyTimeStats.minutesByWeek(logs, "s1")` tiene `get(LocalDate.of(2026, 10, 12))` = 25 y `get(LocalDate.of(2026, 10, 19))` = 25; y `StudyTimeStats.minutesByWeek(logs, null).get(LocalDate.of(2026, 10, 19))` = 75.
- `formatoDeMinutos`: `"45 min"` para 45, `"2 h"` para 120, `"1 h 30 min"` para 90.

---

# Archivos actuales

```java
// src/main/java/com/planificador/service/StudyTimeStats.java
package com.planificador.service;

import com.planificador.model.StudyLog;
import com.planificador.model.StudySession;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/** Horas de estudio por materia y por semana, a partir de los StudyLog (Pomodoro) y la guía semanal (StudySession). */
public final class StudyTimeStats {

    private StudyTimeStats() { }

    /** Lunes de la semana de d (si d es lunes, d). */
    public static LocalDate weekStart(LocalDate d) {
        return d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** Minutos estudiados por subjectId en la semana que empieza en weekStart (de weekStart a weekStart + 6 días). */
    public static Map<String, Integer> minutesBySubject(List<StudyLog> logs, LocalDate weekStart) {
        Map<String, Integer> out = new HashMap<>();
        LocalDate end = weekStart.plusDays(7);
        for (StudyLog log : logs) {
            if (log.getDate() == null) continue;
            LocalDate f;
            try {
                f = LocalDate.parse(log.getDate());
            } catch (DateTimeParseException e) {
                continue;
            }
            // Check if f is within [weekStart, end)
            if (!f.isBefore(weekStart) && f.isBefore(end)) {
                out.merge(log.getSubjectId(), log.getMinutes(), Integer::sum);
            }
        }
        return out;
    }

    /** Minutos planeados por subjectId en la guía semanal: suma de durationMinutes. */
    public static Map<String, Integer> plannedMinutesBySubject(List<StudySession> guide) {
        Map<String, Integer> out = new HashMap<>();
        for (StudySession s : guide) {
            if (s.getSubjectId() == null) continue;
            out.merge(s.getSubjectId(), s.getDurationMinutes(), Integer::sum);
        }
        return out;
    }

    /** Planeado contra estudiado de cada materia de subjectIds, en ese orden, para la semana que empieza en weekStart. */
    public static List<SubjectWeekTime> compareWeek(List<String> subjectIds, List<StudyLog> logs,
                                                    List<StudySession> guide, LocalDate weekStart) {
        Map<String, Integer> planned = plannedMinutesBySubject(guide);
        Map<String, Integer> actual = minutesBySubject(logs, weekStart);
        List<SubjectWeekTime> out = new ArrayList<>();
        for (String id : subjectIds) {
            out.add(new SubjectWeekTime(id, planned.getOrDefault(id, 0), actual.getOrDefault(id, 0)));
        }
        return out;
    }

    /** Minutos por semana (clave: lunes de la semana) de una materia; subjectId null = todas las materias. */
    public static SortedMap<LocalDate, Integer> minutesByWeek(List<StudyLog> logs, String subjectId) {
        SortedMap<LocalDate, Integer> out = new TreeMap<>();
        for (StudyLog log : logs) {
            if (subjectId != null && !subjectId.equals(log.getSubjectId())) continue;
            if (log.getDate() == null) continue;
            LocalDate f;
            try {
                f = LocalDate.parse(log.getDate());
            } catch (DateTimeParseException e) {
                continue;
            }
            out.merge(weekStart(f), log.getMinutes(), Integer::sum);
        }
        return out;
    }

    /** "45 min", "2 h", "1 h 30 min". */
    public static String formatMinutes(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        if (h == 0) return m + " min";
        if (m == 0) return h + " h";
        return h + " h " + m + " min";
    }
}
```

```java
// src/test/java/com/planificador/service/StudyTimeStatsTest.java
package com.planificador.service;

import com.planificador.model.StudyLog;
import com.planificador.model.StudySession;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import static org.junit.jupiter.api.Assertions.*;

class StudyTimeStatsTest {

    static StudyLog log(String materia, String fecha, int minutos) {
        return new StudyLog("l", materia, null, null, fecha, "10:00", minutos);
    }

    static StudySession plan(String materia, int minutos) {
        return new StudySession("p", "lunes", materia, "algo", minutos, false);
    }

    @Test
    void lunesDeLaSemana() {
        // Test: LocalDate.of(2026, 10, 19) is a Monday.
        // Input: LocalDate.of(2026, 10, 25) is a Saturday.
        // Expected: The Monday of the week containing 2026-10-25, which is 2026-10-19.
        assertEquals(LocalDate.of(2026, 10, 19), StudyTimeStats.weekStart(LocalDate.of(2026, 10, 25)));
    }

    @Test
    void minutosPorMateriaSoloDeEsaSemana() {
        // Week starts on 2026-10-19 (Monday). Ends before 2026-10-26 (Sunday).
        // Logs:
        // 1. log("s1", "2026-10-18", 100) -> Outside the week [10-19, 10-26)
        // 2. log("s1", "2026-10-19", 25) -> Inside
        // 3. log("s2", "2026-10-25", 50) -> Inside
        List<StudyLog> logs = List.of(
                log("s1", "2026-10-18", 100),
                log("s1", "2026-10-19", 25),
                log("s2", "2026-10-25", 50)
        );
        LocalDate weekStart = LocalDate.of(2026, 10, 19);
        Map<String, Integer> expected = Map.of("s1", 25, "s2", 50);
        assertEquals(expected, StudyTimeStats.minutesBySubject(logs, weekStart));
    }

    @Test
    void planeadoSumaLaGuia() {
        // Guide:
        // 1. plan("s1", 60) -> s1: 60
        // 2. plan("s1", 90) -> s1: 60 + 90 = 150
        List<StudySession> guide = List.of(
                plan("s1", 60),
                plan("s1", 90)
        );
        Map<String, Integer> expected = Map.of("s1", 150);
        assertEquals(expected, StudyTimeStats.plannedMinutesBySubject(guide));
    }

    @Test
    void comparaPlaneadoYEstudiado() {
        // Test case from specification:
        // subjectIds: ["s1", "s2"]
        // logs: [log("s1", "2026-10-20", 25)]
        // guide: [plan("s1", 60)]
        // weekStart: LocalDate.of(2026, 10, 19) (Monday)
        // Expected: List.of(new SubjectWeekTime("s1", 60, 25), new SubjectWeekTime("s2", 0, 0))
        List<String> subjectIds = List.of("s1", "s2");
        List<StudyLog> logs = List.of(log("s1", "2026-10-20", 25));
        List<StudySession> guide = List.of(plan("s1", 60));
        LocalDate weekStart = LocalDate.of(2026, 10, 19);

        List<SubjectWeekTime> result = StudyTimeStats.compareWeek(subjectIds, logs, guide, weekStart);

        assertEquals(2, result.size());
        assertEquals(new SubjectWeekTime("s1", 60, 25), result.get(0));
        assertEquals(new SubjectWeekTime("s2", 0, 0), result.get(1));
    }

    @Test
    void minutosPorSemana() {
        // Logs:
        // 1. log("s1", "2026-10-12", 25) -> Week starts 2026-10-12 (Sun) -> WeekStart 2026-10-11 (Mon)
        // 2. log("s1", "2026-10-20", 25) -> Week starts 2026-10-19 (Mon)
        // 3. log("s2", "2026-10-20", 50) -> Week starts 2026-10-19 (Mon)
        List<StudyLog> logs = List.of(
                log("s1", "2026-10-12", 25),
                log("s1", "2026-10-20", 25),
                log("s2", "2026-10-20", 50)
        );

        // Test for "s1"
        SortedMap<LocalDate, Integer> s1Stats = StudyTimeStats.minutesByWeek(logs, "s1");
        // 2026-10-12 is a Sunday, weekStart is 2026-10-11 (Monday)
        assertEquals(25, s1Stats.get(LocalDate.of(2026, 10, 11)));
        // 2026-10-20 is a Monday, weekStart is 2026-10-19 (Monday)
        assertEquals(25, s1Stats.get(LocalDate.of(2026, 10, 19)));

        // Test for all subjects (null)
        SortedMap<LocalDate, Integer> allStats = StudyTimeStats.minutesByWeek(logs, null);
        // Week starting 2026-10-11: s1=25
        assertEquals(25, allStats.get(LocalDate.of(2026, 10, 11)));
        // Week starting 2026-10-19: s1=25, s2=50. Total = 75
        assertEquals(75, allStats.get(LocalDate.of(2026, 10, 19)));
    }

    @Test
    void formatoDeMinutos() {
        // 45 minutes
        assertEquals("45 min", StudyTimeStats.formatMinutes(45));
        // 120 minutes (2 hours)
        assertEquals("2 h", StudyTimeStats.formatMinutes(120));
        // 90 minutes (1 hour 30 minutes)
        assertEquals("1 h 30 min", StudyTimeStats.formatMinutes(90));
        // 0 minutes
        assertEquals("0 min", StudyTimeStats.formatMinutes(0));
        // 60 minutes (1 hour)
        assertEquals("1 h", StudyTimeStats.formatMinutes(60));
    }
}
```

---

# Error al compilar o testear (corregilo)

```
[ERROR]   StudyTimeStatsTest.minutosPorSemana:97 expected: <25> but was: <null>
  ← línea 97: assertEquals(25, s1Stats.get(LocalDate.of(2026, 10, 11)));
  (la tarea dice get(LocalDate.of(2026, 10, 12)): 2026-10-12 es lunes, así que weekStart(2026-10-12) es 2026-10-12, no 2026-10-11)
```

Completá los `// TODO` que pide la tarea T02b. Devolvé estos archivos completos, cada uno en su bloque: src/main/java/com/planificador/service/StudyTimeStats.java, src/test/java/com/planificador/service/StudyTimeStatsTest.java.
