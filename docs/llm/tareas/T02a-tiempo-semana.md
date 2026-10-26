<!-- contrato: 1,4 | archivos: src/main/java/com/planificador/service/StudyTimeStats.java, src/test/java/com/planificador/service/StudyTimeStatsTest.java -->
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
