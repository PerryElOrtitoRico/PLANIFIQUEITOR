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
        if (s.getUnits() == null) {   // materia llegada sin lista de unidades: se edita desde la PC
            t.addView(Vistas.texto(c, "Sin unidades."));
            return;
        }
        String sid = s.getId();
        List<Unit> lista = s.getUnits();
        for (int i = 0; i < lista.size(); i++) {
            Unit u = lista.get(i);
            String uid = u.getId();
            int pos = i;
            t.addView(Vistas.subtitulo(c, "Unidad " + (i + 1) + ": " + u.getTitle()));
            t.addView(Vistas.fila(c,
                    Vistas.boton(c, "▲", () -> mover(sid, pos, -1)),
                    Vistas.boton(c, "▼", () -> mover(sid, pos, +1)),
                    Vistas.boton(c, "Renombrar", () -> pedirTexto(c, "Renombrar unidad", u.getTitle(), x -> {
                        Unit a = unidad(sid, uid);
                        if (a == null) return BORRADA;
                        a.setTitle(x);
                        guardar();
                        return null;
                    })),
                    Vistas.boton(c, "Eliminar", () -> {
                        int n = u.getTopics() == null ? 0 : u.getTopics().size();
                        Vistas.confirmar(c, "¿Eliminar \"" + u.getTitle() + "\"" + (n > 0 ? " y sus " + n + " temas" : "") + "?",
                                () -> {
                                    List<Unit> us = unidades(sid);
                                    if (us != null) us.removeIf(x -> java.util.Objects.equals(uid, x.getId()));
                                    guardar();
                                });
                    })));
            if (u.getTopics() == null) continue;
            for (Topic tema : u.getTopics()) {
                String tid = tema.getId();
                t.addView(Vistas.casilla(c, tema.getTitle(), tema.isCompleted(), m -> {
                    Topic a = tema(sid, uid, tid);
                    if (a != null) a.setCompleted(m);
                    guardar();
                }));
                t.addView(Vistas.fila(c,
                        Vistas.boton(c, "Renombrar", () -> pedirTexto(c, "Renombrar tema", tema.getTitle(), x -> {
                            Topic a = tema(sid, uid, tid);
                            if (a == null) return BORRADA;
                            a.setTitle(x);
                            guardar();
                            return null;
                        })),
                        Vistas.boton(c, "Eliminar", () -> {
                            Unit a = unidad(sid, uid);
                            if (a != null && a.getTopics() != null) a.getTopics().removeIf(x -> java.util.Objects.equals(tid, x.getId()));
                            guardar();
                        })));
            }
            t.addView(Vistas.boton(c, "Agregar temas", () -> agregarTemas(c, sid, uid, u.getTitle())));
        }
        t.addView(Vistas.boton(c, "Agregar unidad", () -> pedirTexto(c, "Nueva unidad", "", x -> {
            List<Unit> us = unidades(sid);
            if (us == null) return BORRADA;
            us.add(new Unit("u_" + UUID.randomUUID(), us.size() + 1, x));
            guardar();
            return null;
        })));
    }

    // Se busca todo por id al guardar: si mientras un diálogo estaba abierto se recargaron los datos (Syncthing),
    // los objetos de la pantalla ya no son los que se guardan.
    private static final String BORRADA = "Ya no existe: se borró en otro dispositivo.";

    private static List<Unit> unidades(String sid) {
        Subject s = DataManager.getInstance().getSubjectById(sid);
        return s == null ? null : s.getUnits();
    }

    private static Unit unidad(String sid, String uid) {
        List<Unit> us = unidades(sid);
        if (us != null) for (Unit u : us) if (java.util.Objects.equals(uid, u.getId())) return u;
        return null;
    }

    private static Topic tema(String sid, String uid, String tid) {
        Unit u = unidad(sid, uid);
        if (u != null && u.getTopics() != null) for (Topic t : u.getTopics()) if (java.util.Objects.equals(tid, t.getId())) return t;
        return null;
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

    private void agregarTemas(Context c, String sid, String uid, String titulo) {
        EditText area = Vistas.campoMultilinea(c, "Un tema por línea");
        LinearLayout f = Vistas.columna(c);
        f.addView(Vistas.etiquetado(c, "Temas de \"" + titulo + "\" (uno por línea):", area));
        Vistas.formulario(c, "Agregar temas", f, () -> {
            Unit u = unidad(sid, uid);
            if (u == null || u.getTopics() == null) return BORRADA;
            for (String linea : area.getText().toString().split("\n")) {
                if (!linea.trim().isEmpty()) u.addTopic(new Topic("t_" + UUID.randomUUID(), linea.trim(), false));
            }
            guardar();
            return null;
        });
    }

    /** `alGuardar` devuelve un error para mostrar o null si guardó. */
    private void pedirTexto(Context c, String titulo, String actual, java.util.function.Function<String, String> alGuardar) {
        EditText e = Vistas.campo(c, titulo, actual, false);
        Vistas.formulario(c, titulo, e, () -> {
            String x = e.getText().toString().trim();
            if (x.isEmpty()) return "No puede quedar vacío.";
            return alGuardar.apply(x);
        });
    }

    private void mover(String sid, int i, int delta) {
        List<Unit> us = unidades(sid);
        int j = i + delta;
        if (us == null || i >= us.size() || j < 0 || j >= us.size()) return;
        us.add(j, us.remove(i));
        guardar();
    }

    private void guardar() {
        DataManager.getInstance().saveData();
        refrescar();
    }
}
