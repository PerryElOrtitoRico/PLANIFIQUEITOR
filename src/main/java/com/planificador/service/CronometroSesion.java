package com.planificador.service;

/**
 * Reloj de una sesión de estudio, sin Swing: no tiene hilo propio, quien lo maneja (el Pomodoro) le dice cuántos
 * segundos de estudio pasaron. Cuenta hacia atrás desde la duración de la sesión y se queda en cero.
 */
public final class CronometroSesion {

    private final int totalSegundos;
    private int restantes;

    public CronometroSesion(int minutos) {
        this.totalSegundos = Math.max(0, minutos) * 60;
        this.restantes = totalSegundos;
    }

    /** Suma segundos de estudio (nunca baja de cero). */
    public void avanzar(int segundos) {
        restantes = Math.max(0, restantes - Math.max(0, segundos));
    }

    public int restantes() { return restantes; }
    public int transcurridos() { return totalSegundos - restantes; }
    public boolean cumplida() { return restantes == 0; }
}
