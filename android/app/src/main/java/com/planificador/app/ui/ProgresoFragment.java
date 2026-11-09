package com.planificador.app.ui;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.planificador.app.logica.ResumenProgreso;
import com.planificador.model.Subject;
import com.planificador.service.DataManager;

import java.util.Locale;

/** Progreso: cifras generales y avance de temas por materia, como StatsPanel de la PC. */
public class ProgresoFragment extends PantallaBase {

    @Override
    void dibujar(LinearLayout raiz) {
        Context c = requireContext();
        DataManager dm = DataManager.getInstance();
        ResumenProgreso.Resumen r = ResumenProgreso.de(dm.getSubjects(), dm.getStudySessions());
        int pct = r.temas() > 0 ? r.temasHechos() * 100 / r.temas() : 0;
        raiz.addView(Vistas.titulo(c, "Progreso"));
        raiz.addView(Vistas.texto(c, String.format(Locale.US, "Horas planificadas: %.1f h", r.minutosPlaneados() / 60.0)));
        raiz.addView(Vistas.texto(c, String.format(Locale.US, "Horas completadas: %.1f h", r.minutosHechos() / 60.0)));
        raiz.addView(Vistas.texto(c, "Temas dominados: " + r.temasHechos() + " / " + r.temas() + " (" + pct + "%)"));
        raiz.addView(Vistas.texto(c, "Materias activas: " + r.materias()));
        raiz.addView(Vistas.subtitulo(c, "Avance por materia"));
        if (dm.getSubjects().isEmpty()) raiz.addView(Vistas.texto(c, "No hay materias registradas."));
        for (Subject s : dm.getSubjects()) {
            int[] t = ResumenProgreso.temas(s);
            int p = t[0] > 0 ? t[1] * 100 / t[0] : 0;
            ProgressBar barra = new ProgressBar(c, null, android.R.attr.progressBarStyleHorizontal);
            barra.setMax(100);
            barra.setProgress(p);
            String prof = s.getProfessor() == null || s.getProfessor().isEmpty() ? "Sin asignar" : s.getProfessor();
            raiz.addView(Vistas.tarjeta(c, false,
                    Vistas.subtitulo(c, s.getName()),
                    Vistas.texto(c, "Profesor/a: " + prof + " · " + ResumenProgreso.minutosPorSemana(s.getId(), dm.getStudySessions()) + " min/semana"),
                    barra,
                    Vistas.texto(c, "Temas: " + t[1] + "/" + t[0] + " (" + p + "%)")));
        }
    }
}
