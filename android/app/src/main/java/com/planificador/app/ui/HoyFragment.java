package com.planificador.app.ui;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.planificador.app.logica.DiasSemana;
import com.planificador.model.ImportantDate;
import com.planificador.model.StudySession;
import com.planificador.model.Subject;
import com.planificador.service.Countdown;
import com.planificador.service.DataManager;

import java.time.LocalDate;
import java.util.List;

/** Hoy: la cuenta regresiva de las fechas de los próximos 30 días y las sesiones de la guía de hoy. */
public class HoyFragment extends PantallaBase {

    @Override
    void dibujar(LinearLayout raiz) {
        Context c = requireContext();
        DataManager dm = DataManager.getInstance();
        LocalDate hoy = LocalDate.now();
        String dia = DiasSemana.clave(hoy);
        raiz.addView(Vistas.titulo(c, "Hoy, " + DiasSemana.nombre(dia).toLowerCase() + " " + hoy.getDayOfMonth() + "/" + hoy.getMonthValue()));

        List<ImportantDate> proximas = Countdown.upcoming(dm.getImportantDates(), hoy, 30);
        if (!proximas.isEmpty()) {
            raiz.addView(Vistas.subtitulo(c, "Se viene"));
            for (ImportantDate d : proximas) {
                Subject s = dm.getSubjectById(d.getSubjectId());
                TextView t = Vistas.texto(c, Countdown.text(d, s != null ? s.getName() : null, hoy));
                Long faltan = Countdown.daysUntil(d.getDate(), hoy);
                if (faltan != null && faltan <= 7) t.setTextColor(Vistas.ROJO);
                raiz.addView(t);
            }
        }

        raiz.addView(Vistas.subtitulo(c, "Estudio de hoy"));
        boolean alguna = false;
        for (StudySession s : dm.getStudySessions()) {
            if (!dia.equalsIgnoreCase(s.getDayOfWeek())) continue;
            alguna = true;
            raiz.addView(SesionDialogo.tarjeta(c, s, this::refrescar, () -> ((MainActivity) requireActivity()).cronometroDe(s)));
        }
        if (!alguna) raiz.addView(Vistas.texto(c, "No tenés sesiones para hoy. Tocá \"Agregar estudio hoy\" para sumar una."));
        raiz.addView(Vistas.boton(c, "Agregar estudio hoy", () -> SesionDialogo.abrir(c, null, dia, this::refrescar)));
    }
}
