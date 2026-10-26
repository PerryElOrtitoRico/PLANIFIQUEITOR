<!-- contrato: 1,4 | archivos: src/main/java/com/planificador/service/StudyTimeStats.java, src/test/java/com/planificador/service/StudyTimeStatsTest.java -->
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
