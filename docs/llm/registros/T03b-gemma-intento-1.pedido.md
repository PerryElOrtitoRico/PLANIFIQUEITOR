<!-- modelo google/gemma-4-e4b-qat, temperatura 0.2, max_tokens 4744, razonamiento no -->

## system

Sos un programador Java 17 cuidadoso. Te paso archivos con métodos marcados `// TODO`: completá solo los cuerpos que nombra la tarea; los métodos marcados "lo completa otra subtarea" quedan exactamente como están.
Reglas:
- No cambies package, imports que ya están (aunque todavía no se usen), nombres de clases, firmas ni métodos que ya están hechos. Podés agregar imports y métodos private.
- Las clases de model se leen con getters: log.getMinutes(), rules.getPassGrade(). Los records de service se leen con x(): status.condition(), a.percent(). No existen getCondition() ni getPercent().
- En los tests, copiá las aserciones de la tarea tal cual (mismos valores y fechas) y no escribas comentarios.
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

## 3. Pomodoro
- Empieza en `WORK`. Al terminar un `WORK` se suma un ciclo; si la cantidad de ciclos es múltiplo de `cyclesBeforeLongBreak` sigue `LONG_BREAK`, si no `SHORT_BREAK`. Después de un descanso sigue `WORK`.
- Cada `WORK` terminado se guarda como un `StudyLog` (materia, tema opcional, día y hora de inicio, minutos = `workMinutes`). Saltear una fase no guarda nada.

---

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
        return switch (p) {
            case WORK -> settings.getWorkMinutes();
            case SHORT_BREAK -> settings.getShortBreakMinutes();
            case LONG_BREAK -> settings.getLongBreakMinutes();
        };
    }

    /** Pasa un segundo. Si la fase termina, pasa a la siguiente y devuelve la fase que terminó; si no, devuelve null. */
    public PomodoroPhase tick() {
        if (secondsLeft > 0) secondsLeft--;
        if (secondsLeft > 0) return null;

        PomodoroPhase finished = phase;
        if (phase == PomodoroPhase.WORK) {
            completedWorkCycles++;
            int every = Math.max(1, settings.getCyclesBeforeLongBreak());
            phase = (completedWorkCycles % every == 0) ? PomodoroPhase.LONG_BREAK : PomodoroPhase.SHORT_BREAK;
        } else {
            phase = PomodoroPhase.WORK;
        }

        secondsLeft = minutesFor(phase) * 60;
        return finished;
    }

    /** Termina la fase actual sin contarla: de WORK pasa a SHORT_BREAK (no suma ciclo); de un descanso pasa a WORK. */
    public void skip() {
        // TODO T03b
        throw new UnsupportedOperationException();
    }

    /** Segundos como "MM:SS" (por ejemplo 1500 → "25:00", 65 → "01:05"). */
    public static String format(int seconds) {
        // TODO T03b
        throw new UnsupportedOperationException();
    }
}
```

```java
// src/main/java/com/planificador/service/StudyLogFactory.java
package com.planificador.service;

import com.planificador.model.StudyLog;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Arma el StudyLog de un ciclo de estudio terminado. */
public final class StudyLogFactory {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private StudyLogFactory() { }

    /**
     * @param end     cuándo terminó el ciclo
     * @param minutes duración del ciclo
     * @return StudyLog con date = día de end y startTime = end - minutes en formato HH:mm
     */
    public static StudyLog fromWorkCycle(String id, String subjectId, String topicId, String topicTitle,
                                         LocalDateTime end, int minutes) {
        // TODO T03b
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
        PomodoroEngine e = new PomodoroEngine(new PomodoroSettings());
        assertEquals(25, e.minutesFor(PomodoroPhase.WORK));
        assertEquals(15, e.minutesFor(PomodoroPhase.LONG_BREAK));
        assertEquals(1500, e.secondsLeft());
    }

    @Test
    void cicloCompletoConDescansoLargo() {
        PomodoroEngine e = new PomodoroEngine(chicos());
        assertEquals(PomodoroPhase.WORK, avanzar(e, 120));
        assertEquals(PomodoroPhase.SHORT_BREAK, e.phase());
        assertEquals(1, e.completedWorkCycles());

        assertEquals(PomodoroPhase.SHORT_BREAK, avanzar(e, 60));
        assertEquals(PomodoroPhase.WORK, e.phase());

        assertEquals(PomodoroPhase.WORK, avanzar(e, 120));
        assertEquals(PomodoroPhase.LONG_BREAK, e.phase());
        assertEquals(180, e.secondsLeft());
    }

    @Test
    void saltearNoSumaCiclos() {
        // TODO T03b
    }

    @Test
    void formatoMinutosSegundos() {
        // TODO T03b
    }
}
```

```java
// src/test/java/com/planificador/service/StudyLogFactoryTest.java
package com.planificador.service;

import com.planificador.model.StudyLog;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class StudyLogFactoryTest {

    @Test
    void armaElRegistroDelCiclo() {
        // TODO T03b
    }
}
```

Completá los `// TODO` que pide la tarea T03b. Devolvé estos archivos completos, cada uno en su bloque: src/main/java/com/planificador/service/PomodoroEngine.java, src/main/java/com/planificador/service/StudyLogFactory.java, src/test/java/com/planificador/service/PomodoroEngineTest.java, src/test/java/com/planificador/service/StudyLogFactoryTest.java.
