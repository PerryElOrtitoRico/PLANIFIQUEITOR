package com.planificador.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CronometroSesionTest {

    @Test
    void cuentaHaciaAtrasYSeQuedaEnCero() {
        CronometroSesion c = new CronometroSesion(1);
        assertEquals(60, c.restantes());
        c.avanzar(25);
        assertEquals(35, c.restantes());
        assertEquals(25, c.transcurridos());
        assertFalse(c.cumplida());
        c.avanzar(100);
        assertEquals(0, c.restantes());
        assertTrue(c.cumplida());
    }
}
