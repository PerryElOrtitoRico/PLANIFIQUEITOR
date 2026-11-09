package com.planificador.app.logica;

import com.planificador.model.PomodoroSettings;
import com.planificador.model.StudyLog;
import com.planificador.service.PomodoroPhase;
import com.planificador.service.StudyLogFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Reglas del reloj sobre la hora real, sin Android. Las mismas del Pomodoro de la PC: cada fase termina, frena y
 * avisa; un estudio terminado se guarda; saltear no guarda. Además: si el teléfono se reinicia con el reloj andando,
 * se guardan los minutos de estudio hasta el último latido. Nunca modifica el estado recibido: devuelve una copia.
 */
public final class Reloj {

    /** sesion: la que hay que guardar (o null); avisar: sonar y vibrar. */
    public record Resultado(EstadoReloj estado, StudyLog sesion, boolean avisar) { }

    private Reloj() { }

    public static EstadoReloj nuevoPomodoro(PomodoroSettings s, String subjectId, String topicId, String topicTitle) {
        EstadoReloj e = new EstadoReloj();
        e.modo = "pomodoro";
        e.subjectId = subjectId;
        e.topicId = topicId;
        e.topicTitle = topicTitle;
        e.phase = PomodoroPhase.WORK;
        empezarFase(e, s.getWorkMinutes() * 60);
        return e;
    }

    public static EstadoReloj nuevoCronometro(int minutos, String subjectId, String topicTitle) {
        EstadoReloj e = new EstadoReloj();
        e.modo = "cronometro";
        e.subjectId = subjectId;
        e.topicTitle = topicTitle;
        empezarFase(e, minutos * 60);
        return e;
    }

    public static EstadoReloj iniciar(EstadoReloj e, long ahoraMs, long elapsedMs) {
        EstadoReloj c = e.copia();
        if (c.corriendo) return c;
        c.corriendo = true;
        c.finMs = ahoraMs + c.restanteSeg * 1000L;
        c.tramoInicioMs = ahoraMs;
        c.latidoMs = ahoraMs;
        c.latidoElapsedMs = elapsedMs;
        return c;
    }

    public static EstadoReloj pausar(EstadoReloj e, long ahoraMs) {
        EstadoReloj c = e.copia();
        if (!c.corriendo) return c;
        c.restanteSeg = restante(c, ahoraMs);
        if (c.esEstudio()) c.estudiadoSeg += (int) ((ahoraMs - c.tramoInicioMs) / 1000);
        c.corriendo = false;
        return c;
    }

    /** Segundos que faltan para terminar la fase, nunca negativo. */
    public static int restante(EstadoReloj e, long ahoraMs) {
        if (!e.corriendo) return e.restanteSeg;
        return (int) Math.max(0, (e.finMs - ahoraMs + 999) / 1000);
    }

    /** La fase llegó a su fin (a la hora finMs). */
    public static Resultado terminarFase(EstadoReloj e, PomodoroSettings s) {
        EstadoReloj c = e.copia();
        StudyLog sesion = null;
        if (c.esEstudio()) {
            sesion = sesion(c, c.finMs, c.faseSeg / 60);
            c.note = null;
        }
        if (c.esCronometro()) {
            empezarFase(c, c.faseSeg);
        } else if (c.phase == PomodoroPhase.WORK) {
            c.ciclos++;
            c.phase = c.ciclos % Math.max(1, s.getCyclesBeforeLongBreak()) == 0
                    ? PomodoroPhase.LONG_BREAK : PomodoroPhase.SHORT_BREAK;
            empezarFase(c, minutos(s, c.phase) * 60);
        } else {
            c.phase = PomodoroPhase.WORK;
            empezarFase(c, s.getWorkMinutes() * 60);
        }
        return new Resultado(c, sesion, true);
    }

    /** Botón Terminar: guarda lo estudiado (si es al menos 1 minuto) y vuelve al inicio de la fase. */
    public static Resultado detener(EstadoReloj e, long ahoraMs) {
        EstadoReloj c = e.copia();
        int seg = c.estudiadoSeg + (c.corriendo ? (int) ((ahoraMs - c.tramoInicioMs) / 1000) : 0);
        StudyLog sesion = c.esEstudio() && seg >= 60 ? sesion(c, ahoraMs, seg / 60) : null;
        if (sesion != null) c.note = null;
        empezarFase(c, c.faseSeg);
        return new Resultado(c, sesion, false);
    }

    /** Como PomodoroEngine.skip(): no cuenta la fase ni suma ciclo. Si corría, sigue corriendo con la fase nueva. */
    public static EstadoReloj saltear(EstadoReloj e, PomodoroSettings s, long ahoraMs) {
        EstadoReloj c = e.copia();
        boolean corria = c.corriendo;
        c.phase = c.phase == PomodoroPhase.WORK ? PomodoroPhase.SHORT_BREAK : PomodoroPhase.WORK;
        empezarFase(c, minutos(s, c.phase) * 60);
        return corria ? iniciar(c, ahoraMs, e.latidoElapsedMs) : c;
    }

    public static EstadoReloj latido(EstadoReloj e, long ahoraMs, long elapsedMs) {
        EstadoReloj c = e.copia();
        c.latidoMs = ahoraMs;
        c.latidoElapsedMs = elapsedMs;
        return c;
    }

    /**
     * Al abrir la app, al sonar la alarma o al prender el teléfono. Si el teléfono se reinició (elapsed volvió a
     * empezar), guarda el estudio hasta el último latido. Si no, y la fase venció, la termina a su hora.
     */
    public static Resultado recuperar(EstadoReloj e, PomodoroSettings s, long ahoraMs, long elapsedMs) {
        if (e == null || !e.corriendo) return new Resultado(e == null ? null : e.copia(), null, false);
        if (elapsedMs < e.latidoElapsedMs) {
            EstadoReloj c = e.copia();
            int seg = c.estudiadoSeg + (int) ((c.latidoMs - c.tramoInicioMs) / 1000);
            StudyLog sesion = c.esEstudio() && seg >= 60 ? sesion(c, c.latidoMs, seg / 60) : null;
            if (sesion != null) c.note = null;
            empezarFase(c, c.faseSeg);
            return new Resultado(c, sesion, false);
        }
        if (ahoraMs >= e.finMs) return terminarFase(e, s);
        return new Resultado(e.copia(), null, false);
    }

    // ---- ayudas ----

    /** Deja la fase sin correr, completa, sin estudio acumulado y con un id de sesión nuevo. */
    private static void empezarFase(EstadoReloj c, int segundos) {
        c.corriendo = false;
        c.faseSeg = segundos;
        c.restanteSeg = segundos;
        c.estudiadoSeg = 0;
        c.idSesion = (c.esCronometro() ? "cron_" : "pl_") + UUID.randomUUID();
    }

    private static StudyLog sesion(EstadoReloj c, long finMs, int minutos) {
        LocalDateTime fin = LocalDateTime.ofInstant(Instant.ofEpochMilli(finMs), ZoneId.systemDefault());
        StudyLog l = StudyLogFactory.fromWorkCycle(c.idSesion, c.subjectId, c.topicId, c.topicTitle, fin, minutos);
        if (c.esCronometro()) l.setSource("cronometro");
        l.setNote(c.note);
        return l;
    }

    private static int minutos(PomodoroSettings s, PomodoroPhase p) {
        return switch (p) {
            case WORK -> s.getWorkMinutes();
            case SHORT_BREAK -> s.getShortBreakMinutes();
            case LONG_BREAK -> s.getLongBreakMinutes();
        };
    }
}
