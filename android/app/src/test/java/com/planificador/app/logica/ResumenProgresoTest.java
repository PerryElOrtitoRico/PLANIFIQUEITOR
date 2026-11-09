package com.planificador.app.logica;

import com.planificador.model.StudySession;
import com.planificador.model.Subject;
import com.planificador.model.Topic;
import com.planificador.model.Unit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResumenProgresoTest {

    @Test
    void lasCuentasDeStatsPanel() {
        Subject s = new Subject("m1", "Fluidos", "#000000", "", "");
        Unit u = new Unit("u1", 1, "Estática");
        u.getTopics().add(new Topic("t1", "Presión", true));
        u.getTopics().add(new Topic("t2", "Empuje", false));
        s.getUnits().add(u);
        StudySession hecha = new StudySession("g1", "lunes", "m1", "A", 60, false);
        hecha.setCompleted(true);
        StudySession pendiente = new StudySession("g2", "martes", "m1", "B", 30, false);
        ResumenProgreso.Resumen r = ResumenProgreso.de(List.of(s, new Subject("m2", "Química", "#000000", "", "")),
                List.of(hecha, pendiente));
        assertEquals(new ResumenProgreso.Resumen(90, 60, 2, 1, 2), r);
        assertEquals(90, ResumenProgreso.minutosPorSemana("m1", List.of(hecha, pendiente)));
    }

    @Test
    void materiaSinUnidadesNoRompe() {
        Subject s = new Subject("m1", "Fluidos", "#000000", "", "");
        assertEquals(0, ResumenProgreso.de(List.of(s), List.of()).temas());
    }
}
