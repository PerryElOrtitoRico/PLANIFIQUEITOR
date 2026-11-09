package com.planificador.app.ui;

import android.content.Context;
import android.widget.LinearLayout;

import com.planificador.app.logica.DiasSemana;
import com.planificador.model.StudySession;
import com.planificador.service.DataManager;
import com.planificador.service.StudyTimeStats;

/** Guía semanal: los 7 días con sus sesiones planeadas. */
public class GuiaFragment extends PantallaBase {

    @Override
    void dibujar(LinearLayout raiz) {
        Context c = requireContext();
        MainActivity a = (MainActivity) requireActivity();
        raiz.addView(Vistas.titulo(c, "Guía semanal"));
        for (int i = 0; i < DiasSemana.CLAVES.length; i++) {
            String dia = DiasSemana.CLAVES[i];
            int total = 0;
            for (StudySession s : DataManager.getInstance().getStudySessions()) {
                if (dia.equalsIgnoreCase(s.getDayOfWeek())) total += s.getDurationMinutes();
            }
            raiz.addView(Vistas.fila(c,
                    Vistas.subtitulo(c, DiasSemana.NOMBRES[i] + (total > 0 ? " · " + StudyTimeStats.formatMinutes(total) : "")),
                    Vistas.boton(c, "+", () -> SesionDialogo.abrir(c, null, dia, this::refrescar))));
            for (StudySession s : DataManager.getInstance().getStudySessions()) {
                if (dia.equalsIgnoreCase(s.getDayOfWeek())) {
                    raiz.addView(SesionDialogo.tarjeta(c, s, this::refrescar, () -> a.cronometroDe(s)));
                }
            }
        }
    }
}
