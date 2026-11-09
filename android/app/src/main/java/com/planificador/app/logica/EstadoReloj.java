package com.planificador.app.logica;

import com.planificador.service.PomodoroPhase;

/**
 * Estado del Pomodoro o del cronómetro, guardado en disco local (Gson) para sobrevivir a que Android cierre la app.
 * Las horas son de la hora real (System.currentTimeMillis); los "elapsed" son SystemClock.elapsedRealtime, que vuelve
 * a cero al reiniciar el teléfono: así se detecta un reinicio.
 */
public class EstadoReloj {
    public String modo;           // "pomodoro" o "cronometro"
    public String subjectId;
    public String topicId;        // puede ser null
    public String topicTitle;     // puede ser null
    public String note;           // nota de la sesión en curso; null = sin nota
    public PomodoroPhase phase;   // null en el cronómetro
    public int ciclos;            // ciclos de estudio terminados
    public boolean corriendo;
    public long finMs;            // si corre: cuándo termina la fase
    public int restanteSeg;       // si no corre: segundos que faltan
    public int faseSeg;           // duración total de la fase
    public long tramoInicioMs;    // cuándo arrancó el tramo actual sin pausa
    public int estudiadoSeg;      // estudio acumulado en la fase antes de este tramo
    public long latidoMs;         // última vez que se supo que el reloj andaba
    public long latidoElapsedMs;
    public String idSesion;       // id de la sesión que se guarda al terminar o cortar esta fase
    public String prefijoId;      // cronómetro de una sesión de la guía: "cron_<id>_<fecha>_" (como la PC); null = "cron_"/"pl_"

    EstadoReloj copia() {
        EstadoReloj c = new EstadoReloj();
        c.modo = modo; c.subjectId = subjectId; c.topicId = topicId; c.topicTitle = topicTitle; c.note = note;
        c.phase = phase; c.ciclos = ciclos; c.corriendo = corriendo; c.finMs = finMs; c.restanteSeg = restanteSeg;
        c.faseSeg = faseSeg; c.tramoInicioMs = tramoInicioMs; c.estudiadoSeg = estudiadoSeg; c.latidoMs = latidoMs;
        c.latidoElapsedMs = latidoElapsedMs; c.idSesion = idSesion; c.prefijoId = prefijoId;
        return c;
    }

    public boolean esCronometro() {
        return "cronometro".equals(modo);
    }

    /** Si la fase actual es de estudio (cuenta como sesión). */
    public boolean esEstudio() {
        return esCronometro() || phase == PomodoroPhase.WORK;
    }
}
