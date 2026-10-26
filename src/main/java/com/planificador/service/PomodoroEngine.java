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
