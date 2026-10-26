<!-- modelo google/gemma-4-e4b-qat, temperatura 0.2, max_tokens 5462, razonamiento no -->

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

# T02a — Tiempo por materia: semana, estudiado y planeado

## Objetivo
Completar **solo** `weekStart`, `minutesBySubject` y `plannedMinutesBySubject` de `StudyTimeStats` y los tests `lunesDeLaSemana`, `minutosPorMateriaSoloDeEsaSemana` y `planeadoSumaLaGuia`.

## Código que ya existe
- `StudyLog` (clase con getters): `log.getSubjectId()`, `log.getDate()` (texto `YYYY-MM-DD`, puede ser null), `log.getMinutes()`.
- `StudySession` (clase con getters): `s.getSubjectId()` (puede ser null), `s.getDurationMinutes()`.

## Métodos (pasos en orden)
- `weekStart(d)`: `return d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));`
- `minutesBySubject(logs, weekStart)`:
  1. `Map<String, Integer> out = new HashMap<>(); LocalDate end = weekStart.plusDays(7);`
  2. `for (StudyLog log : logs) {`
  3. `  if (log.getDate() == null) continue;`
  4. `  LocalDate f; try { f = LocalDate.parse(log.getDate()); } catch (DateTimeParseException e) { continue; }`
  5. `  if (!f.isBefore(weekStart) && f.isBefore(end)) out.merge(log.getSubjectId(), log.getMinutes(), Integer::sum);`
  6. `}` y `return out;`
- `plannedMinutesBySubject(guide)`:
  1. `Map<String, Integer> out = new HashMap<>();`
  2. `for (StudySession s : guide) { if (s.getSubjectId() == null) continue; out.merge(s.getSubjectId(), s.getDurationMinutes(), Integer::sum); }`
  3. `return out;`

## Tests (usan `log(materia, fecha, minutos)` y `plan(materia, minutos)` que ya están en el test)
- `lunesDeLaSemana`: `assertEquals(LocalDate.of(2026, 10, 19), StudyTimeStats.weekStart(LocalDate.of(2026, 10, 25)));`
- `minutosPorMateriaSoloDeEsaSemana`: con `List.of(log("s1", "2026-10-18", 100), log("s1", "2026-10-19", 25), log("s2", "2026-10-25", 50))` → `assertEquals(Map.of("s1", 25, "s2", 50), StudyTimeStats.minutesBySubject(logs, LocalDate.of(2026, 10, 19)));`
- `planeadoSumaLaGuia`: `assertEquals(Map.of("s1", 150), StudyTimeStats.plannedMinutesBySubject(List.of(plan("s1", 60), plan("s1", 90))));`

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
        // TODO T02a
        throw new UnsupportedOperationException();
    }

    /** Minutos estudiados por subjectId en la semana que empieza en weekStart (de weekStart a weekStart + 6 días). */
    public static Map<String, Integer> minutesBySubject(List<StudyLog> logs, LocalDate weekStart) {
        // TODO T02a
        throw new UnsupportedOperationException();
    }

    /** Minutos planeados por subjectId en la guía semanal: suma de durationMinutes. */
    public static Map<String, Integer> plannedMinutesBySubject(List<StudySession> guide) {
        // TODO T02a
        throw new UnsupportedOperationException();
    }

    /** Planeado contra estudiado de cada materia de subjectIds, en ese orden, para la semana que empieza en weekStart. */
    public static List<SubjectWeekTime> compareWeek(List<String> subjectIds, List<StudyLog> logs,
                                                    List<StudySession> guide, LocalDate weekStart) {
        // lo completa otra subtarea (T02b): no lo cambies
        throw new UnsupportedOperationException();
    }

    /** Minutos por semana (clave: lunes de la semana) de una materia; subjectId null = todas las materias. */
    public static SortedMap<LocalDate, Integer> minutesByWeek(List<StudyLog> logs, String subjectId) {
        // lo completa otra subtarea (T02b): no lo cambies
        throw new UnsupportedOperationException();
    }

    /** "45 min", "2 h", "1 h 30 min". */
    public static String formatMinutes(int minutes) {
        // lo completa otra subtarea (T02b): no lo cambies
        throw new UnsupportedOperationException();
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
        // TODO T02a
    }

    @Test
    void minutosPorMateriaSoloDeEsaSemana() {
        // TODO T02a
    }

    @Test
    void planeadoSumaLaGuia() {
        // TODO T02a
    }

    @Test
    void comparaPlaneadoYEstudiado() {
        // lo completa otra subtarea (T02b): no lo cambies
    }

    @Test
    void minutosPorSemana() {
        // lo completa otra subtarea (T02b): no lo cambies
    }

    @Test
    void formatoDeMinutos() {
        // lo completa otra subtarea (T02b): no lo cambies
    }
}
```

Completá los `// TODO` que pide la tarea T02a. Devolvé estos archivos completos, cada uno en su bloque: src/main/java/com/planificador/service/StudyTimeStats.java, src/test/java/com/planificador/service/StudyTimeStatsTest.java.
