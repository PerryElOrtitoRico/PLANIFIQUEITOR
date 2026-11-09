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

    static String hora(long ms) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    static EstadoReloj pomodoroCorriendo() {
        return Reloj.iniciar(Reloj.nuevoPomodoro(AJUSTES, "m1", "t1", "Bernoulli"), T0, 1_000);
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
            e = Reloj.terminarFase(Reloj.iniciar(e, T0, 1_000), AJUSTES).estado();   // estudio
            if (i < 3) {
                assertEquals(PomodoroPhase.SHORT_BREAK, e.phase);
                e = Reloj.terminarFase(Reloj.iniciar(e, T0, 1_000), AJUSTES).estado(); // descanso
            }
        }
        assertEquals(PomodoroPhase.LONG_BREAK, e.phase);
        assertEquals(15 * 60, e.restanteSeg);
    }

    @Test
    void unDescansoTerminadoNoGuardaYCadaEstudioTieneOtroId() {
        Reloj.Resultado r1 = Reloj.terminarFase(pomodoroCorriendo(), AJUSTES);
        Reloj.Resultado r2 = Reloj.terminarFase(Reloj.iniciar(r1.estado(), T0 + 30 * MIN, 2_000), AJUSTES);
        assertNull(r2.sesion());
        assertTrue(r2.avisar());
        assertEquals(PomodoroPhase.WORK, r2.estado().phase);
        assertNotEquals(r1.sesion().getId(), r2.estado().idSesion);
    }

    @Test
    void elTiempoEnPausaNoCuenta() {
        EstadoReloj e = Reloj.pausar(pomodoroCorriendo(), T0 + 10 * MIN);
        assertEquals(15 * 60, Reloj.restante(e, T0 + 20 * MIN));
        e = Reloj.iniciar(e, T0 + 30 * MIN, 2_000);
        e = Reloj.latido(e, T0 + 35 * MIN, 5_000);
        StudyLog l = Reloj.recuperar(e, AJUSTES, T0 + 90 * MIN, 100).sesion();
        assertEquals(15, l.getMinutes());
    }

    @Test
    void trasReinicioGuardaHastaElUltimoLatido() {
        EstadoReloj e = Reloj.latido(pomodoroCorriendo(), T0 + 12 * MIN, 5_000);
        Reloj.Resultado r = Reloj.recuperar(e, AJUSTES, T0 + 60 * MIN, 100);
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
        EstadoReloj e = Reloj.latido(pomodoroCorriendo(), T0 + 50_000, 5_000);
        Reloj.Resultado r = Reloj.recuperar(e, AJUSTES, T0 + 60 * MIN, 100);
        assertNull(r.sesion());
        assertFalse(r.estado().corriendo);
    }

    @Test
    void trasReinicioEnUnDescansoNoGuarda() {
        EstadoReloj descanso = Reloj.terminarFase(pomodoroCorriendo(), AJUSTES).estado();
        EstadoReloj e = Reloj.latido(Reloj.iniciar(descanso, T0 + 25 * MIN, 2_000), T0 + 28 * MIN, 5_000);
        Reloj.Resultado r = Reloj.recuperar(e, AJUSTES, T0 + 60 * MIN, 100);
        assertNull(r.sesion());
        assertFalse(r.estado().corriendo);
    }

    @Test
    void recuperarDosVecesNoDuplica() {
        EstadoReloj e = Reloj.latido(pomodoroCorriendo(), T0 + 12 * MIN, 5_000);
        Reloj.Resultado r = Reloj.recuperar(e, AJUSTES, T0 + 60 * MIN, 100);
        assertNull(Reloj.recuperar(r.estado(), AJUSTES, T0 + 61 * MIN, 200).sesion());
        // el aviso de arranque y la app leyendo el mismo estado a la vez: mismo id, mismo archivo
        assertEquals(r.sesion().getId(), Reloj.recuperar(e, AJUSTES, T0 + 60 * MIN, 150).sesion().getId());
    }

    @Test
    void alAbrirConLaFaseVencidaLaTermina() {
        Reloj.Resultado r = Reloj.recuperar(pomodoroCorriendo(), AJUSTES, T0 + 40 * MIN, 5_000);
        assertEquals(25, r.sesion().getMinutes());
        assertEquals(hora(T0), r.sesion().getStartTime());
        assertEquals(PomodoroPhase.SHORT_BREAK, r.estado().phase);
        assertTrue(r.avisar());
    }

    @Test
    void recuperarConLaFaseEnCursoNoHaceNada() {
        EstadoReloj e = pomodoroCorriendo();
        Reloj.Resultado r = Reloj.recuperar(e, AJUSTES, T0 + 10 * MIN, 5_000);
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
        EstadoReloj e = Reloj.saltear(pomodoroCorriendo(), AJUSTES, T0 + 3 * MIN);
        assertEquals(PomodoroPhase.SHORT_BREAK, e.phase);
        assertEquals(0, e.ciclos);
        assertTrue(e.corriendo);
        assertEquals(5 * 60, Reloj.restante(e, T0 + 3 * MIN));
        assertEquals(PomodoroPhase.WORK, Reloj.saltear(e, AJUSTES, T0 + 4 * MIN).phase);
    }

    @Test
    void cronometroDetenidoGuardaLoEstudiado() {
        EstadoReloj e = Reloj.iniciar(Reloj.nuevoCronometro(60, "m1", "Guía 3"), T0, 1_000);
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
        EstadoReloj e = Reloj.iniciar(Reloj.nuevoCronometro(5, "m1", null), T0, 1_000);
        Reloj.Resultado r = Reloj.recuperar(e, AJUSTES, T0 + 6 * MIN, 5_000);
        assertEquals(5, r.sesion().getMinutes());
        assertEquals("cronometro", r.sesion().getSource());
        assertTrue(r.avisar());
    }

    @Test
    void cronometroConMenosDeUnMinutoNoGuarda() {
        EstadoReloj e = Reloj.iniciar(Reloj.nuevoCronometro(60, "m1", null), T0, 1_000);
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
}
