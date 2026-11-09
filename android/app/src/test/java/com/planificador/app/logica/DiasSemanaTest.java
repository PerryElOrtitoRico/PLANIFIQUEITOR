package com.planificador.app.logica;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiasSemanaTest {

    @Test
    void laClaveEsLaQueUsaLaGuiaDeLaPc() {
        assertEquals("lunes", DiasSemana.clave(LocalDate.of(2026, 11, 9)));
        assertEquals("miercoles", DiasSemana.clave(LocalDate.of(2026, 11, 11)));
        assertEquals("domingo", DiasSemana.clave(LocalDate.of(2026, 11, 15)));
    }

    @Test
    void nombreDeUnaClave() {
        assertEquals("Miércoles", DiasSemana.nombre("miercoles"));
        assertEquals("Lunes", DiasSemana.nombre("LUNES"));
    }
}
