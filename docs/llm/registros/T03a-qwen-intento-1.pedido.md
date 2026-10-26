<!-- modelo qwen/qwen3.5-9b, temperatura 0.2, max_tokens 5527, razonamiento no -->

## system

Sos un programador Java 17 cuidadoso. Te paso archivos con métodos marcados `// TODO`: completá solo los cuerpos que nombra la tarea; los métodos marcados "lo completa otra subtarea" quedan exactamente como están.
Reglas:
- No cambies package, imports que ya están (aunque todavía no se usen), nombres de clases, firmas ni métodos que ya están hechos. Podés agregar imports y métodos private.
- Las clases de model se leen con getters: log.getMinutes(), rules.getPassGrade(). Los records de service se leen con x(): status.condition(), a.percent(). No existen getCondition() ni getPercent().
- No crees ni modifiques otros archivos.
- Devolvé cada archivo completo (nada de '...') en su propio bloque de código, con la ruta en la primera línea. Ejemplo:
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

## 3. Pomodoro
- Empieza en `WORK`. Al terminar un `WORK` se suma un ciclo; si la cantidad de ciclos es múltiplo de `cyclesBeforeLongBreak` sigue `LONG_BREAK`, si no `SHORT_BREAK`. Después de un descanso sigue `WORK`.
- Cada `WORK` terminado se guarda como un `StudyLog` (materia, tema opcional, día y hora de inicio, minutos = `workMinutes`). Saltear una fase no guarda nada.

---

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

---

# Archivos actuales

```java
// src/main/java/com/planificador/service/PomodoroEngine.java
package com.planificador.service;

import com.planificador.model.PomodoroSettings;

/**
 * Lógica del Pomodoro, sin Swing: la interfaz llama a tick() una vez por segundo.
 * Empieza en WORK. Después de cada WORK viene SHORT_BREAK, salvo cada cyclesBeforeLongBreak ciclos, que viene LONG_BREAK.
 * Después de cualquier descanso viene WORK.
 */
public final class PomodoroEngine {

    private final PomodoroSettings settings;
    private PomodoroPhase phase = PomodoroPhase.WORK;
    private int secondsLeft;
    private int completedWorkCycles = 0;

    public PomodoroEngine(PomodoroSettings settings) {
        this.settings = settings;
        this.secondsLeft = minutesFor(PomodoroPhase.WORK) * 60;
    }

    public PomodoroPhase phase() { return phase; }
    public int secondsLeft() { return secondsLeft; }
    public int completedWorkCycles() { return completedWorkCycles; }

    /** Minutos que dura la fase según los ajustes. */
    public int minutesFor(PomodoroPhase p) {
        // TODO T03a
        throw new UnsupportedOperationException();
    }

    /** Pasa un segundo. Si la fase termina, pasa a la siguiente y devuelve la fase que terminó; si no, devuelve null. */
    public PomodoroPhase tick() {
        // TODO T03a
        throw new UnsupportedOperationException();
    }

    /** Termina la fase actual sin contarla: de WORK pasa a SHORT_BREAK (no suma ciclo); de un descanso pasa a WORK. */
    public void skip() {
        // lo completa otra subtarea (T03b): no lo cambies
        throw new UnsupportedOperationException();
    }

    /** Segundos como "MM:SS" (por ejemplo 1500 → "25:00", 65 → "01:05"). */
    public static String format(int seconds) {
        // lo completa otra subtarea (T03b): no lo cambies
        throw new UnsupportedOperationException();
    }
}
```

```java
// src/test/java/com/planificador/service/PomodoroEngineTest.java
package com.planificador.service;

import com.planificador.model.PomodoroSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PomodoroEngineTest {

    /** Ajustes chicos para no tener que llamar a tick() miles de veces: 2 min de estudio, 1 de descanso, 3 largo, cada 2. */
    static PomodoroSettings chicos() {
        return new PomodoroSettings(2, 1, 3, 2);
    }

    /** Llama a tick() n veces y devuelve la última fase terminada que no fue null (o null). */
    static PomodoroPhase avanzar(PomodoroEngine e, int n) {
        PomodoroPhase ultima = null;
        for (int i = 0; i < n; i++) {
            PomodoroPhase p = e.tick();
            if (p != null) ultima = p;
        }
        return ultima;
    }

    @Test
    void minutosDeCadaFase() {
        // TODO T03a
    }

    @Test
    void cicloCompletoConDescansoLargo() {
        // TODO T03a
    }

    @Test
    void saltearNoSumaCiclos() {
        // lo completa otra subtarea (T03b): no lo cambies
    }

    @Test
    void formatoMinutosSegundos() {
        // lo completa otra subtarea (T03b): no lo cambies
    }
}
```

Completá los `// TODO` que pide la tarea T03a. Devolvé estos archivos completos, cada uno en su bloque: src/main/java/com/planificador/service/PomodoroEngine.java, src/test/java/com/planificador/service/PomodoroEngineTest.java.
