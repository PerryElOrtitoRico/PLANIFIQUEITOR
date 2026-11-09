package com.planificador.app.logica;

import com.planificador.model.PomodoroSettings;
import com.planificador.model.StudyLog;
import com.planificador.service.PomodoroPhase;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/** Reloj del Pomodoro y del cronómetro sobre la hora real, con cortes y recuperación. */
class RelojTest {

    static final long T0 = 1_762_600_000_000L;
    static final long MIN = 60_000L;
    static final PomodoroSettings AJUSTES = new PomodoroSettings(25, 5, 15, 4);
    static final long ARRANQUE = T0 - 1_000_000L;     // el teléfono se prendió antes de T0
    static final long REARRANQUE = T0 + 50 * MIN;     // y se reinició a los 50 min

    /** SystemClock.elapsedRealtime() en ese momento, sin reinicio. */
    static long el(long ms) { return ms - ARRANQUE; }

    /** elapsedRealtime después del reinicio. */
    static long el2(long ms) { return ms - REARRANQUE; }

    static String hora(long ms) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    static EstadoReloj pomodoroCorriendo() {
        return Reloj.iniciar(Reloj.nuevoPomodoro(AJUSTES, "m1", "t1", "Bernoulli"), T0, el(T0));
    }

    @Test
    void unPomodoroNuevoEmpiezaEnEstudioSinCorrer() {
        EstadoReloj e = Reloj.nuevoPomodoro(AJUSTES, "m1", null, null);
        assertEquals(PomodoroPhase.WORK, e.phase);
        assertFalse(e.corriendo);
        assertEquals(25 * 60, Reloj.restante(e, T0));
        assertTrue(e.idSesion.startsWith("pl_"));
    }

    @Test
    void restanteSigueLaHoraReal() {
        EstadoReloj e = pomodoroCorriendo();
        assertEquals(25 * 60 - 90, Reloj.restante(e, T0 + 90_000));
        assertEquals(0, Reloj.restante(e, T0 + 30 * MIN));
    }

    @Test
    void unCicloTerminadoGuardaLosMinutosDelAjuste() {
        Reloj.Resultado r = Reloj.terminarFase(pomodoroCorriendo(), AJUSTES);
        StudyLog l = r.sesion();
        assertEquals(25, l.getMinutes());
        assertEquals(hora(T0), l.getStartTime());
        assertEquals("m1", l.getSubjectId());
        assertEquals("t1", l.getTopicId());
        assertNull(l.getSource());
        assertEquals(PomodoroPhase.SHORT_BREAK, r.estado().phase);
        assertEquals(1, r.estado().ciclos);
        assertTrue(r.avisar());
        assertFalse(r.estado().corriendo);
        assertEquals(5 * 60, Reloj.restante(r.estado(), T0 + 26 * MIN));
    }

    @Test
    void cadaCuatroCiclosVieneElDescansoLargo() {
        EstadoReloj e = Reloj.nuevoPomodoro(AJUSTES, "m1", null, null);
        for (int i = 0; i < 4; i++) {
            e = Reloj.terminarFase(Reloj.iniciar(e, T0, el(T0)), AJUSTES).estado();   // estudio
            if (i < 3) {
                assertEquals(PomodoroPhase.SHORT_BREAK, e.phase);
                e = Reloj.terminarFase(Reloj.iniciar(e, T0, el(T0)), AJUSTES).estado(); // descanso
            }
        }
        assertEquals(PomodoroPhase.LONG_BREAK, e.phase);
        assertEquals(15 * 60, e.restanteSeg);
    }

    @Test
    void unDescansoTerminadoNoGuardaYCadaEstudioTieneOtroId() {
        Reloj.Resultado r1 = Reloj.terminarFase(pomodoroCorriendo(), AJUSTES);
        Reloj.Resultado r2 = Reloj.terminarFase(Reloj.iniciar(r1.estado(), T0 + 30 * MIN, el(T0 + 30 * MIN)), AJUSTES);
        assertNull(r2.sesion());
        assertTrue(r2.avisar());
        assertEquals(PomodoroPhase.WORK, r2.estado().phase);
        assertNotEquals(r1.sesion().getId(), r2.estado().idSesion);
    }

    @Test
    void elTiempoEnPausaNoCuenta() {
        EstadoReloj e = Reloj.pausar(pomodoroCorriendo(), T0 + 10 * MIN);
        assertEquals(15 * 60, Reloj.restante(e, T0 + 20 * MIN));
        e = Reloj.iniciar(e, T0 + 30 * MIN, el(T0 + 30 * MIN));
        e = Reloj.latido(e, T0 + 35 * MIN, el(T0 + 35 * MIN));
        StudyLog l = Reloj.recuperar(e, AJUSTES, T0 + 90 * MIN, el2(T0 + 90 * MIN)).sesion();
        assertEquals(15, l.getMinutes());
    }

    @Test
    void trasReinicioGuardaHastaElUltimoLatido() {
        EstadoReloj e = Reloj.latido(pomodoroCorriendo(), T0 + 12 * MIN, el(T0 + 12 * MIN));
        Reloj.Resultado r = Reloj.recuperar(e, AJUSTES, T0 + 60 * MIN, el2(T0 + 60 * MIN));
        assertEquals(12, r.sesion().getMinutes());
        assertEquals(hora(T0), r.sesion().getStartTime());
        assertEquals(e.idSesion, r.sesion().getId());
        assertFalse(r.estado().corriendo);
        assertFalse(r.avisar());
        assertEquals(PomodoroPhase.WORK, r.estado().phase);
        assertEquals(25 * 60, r.estado().restanteSeg);
    }

    @Test
    void trasReinicioConMenosDeUnMinutoNoGuarda() {
        EstadoReloj e = Reloj.latido(pomodoroCorriendo(), T0 + 50_000, el(T0 + 50_000));
        Reloj.Resultado r = Reloj.recuperar(e, AJUSTES, T0 + 60 * MIN, el2(T0 + 60 * MIN));
        assertNull(r.sesion());
        assertFalse(r.estado().corriendo);
    }

    @Test
    void trasReinicioEnUnDescansoNoGuarda() {
        EstadoReloj descanso = Reloj.terminarFase(pomodoroCorriendo(), AJUSTES).estado();
        EstadoReloj e = Reloj.latido(Reloj.iniciar(descanso, T0 + 25 * MIN, el(T0 + 25 * MIN)), T0 + 28 * MIN, el(T0 + 28 * MIN));
        Reloj.Resultado r = Reloj.recuperar(e, AJUSTES, T0 + 60 * MIN, el2(T0 + 60 * MIN));
        assertNull(r.sesion());
        assertFalse(r.estado().corriendo);
    }

    @Test
    void recuperarDosVecesNoDuplica() {
        EstadoReloj e = Reloj.latido(pomodoroCorriendo(), T0 + 12 * MIN, el(T0 + 12 * MIN));
        Reloj.Resultado r = Reloj.recuperar(e, AJUSTES, T0 + 60 * MIN, el2(T0 + 60 * MIN));
        assertNull(Reloj.recuperar(r.estado(), AJUSTES, T0 + 61 * MIN, el2(T0 + 61 * MIN)).sesion());
        // el aviso de arranque y la app leyendo el mismo estado a la vez: mismo id, mismo archivo
        assertEquals(r.sesion().getId(), Reloj.recuperar(e, AJUSTES, T0 + 60 * MIN, el2(T0 + 60 * MIN)).sesion().getId());
    }

    @Test
    void alAbrirConLaFaseVencidaLaTermina() {
        Reloj.Resultado r = Reloj.recuperar(pomodoroCorriendo(), AJUSTES, T0 + 40 * MIN, el(T0 + 40 * MIN));
        assertEquals(25, r.sesion().getMinutes());
        assertEquals(hora(T0), r.sesion().getStartTime());
        assertEquals(PomodoroPhase.SHORT_BREAK, r.estado().phase);
        assertTrue(r.avisar());
    }

    @Test
    void recuperarConLaFaseEnCursoNoHaceNada() {
        EstadoReloj e = pomodoroCorriendo();
        Reloj.Resultado r = Reloj.recuperar(e, AJUSTES, T0 + 10 * MIN, el(T0 + 10 * MIN));
        assertNull(r.sesion());
        assertFalse(r.avisar());
        assertTrue(r.estado().corriendo);
        assertEquals(e.finMs, r.estado().finMs);
    }

    @Test
    void recuperarSinEstadoOSinCorrerNoHaceNada() {
        assertNull(Reloj.recuperar(null, AJUSTES, T0, 1).sesion());
        assertNull(Reloj.recuperar(Reloj.nuevoPomodoro(AJUSTES, "m1", null, null), AJUSTES, T0, 1).sesion());
    }

    @Test
    void saltearNoGuardaYSigueCorriendoSiCorria() {
        EstadoReloj e = Reloj.saltear(pomodoroCorriendo(), AJUSTES, T0 + 3 * MIN, el(T0 + 3 * MIN));
        assertEquals(PomodoroPhase.SHORT_BREAK, e.phase);
        assertEquals(0, e.ciclos);
        assertTrue(e.corriendo);
        assertEquals(5 * 60, Reloj.restante(e, T0 + 3 * MIN));
        assertEquals(PomodoroPhase.WORK, Reloj.saltear(e, AJUSTES, T0 + 4 * MIN, el(T0 + 4 * MIN)).phase);
    }

    @Test
    void cronometroDetenidoGuardaLoEstudiado() {
        EstadoReloj e = Reloj.iniciar(Reloj.nuevoCronometro(60, "m1", "Guía 3"), T0, el(T0));
        Reloj.Resultado r = Reloj.detener(e, T0 + 20 * MIN + 30_000);
        assertEquals(20, r.sesion().getMinutes());
        assertEquals("cronometro", r.sesion().getSource());
        assertEquals("Guía 3", r.sesion().getTopicTitle());
        assertTrue(r.sesion().getId().startsWith("cron_"));
        assertFalse(r.estado().corriendo);
        assertEquals(60 * 60, r.estado().restanteSeg);
    }

    @Test
    void cronometroTerminadoGuardaTodo() {
        EstadoReloj e = Reloj.iniciar(Reloj.nuevoCronometro(5, "m1", null), T0, el(T0));
        Reloj.Resultado r = Reloj.recuperar(e, AJUSTES, T0 + 6 * MIN, el(T0 + 6 * MIN));
        assertEquals(5, r.sesion().getMinutes());
        assertEquals("cronometro", r.sesion().getSource());
        assertTrue(r.avisar());
    }

    @Test
    void cronometroConMenosDeUnMinutoNoGuarda() {
        EstadoReloj e = Reloj.iniciar(Reloj.nuevoCronometro(60, "m1", null), T0, el(T0));
        assertNull(Reloj.detener(e, T0 + 59_000).sesion());
    }

    @Test
    void laNotaSeGuardaYDespuesQuedaVacia() {
        EstadoReloj e = pomodoroCorriendo();
        e.note = "guía 3";
        Reloj.Resultado r = Reloj.terminarFase(e, AJUSTES);
        assertEquals("guía 3", r.sesion().getNote());
        assertNull(r.estado().note);
        assertEquals("guía 3", e.note, "el estado original no se toca");
    }
    @Test
    void trasReinicioSeDetectaAunqueElTelefonoLleveMasTiempoPrendido() {
        EstadoReloj e = Reloj.latido(pomodoroCorriendo(), T0 + 12 * MIN, el(T0 + 12 * MIN));
        long otroArranque = T0 + 13 * MIN;   // se apagó a los 12 min y se prendió al toque, pero se desbloqueó tarde
        Reloj.Resultado r = Reloj.recuperar(e, AJUSTES, T0 + 60 * MIN, T0 + 60 * MIN - otroArranque);
        assertEquals(12, r.sesion().getMinutes());
        assertFalse(r.avisar());
    }

    @Test
    void trasReinicioNoSeGuardaMasQueLaFase() {
        EstadoReloj e = Reloj.latido(pomodoroCorriendo(), T0 + 40 * MIN, el(T0 + 40 * MIN));  // la alarma no sonó
        Reloj.Resultado r = Reloj.recuperar(e, AJUSTES, T0 + 60 * MIN, el2(T0 + 60 * MIN));
        assertEquals(25, r.sesion().getMinutes());
    }

    @Test
    void unLatidoDespuesDeUnReinicioNoBorraLaEvidencia() {
        EstadoReloj e = Reloj.latido(pomodoroCorriendo(), T0 + 12 * MIN, el(T0 + 12 * MIN));
        EstadoReloj tarde = Reloj.latido(e, T0 + 60 * MIN, el2(T0 + 60 * MIN));
        assertEquals(12, Reloj.recuperar(tarde, AJUSTES, T0 + 61 * MIN, el2(T0 + 61 * MIN)).sesion().getMinutes());
    }

    @Test
    void unRelojAtrasadoNoRestaEstudio() {
        EstadoReloj e = Reloj.pausar(pomodoroCorriendo(), T0 - 5 * MIN);
        assertEquals(0, e.estudiadoSeg);
    }
    @Test
    void conNotaCambiaSoloLaNotaLimpia() {
        EstadoReloj e = pomodoroCorriendo();
        EstadoReloj n = Reloj.conNota(e, "  guía 3 ");
        assertEquals("guía 3", n.note);
        assertEquals(e.finMs, n.finMs);
        assertNull(e.note);
        assertNull(Reloj.conNota(e, "   ").note);
    }
    @Test
    void cronometroDeUnaSesionDeLaGuiaUsaElIdDeLaPcConLaFechaDeFin() {
        long medianoche = java.time.LocalDate.of(2026, 11, 10).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        EstadoReloj e = Reloj.iniciar(Reloj.nuevoCronometro(60, "m1", "Bernoulli", "ss1"), medianoche - 20 * MIN, el(medianoche - 20 * MIN));
        Reloj.Resultado r = Reloj.detener(e, medianoche + 20 * MIN);
        assertTrue(r.sesion().getId().startsWith("cron_ss1_2026-11-10_"), r.sesion().getId());
        assertEquals("ss1", r.estado().sesionGuiaId, "la próxima vuelta sigue ligada");
        // el mismo estado recuperado dos veces da el mismo id (no duplica)
        EstadoReloj vencido = Reloj.iniciar(Reloj.nuevoCronometro(5, "m1", null, "ss1"), T0, el(T0));
        assertEquals(Reloj.recuperar(vencido, AJUSTES, T0 + 6 * MIN, el(T0 + 6 * MIN)).sesion().getId(),
                Reloj.recuperar(vencido, AJUSTES, T0 + 7 * MIN, el(T0 + 7 * MIN)).sesion().getId());
    }

}
