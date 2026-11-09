package com.planificador.app.ui;

import android.content.Context;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.planificador.app.logica.DiasSemana;
import com.planificador.model.StudySession;
import com.planificador.model.Subject;
import com.planificador.service.DataManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Alta, edición y borrado de una sesión de la guía, como WeeklyPanel/TodayPanel de la PC. */
final class SesionDialogo {

    private SesionDialogo() { }

    /** `sesion` null = nueva en el día `dia`. */
    static void abrir(Context c, StudySession sesion, String dia, Runnable despues) {
        List<Subject> materias = DataManager.getInstance().getSubjects();
        List<String> nombres = new ArrayList<>();
        for (Subject s : materias) nombres.add(s.getName());
        if (nombres.isEmpty()) nombres.add("General");
        int elegida = 0;
        for (int i = 0; i < materias.size(); i++) {
            if (sesion != null && materias.get(i).getId().equals(sesion.getSubjectId())) elegida = i;
        }
        String diaActual = sesion != null ? sesion.getDayOfWeek() : dia;
        int diaElegido = Math.max(0, Arrays.asList(DiasSemana.CLAVES).indexOf(diaActual.toLowerCase(java.util.Locale.ROOT)));

        Spinner materia = Vistas.selector(c, nombres, elegida);
        Spinner diaSel = Vistas.selector(c, Arrays.asList(DiasSemana.NOMBRES), diaElegido);
        EditText tema = Vistas.campo(c, "Tema / actividad", sesion != null ? sesion.getTopicDescription() : "", false);
        EditText minutos = Vistas.campo(c, "Minutos", String.valueOf(sesion != null ? sesion.getDurationMinutes() : 60), true);
        LinearLayout f = Vistas.columna(c);
        f.addView(Vistas.etiquetado(c, "Materia", materia));
        f.addView(Vistas.etiquetado(c, "Día", diaSel));
        f.addView(Vistas.etiquetado(c, "Tema / actividad", tema));
        f.addView(Vistas.etiquetado(c, "Duración (minutos)", minutos));

        Vistas.formulario(c, sesion == null ? "Agregar sesión (" + DiasSemana.nombre(dia) + ")" : "Editar sesión", f, () -> {
            String t = tema.getText().toString().trim();
            if (t.isEmpty()) return "El tema de estudio no puede estar vacío.";
            int min = Vistas.entero(minutos, 60);
            int i = materia.getSelectedItemPosition();
            String subjectId = materias.isEmpty() || i >= materias.size() ? "general" : materias.get(i).getId();
            String diaClave = DiasSemana.CLAVES[diaSel.getSelectedItemPosition()];
            DataManager dm = DataManager.getInstance();
            if (sesion == null) {
                dm.addStudySession(new StudySession("ss_" + System.currentTimeMillis(), diaClave, subjectId, t, min, false));
            } else {
                // Se busca de nuevo: si mientras el diálogo estaba abierto se recargaron los datos, el objeto viejo
                // ya no es el que se guarda.
                StudySession actual = buscar(sesion.getId());
                if (actual == null) return "La sesión ya no existe: se borró en otro dispositivo.";
                actual.setSubjectId(subjectId);
                actual.setDayOfWeek(diaClave);
                actual.setTopicDescription(t);
                actual.setDurationMinutes(min);
                dm.saveData();
            }
            despues.run();
            return null;
        });
    }

    /** La sesión de la guía con ese id en los datos actuales, o null. */
    static StudySession buscar(String id) {
        for (StudySession x : DataManager.getInstance().getStudySessions()) if (x.getId().equals(id)) return x;
        return null;
    }

    static void borrar(Context c, StudySession s, Runnable despues) {
        Vistas.confirmar(c, "¿Eliminar la sesión: " + s.getTopicDescription() + "?", () -> {
            DataManager.getInstance().removeStudySession(s.getId());
            despues.run();
        });
    }

    /** Tarjeta de una sesión: casilla de hecha, ▶ Cronómetro, Editar y Borrar. */
    static LinearLayout tarjeta(Context c, StudySession s, Runnable despues, Runnable cronometro) {
        Subject sub = DataManager.getInstance().getSubjectById(s.getSubjectId());
        String texto = (sub != null ? sub.getName() : "General") + " — " + s.getTopicDescription()
                + " (" + s.getDurationMinutes() + " min)";
        return Vistas.tarjeta(c, s.isCompleted(),
                Vistas.casilla(c, texto, s.isCompleted(), m -> {
                    StudySession actual = buscar(s.getId());
                    if (actual != null) DataManager.getInstance().setSessionCompleted(actual, m);
                    despues.run();
                }),
                Vistas.fila(c,
                        Vistas.boton(c, "▶ Cronómetro", cronometro),
                        Vistas.boton(c, "Editar", () -> abrir(c, s, s.getDayOfWeek(), despues)),
                        Vistas.boton(c, "Borrar", () -> borrar(c, s, despues))));
    }
}
