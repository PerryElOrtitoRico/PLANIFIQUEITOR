package com.planificador.service;

import com.planificador.model.ImportantDate;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CountdownTest {

    static final LocalDate HOY = LocalDate.of(2026, 10, 25);

    static ImportantDate fecha(String id, String titulo, String iso) {
        return new ImportantDate(id, titulo, "s1", iso, "parcial", "");
    }

    @Test
    void cuentaLosDias() {
        // TODO T01
    }

    @Test
    void proximasOrdenadasYSinLasViejas() {
        // TODO T01
    }

    @Test
    void textoSegunLosDias() {
        // TODO T01
    }
}
