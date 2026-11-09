package com.planificador.app.logica;

import com.planificador.model.Subject;
import com.planificador.model.Topic;
import com.planificador.model.Unit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpcionesRelojTest {

    static Subject materia() {
        Subject s = new Subject("m1", "Fluidos", "#000000", "", "");
        Unit u1 = new Unit("u1", 1, "Estática");
        u1.getTopics().add(new Topic("t1", "Presión", false));
        u1.getTopics().add(new Topic("t2", "Empuje", false));
        Unit u2 = new Unit("u2", 2, "Dinámica");
        u2.getTopics().add(new Topic("t3", "Bernoulli", false));
        u2.getTopics().add(new Topic("t4", "Viscosidad", false));
        s.getUnits().add(u1);
        s.getUnits().add(u2);
        return s;
    }

    @Test
    void materiasEnElOrdenDeLaLista() {
        assertEquals(List.of("Fluidos", "Química"),
                OpcionesReloj.materias(List.of(materia(), new Subject("m2", "Química", "#000000", "", ""))));
    }

    @Test
    void temasEmpiezanConSinTemaYSiguenLasUnidades() {
        assertEquals(List.of("(sin tema)", "Presión", "Empuje", "Bernoulli", "Viscosidad"), OpcionesReloj.temas(materia()));
        assertNull(OpcionesReloj.tema(materia(), 0));
        Topic t = OpcionesReloj.tema(materia(), 3);
        assertEquals("t3", t.getId());
    }

    @Test
    void unaMateriaSinUnidadesSoloTieneSinTema() {
        assertEquals(List.of("(sin tema)"), OpcionesReloj.temas(new Subject("m2", "Química", "#000000", "", "")));
    }
}
