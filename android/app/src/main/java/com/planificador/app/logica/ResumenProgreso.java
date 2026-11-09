package com.planificador.app.logica;

import com.planificador.model.StudySession;
import com.planificador.model.Subject;
import com.planificador.model.Topic;
import com.planificador.model.Unit;

import java.util.List;

/** Las cifras de la pantalla Progreso, con las mismas cuentas que StatsPanel de la PC. */
public final class ResumenProgreso {

    public record Resumen(int minutosPlaneados, int minutosHechos, int temas, int temasHechos, int materias) { }

    private ResumenProgreso() { }

    public static Resumen de(List<Subject> materias, List<StudySession> guia) {
        int planeados = 0, hechos = 0, temas = 0, temasHechos = 0;
        for (StudySession s : guia) {
            planeados += s.getDurationMinutes();
            if (s.isCompleted()) hechos += s.getDurationMinutes();
        }
        for (Subject m : materias) {
            int[] t = temas(m);
            temas += t[0];
            temasHechos += t[1];
        }
        return new Resumen(planeados, hechos, temas, temasHechos, materias.size());
    }

    /** {temas, temas hechos} de una materia (0 y 0 si llegó sin unidades). */
    public static int[] temas(Subject m) {
        int total = 0, hechos = 0;
        if (m.getUnits() != null) {
            for (Unit u : m.getUnits()) {
                if (u.getTopics() == null) continue;
                for (Topic t : u.getTopics()) {
                    total++;
                    if (t.isCompleted()) hechos++;
                }
            }
        }
        return new int[]{total, hechos};
    }

    public static int minutosPorSemana(String subjectId, List<StudySession> guia) {
        int r = 0;
        for (StudySession s : guia) if (subjectId.equals(s.getSubjectId())) r += s.getDurationMinutes();
        return r;
    }
}
