package com.planificador.app.ui;

import android.content.Context;
import android.widget.LinearLayout;

import com.planificador.model.Subject;
import com.planificador.service.DataManager;
import com.planificador.service.StudyTimeStats;
import com.planificador.service.SubjectWeekTime;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Tiempo: lo estudiado contra lo planeado por semana, y las últimas 8 semanas, como StudyTimePanel de la PC. */
public class TiempoFragment extends PantallaBase {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");
    private static final int SEMANAS = 8;
    private LocalDate semana = StudyTimeStats.weekStart(LocalDate.now());

    @Override
    void dibujar(LinearLayout raiz) {
        Context c = requireContext();
        DataManager dm = DataManager.getInstance();
        raiz.addView(Vistas.titulo(c, "Tiempo"));
        raiz.addView(Vistas.fila(c,
                Vistas.boton(c, "◀", () -> { semana = semana.minusWeeks(1); refrescar(); }),
                Vistas.boton(c, "Esta semana", () -> { semana = StudyTimeStats.weekStart(LocalDate.now()); refrescar(); }),
                Vistas.boton(c, "▶", () -> { semana = semana.plusWeeks(1); refrescar(); })));
        raiz.addView(Vistas.subtitulo(c, "Semana del " + semana.format(DIA) + " al " + semana.plusDays(6).format(DIA)));

        List<Subject> materias = dm.getSubjects();
        List<String> ids = new ArrayList<>();
        for (Subject s : materias) ids.add(s.getId());
        List<SubjectWeekTime> tiempos = StudyTimeStats.compareWeek(ids, dm.getStudyLogs(), dm.getStudySessions(), semana);
        if (materias.isEmpty()) raiz.addView(Vistas.texto(c, "No hay materias."));
        for (int i = 0; i < tiempos.size(); i++) {
            SubjectWeekTime t = tiempos.get(i);
            String avance = t.plannedMinutes() > 0 ? (t.actualMinutes() * 100 / t.plannedMinutes()) + " %" : "—";
            raiz.addView(Vistas.texto(c, materias.get(i).getName() + ": " + StudyTimeStats.formatMinutes(t.actualMinutes())
                    + " de " + StudyTimeStats.formatMinutes(t.plannedMinutes()) + " planeados (" + avance + ")"));
        }

        raiz.addView(Vistas.subtitulo(c, "Últimas " + SEMANAS + " semanas"));
        for (int w = SEMANAS - 1; w >= 0; w--) {
            LocalDate ws = semana.minusWeeks(w);
            Map<String, Integer> mins = StudyTimeStats.minutesBySubject(dm.getStudyLogs(), ws);
            StringBuilder detalle = new StringBuilder();
            int total = 0;
            for (Subject s : materias) {
                int x = mins.getOrDefault(s.getId(), 0);
                total += x;
                if (x > 0) detalle.append(detalle.length() > 0 ? " · " : "").append(s.getName()).append(" ")
                        .append(StudyTimeStats.formatMinutes(x));
            }
            raiz.addView(Vistas.texto(c, ws.format(DIA) + ": " + StudyTimeStats.formatMinutes(total)
                    + (detalle.length() > 0 ? " (" + detalle + ")" : "")));
        }
    }
}
