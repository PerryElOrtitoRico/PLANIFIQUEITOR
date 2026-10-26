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
        // TODO T03b
        throw new UnsupportedOperationException();
    }

    /** Segundos como "MM:SS" (por ejemplo 1500 → "25:00", 65 → "01:05"). */
    public static String format(int seconds) {
        // TODO T03b
        throw new UnsupportedOperationException();
    }
}
