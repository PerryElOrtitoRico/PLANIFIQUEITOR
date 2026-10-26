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
        return switch (p) {
            case WORK -> settings.getWorkMinutes();
            case SHORT_BREAK -> settings.getShortBreakMinutes();
            case LONG_BREAK -> settings.getLongBreakMinutes();
        };
    }

    /** Pasa un segundo. Si la fase termina, pasa a la siguiente y devuelve la fase que terminó; si no, devuelve null. */
    public PomodoroPhase tick() {
        // TODO T03a
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
        // lo completa otra subtarea (T03b): no lo cambies
        throw new UnsupportedOperationException();
    }

    /** Segundos como "MM:SS" (por ejemplo 1500 → "25:00", 65 → "01:05"). */
    public static String format(int seconds) {
        // lo completa otra subtarea (T03b): no lo cambies
        throw new UnsupportedOperationException();
    }
}

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
        PomodoroEngine e = new PomodoroEngine(new PomodoroSettings());
        assertEquals(25, e.minutesFor(PomodoroPhase.WORK));
        assertEquals(15, e.minutesFor(PomodoroPhase.LONG_BREAK));
        assertEquals(1500, e.secondsLeft());
    }

    @Test
    void cicloCompletoConDescansoLargo() {
        // TODO T03a
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
        // lo completa otra subtarea (T03b): no lo cambies
    }

    @Test
    void formatoMinutosSegundos() {
        // lo completa otra subtarea (T03b): no lo cambies
    }
}
```

<!-- 98 s · entrada 2241 · salida 1212 · finish_reason stop -->
