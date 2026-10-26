<!-- contrato: 1,3 | archivos: src/main/java/com/planificador/service/PomodoroEngine.java, src/test/java/com/planificador/service/PomodoroEngineTest.java -->
# T03a — Pomodoro: duración de cada fase y paso del tiempo

## Objetivo
Completar **solo** `minutesFor` y `tick` de `PomodoroEngine` y los tests `minutosDeCadaFase` y `cicloCompletoConDescansoLargo`.

## Código que ya existe
- `PomodoroSettings` (clase con getters): `settings.getWorkMinutes()`, `getShortBreakMinutes()`, `getLongBreakMinutes()`, `getCyclesBeforeLongBreak()`. Constructor `new PomodoroSettings(work, shortBreak, longBreak, cyclesBeforeLongBreak)`; `new PomodoroSettings()` = 25, 5, 15, 4.
- Los campos `phase`, `secondsLeft` y `completedWorkCycles` ya están declarados; el constructor ya pone `secondsLeft = minutesFor(WORK) * 60`.

## Métodos
- `minutesFor(p)`, exactamente así:
```java
return switch (p) {
    case WORK -> settings.getWorkMinutes();
    case SHORT_BREAK -> settings.getShortBreakMinutes();
    case LONG_BREAK -> settings.getLongBreakMinutes();
};
```
- `tick()`, pasos en orden:
  1. `if (secondsLeft > 0) secondsLeft--;`
  2. `if (secondsLeft > 0) return null;`
  3. `PomodoroPhase finished = phase;`
  4. `if (phase == PomodoroPhase.WORK) { completedWorkCycles++; int every = Math.max(1, settings.getCyclesBeforeLongBreak()); phase = (completedWorkCycles % every == 0) ? PomodoroPhase.LONG_BREAK : PomodoroPhase.SHORT_BREAK; } else { phase = PomodoroPhase.WORK; }`
  5. `secondsLeft = minutesFor(phase) * 60;`
  6. `return finished;`

## Tests (usan `chicos()` = 2 min de estudio, 1 de descanso, 3 largo, cada 2 ciclos, y `avanzar(e, n)` que ya están en el test)
- `minutosDeCadaFase`: con `new PomodoroEngine(new PomodoroSettings())`: `assertEquals(25, e.minutesFor(PomodoroPhase.WORK))`, `assertEquals(15, e.minutesFor(PomodoroPhase.LONG_BREAK))` y `assertEquals(1500, e.secondsLeft())`.
- `cicloCompletoConDescansoLargo`: con `PomodoroEngine e = new PomodoroEngine(chicos());`
  - `assertEquals(PomodoroPhase.WORK, avanzar(e, 120));` → `assertEquals(PomodoroPhase.SHORT_BREAK, e.phase());` y `assertEquals(1, e.completedWorkCycles());`
  - `assertEquals(PomodoroPhase.SHORT_BREAK, avanzar(e, 60));` → `assertEquals(PomodoroPhase.WORK, e.phase());`
  - `assertEquals(PomodoroPhase.WORK, avanzar(e, 120));` → `assertEquals(PomodoroPhase.LONG_BREAK, e.phase());` y `assertEquals(180, e.secondsLeft());`
