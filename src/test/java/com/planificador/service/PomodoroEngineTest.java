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
    }

    @Test
    void cicloCompletoConDescansoLargo() {
        // TODO T03a
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
