<!-- contrato: 1,3 | archivos: src/main/java/com/planificador/service/PomodoroEngine.java, src/main/java/com/planificador/service/StudyLogFactory.java, src/test/java/com/planificador/service/PomodoroEngineTest.java, src/test/java/com/planificador/service/StudyLogFactoryTest.java -->
# T03b — Pomodoro: saltear, formato y registro del ciclo

## Objetivo
Completar **solo** `skip` y `format` de `PomodoroEngine`, `fromWorkCycle` de `StudyLogFactory`, y los tests `saltearNoSumaCiclos`, `formatoMinutosSegundos` y `armaElRegistroDelCiclo`. `minutesFor` y `tick` ya están hechos.

## Código que ya existe
- `StudyLog` (clase con getters): `new StudyLog(id, subjectId, topicId, topicTitle, date, startTime, minutes)`; `l.getDate()`, `l.getStartTime()`, `l.getMinutes()`.
- En `StudyLogFactory` ya está `HORA = DateTimeFormatter.ofPattern("HH:mm")`.

## Métodos (pasos en orden)
- `skip()`:
  1. `if (phase == PomodoroPhase.WORK) phase = PomodoroPhase.SHORT_BREAK; else phase = PomodoroPhase.WORK;`
  2. `secondsLeft = minutesFor(phase) * 60;` (no se toca `completedWorkCycles`)
- `format(seconds)`: `return String.format("%02d:%02d", seconds / 60, seconds % 60);`
- `fromWorkCycle(id, subjectId, topicId, topicTitle, end, minutes)`:
  1. `LocalDateTime start = end.minusMinutes(minutes);`
  2. `return new StudyLog(id, subjectId, topicId, topicTitle, end.toLocalDate().toString(), start.format(HORA), minutes);`

## Tests
- `saltearNoSumaCiclos` (usa `chicos()` del test): `PomodoroEngine e = new PomodoroEngine(chicos()); e.skip();` → `assertEquals(PomodoroPhase.SHORT_BREAK, e.phase())`, `assertEquals(60, e.secondsLeft())`, `assertEquals(0, e.completedWorkCycles())`; otro `e.skip();` → `assertEquals(PomodoroPhase.WORK, e.phase())`.
- `formatoMinutosSegundos`: `assertEquals("25:00", PomodoroEngine.format(1500));` y `assertEquals("01:05", PomodoroEngine.format(65));`
- `armaElRegistroDelCiclo`: `StudyLog l = StudyLogFactory.fromWorkCycle("x", "s1", null, null, LocalDateTime.of(2026, 10, 25, 10, 25), 25);` → `assertEquals("2026-10-25", l.getDate())`, `assertEquals("10:00", l.getStartTime())`, `assertEquals(25, l.getMinutes())`.
