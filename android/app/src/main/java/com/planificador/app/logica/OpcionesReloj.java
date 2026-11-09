package com.planificador.app.logica;

import com.planificador.model.Subject;
import com.planificador.model.Topic;
import com.planificador.model.Unit;

import java.util.ArrayList;
import java.util.List;

/** Lo que muestran los selectores de materia y tema de la pantalla Reloj. */
public final class OpcionesReloj {

    private OpcionesReloj() { }

    public static List<String> materias(List<Subject> materias) {
        List<String> r = new ArrayList<>();
        for (Subject s : materias) r.add(s.getName());
        return r;
    }

    /** "(sin tema)" y después todos los temas de todas las unidades, en orden. */
    public static List<String> temas(Subject s) {
        List<String> r = new ArrayList<>();
        r.add("(sin tema)");
        for (Topic t : todos(s)) r.add(t.getTitle());
        return r;
    }

    /** El tema del índice del selector; null para "(sin tema)". */
    public static Topic tema(Subject s, int indice) {
        return indice <= 0 ? null : todos(s).get(indice - 1);
    }

    private static List<Topic> todos(Subject s) {
        List<Topic> r = new ArrayList<>();
        if (s.getUnits() == null) return r;   // una materia leída de un JSON sin unidades
        for (Unit u : s.getUnits()) if (u.getTopics() != null) r.addAll(u.getTopics());
        return r;
    }
}
