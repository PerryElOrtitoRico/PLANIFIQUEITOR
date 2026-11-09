package com.planificador.app.logica;

import com.planificador.model.PomodoroSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FormularioAjustesTest {

    @Test
    void ajustesValidos() {
        PomodoroSettings s = FormularioAjustes.ajustes("50", "10", "20", "3");
        assertEquals(50, s.getWorkMinutes());
        assertEquals(10, s.getShortBreakMinutes());
        assertEquals(20, s.getLongBreakMinutes());
        assertEquals(3, s.getCyclesBeforeLongBreak());
    }

    @Test
    void losRangosDeLaPc() {
        assertThrows(IllegalArgumentException.class, () -> FormularioAjustes.ajustes("0", "5", "15", "4"));
        assertThrows(IllegalArgumentException.class, () -> FormularioAjustes.ajustes("181", "5", "15", "4"));
        assertThrows(IllegalArgumentException.class, () -> FormularioAjustes.ajustes("25", "61", "15", "4"));
        assertThrows(IllegalArgumentException.class, () -> FormularioAjustes.ajustes("25", "5", "121", "4"));
        assertThrows(IllegalArgumentException.class, () -> FormularioAjustes.ajustes("25", "5", "15", "13"));
        assertThrows(IllegalArgumentException.class, () -> FormularioAjustes.ajustes("25,5", "5", "15", "4"));
    }
}
