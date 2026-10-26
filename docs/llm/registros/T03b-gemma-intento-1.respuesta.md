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
        if (phase == PomodoroPhase.WORK) {
            phase = PomodoroPhase.SHORT_BREAK;
        } else {
            phase = PomodoroPhase.WORK;
        }
        secondsLeft = minutesFor(phase) * 60;
    }

    /** Segundos como "MM:SS" (por ejemplo 1500 → "25:00", 65 → "01:05"). */
    public static String format(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
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
        LocalDateTime start = end.minusMinutes(minutes);
        String date = end.toLocalDate().toString();
        String startTime = start.format(HORA);
        return new StudyLog(id, subjectId, topicId, topicTitle, date, startTime, minutes);
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
        PomodoroEngine e = new PomodoroEngine(chicos());
        e.skip();
        assertEquals(PomodoroPhase.SHORT_BREAK, e.phase());
        assertEquals(60, e.secondsLeft());
        assertEquals(0, e.completedWorkCycles());
        e.skip();
        assertEquals(PomodoroPhase.WORK, e.phase());
    }

    @Test
    void formatoMinutosSegundos() {
        assertEquals("25:00", PomodoroEngine.format(1500));
        assertEquals("01:05", PomodoroEngine.format(65));
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
        StudyLog l = StudyLogFactory.fromWorkCycle("x", "s1", null, null, LocalDateTime.of(2026, 10, 25, 10, 25), 25);
        assertEquals("2026-10-25", l.getDate());
        assertEquals("10:00", l.getStartTime());
        assertEquals(25, l.getMinutes());
    }
}
```

<!-- 121 s · entrada 2952 · salida 1826 · finish_reason stop -->
