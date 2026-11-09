package com.planificador.app.ui;

import android.content.Context;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.planificador.model.Subject;
import com.planificador.model.Topic;
import com.planificador.model.Unit;
import com.planificador.service.DataManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/** Materias con sus unidades y temas, como SubjectsPanel de la PC. */
public class MateriasFragment extends PantallaBase {

    private final Set<String> abiertas = new HashSet<>();   // materias desplegadas

    @Override
    void dibujar(LinearLayout raiz) {
        Context c = requireContext();
        DataManager dm = DataManager.getInstance();
        raiz.addView(Vistas.titulo(c, "Materias"));
        raiz.addView(Vistas.boton(c, "Nueva materia", this::nuevaMateria));
        if (dm.getSubjects().isEmpty()) raiz.addView(Vistas.texto(c, "Todavía no hay materias."));
        for (Subject s : dm.getSubjects()) {
            boolean abierta = abiertas.contains(s.getId());
            LinearLayout t = Vistas.tarjeta(c, false,
                    Vistas.subtitulo(c, s.getName() + " · " + Math.round(s.getProgressPercentage()) + "%"),
                    Vistas.fila(c,
                            Vistas.boton(c, abierta ? "Ocultar" : "Ver", () -> {
                                if (!abiertas.remove(s.getId())) abiertas.add(s.getId());
                                refrescar();
                            }),
                            Vistas.boton(c, "Eliminar materia", () -> Vistas.confirmar(c,
                                    "¿Eliminar \"" + s.getName() + "\"? También se borran sus sesiones de la guía y sus fechas.",
                                    () -> { dm.removeSubject(s.getId()); refrescar(); }))));
            if (abierta) detalle(c, t, s);
            raiz.addView(t);
        }
    }

    private void detalle(Context c, LinearLayout t, Subject s) {
        String prof = s.getProfessor() == null || s.getProfessor().isEmpty() ? "Sin asignar" : s.getProfessor();
        t.addView(Vistas.texto(c, "Profesor/a: " + prof + (s.getSchedule() != null && !s.getSchedule().isEmpty()
                ? " · Horario: " + s.getSchedule() : "")));
        List<Unit> unidades = s.getUnits();
        if (unidades == null) {   // materia llegada sin lista de unidades: se edita desde la PC
            t.addView(Vistas.texto(c, "Sin unidades."));
            return;
        }
        for (int i = 0; i < unidades.size(); i++) {
            Unit u = unidades.get(i);
            int pos = i;
            t.addView(Vistas.subtitulo(c, "Unidad " + (i + 1) + ": " + u.getTitle()));
            t.addView(Vistas.fila(c,
                    Vistas.boton(c, "▲", () -> mover(unidades, pos, -1)),
                    Vistas.boton(c, "▼", () -> mover(unidades, pos, +1)),
                    Vistas.boton(c, "Renombrar", () -> pedirTexto(c, "Renombrar unidad", u.getTitle(), x -> {
                        u.setTitle(x);
                        guardar();
                    })),
                    Vistas.boton(c, "Eliminar", () -> {
                        int n = u.getTopics() == null ? 0 : u.getTopics().size();
                        Vistas.confirmar(c, "¿Eliminar \"" + u.getTitle() + "\"" + (n > 0 ? " y sus " + n + " temas" : "") + "?",
                                () -> { unidades.remove(u); guardar(); });
                    })));
            if (u.getTopics() != null) {
                for (Topic tema : u.getTopics()) {
                    t.addView(Vistas.casilla(c, tema.getTitle(), tema.isCompleted(), m -> {
                        tema.setCompleted(m);
                        guardar();
                    }));
                    t.addView(Vistas.fila(c,
                            Vistas.boton(c, "Renombrar", () -> pedirTexto(c, "Renombrar tema", tema.getTitle(), x -> {
                                tema.setTitle(x);
                                guardar();
                            })),
                            Vistas.boton(c, "Eliminar", () -> { u.getTopics().remove(tema); guardar(); })));
                }
                t.addView(Vistas.boton(c, "Agregar temas", () -> agregarTemas(c, u)));
            }
        }
        t.addView(Vistas.boton(c, "Agregar unidad", () -> pedirTexto(c, "Nueva unidad", "", x -> {
            unidades.add(new Unit("u_" + UUID.randomUUID(), unidades.size() + 1, x));
            guardar();
        })));
    }

    private void nuevaMateria() {
        Context c = requireContext();
        EditText nombre = Vistas.campo(c, "Nombre de la materia", "", false);
        EditText prof = Vistas.campo(c, "Profesor/a", "", false);
        EditText horario = Vistas.campo(c, "Horario de clases", "", false);
        LinearLayout f = Vistas.columna(c);
        f.addView(Vistas.etiquetado(c, "Nombre de la materia", nombre));
        f.addView(Vistas.etiquetado(c, "Profesor/a", prof));
        f.addView(Vistas.etiquetado(c, "Horario de clases", horario));
        Vistas.formulario(c, "Agregar nueva materia", f, () -> {
            String n = nombre.getText().toString().trim();
            if (n.isEmpty()) return "El nombre no puede estar vacío.";
            DataManager.getInstance().addSubject(new Subject("s_" + UUID.randomUUID(), n, "#4f46e5",
                    prof.getText().toString().trim(), horario.getText().toString().trim()));
            refrescar();
            return null;
        });
    }

    private void agregarTemas(Context c, Unit u) {
        EditText area = Vistas.campoMultilinea(c, "Un tema por línea");
        LinearLayout f = Vistas.columna(c);
        f.addView(Vistas.etiquetado(c, "Temas de \"" + u.getTitle() + "\" (uno por línea):", area));
        Vistas.formulario(c, "Agregar temas", f, () -> {
            for (String linea : area.getText().toString().split("\n")) {
                if (!linea.trim().isEmpty()) u.addTopic(new Topic("t_" + UUID.randomUUID(), linea.trim(), false));
            }
            guardar();
            return null;
        });
    }

    private void pedirTexto(Context c, String titulo, String actual, Consumer<String> alGuardar) {
        EditText e = Vistas.campo(c, titulo, actual, false);
        Vistas.formulario(c, titulo, e, () -> {
            String x = e.getText().toString().trim();
            if (x.isEmpty()) return "No puede quedar vacío.";
            alGuardar.accept(x);
            return null;
        });
    }

    private void mover(List<Unit> unidades, int i, int delta) {
        int j = i + delta;
        if (j < 0 || j >= unidades.size()) return;
        unidades.add(j, unidades.remove(i));
        guardar();
    }

    private void guardar() {
        DataManager.getInstance().saveData();
        refrescar();
    }
}
