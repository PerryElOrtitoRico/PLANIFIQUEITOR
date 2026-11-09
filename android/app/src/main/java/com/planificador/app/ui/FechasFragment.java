package com.planificador.app.ui;

import android.content.Context;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.planificador.model.ImportantDate;
import com.planificador.model.Subject;
import com.planificador.service.Countdown;
import com.planificador.service.DataManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Fechas clave (parciales, finales, TP, entregas), como DatesPanel de la PC. */
public class FechasFragment extends PantallaBase {

    private static final String[] TIPOS = {"parcial", "final", "tp", "entrega"};

    @Override
    void dibujar(LinearLayout raiz) {
        Context c = requireContext();
        DataManager dm = DataManager.getInstance();
        raiz.addView(Vistas.titulo(c, "Fechas clave"));
        raiz.addView(Vistas.boton(c, "Agregar fecha", this::agregar));
        List<ImportantDate> lista = new ArrayList<>(dm.getImportantDates());
        // Las de fecha ilegible van al final (no rompen el orden).
        lista.sort(Comparator.comparing((ImportantDate d) -> Countdown.daysUntil(d.getDate(), LocalDate.now()),
                Comparator.nullsLast(Comparator.naturalOrder())));
        if (lista.isEmpty()) raiz.addView(Vistas.texto(c, "No hay fechas cargadas."));
        for (ImportantDate d : lista) {
            Subject s = dm.getSubjectById(d.getSubjectId());
            Long faltan = Countdown.daysUntil(d.getDate(), LocalDate.now());
            String cuando = faltan == null ? "Fecha inválida" : faltan == 0 ? "¡Hoy!"
                    : faltan > 0 ? "Faltan " + faltan + " días" : "Pasó hace " + Math.abs(faltan) + " días";
            android.widget.TextView t = Vistas.texto(c, cuando);
            if (faltan != null && faltan >= 0 && faltan <= 7) t.setTextColor(Vistas.ROJO);
            raiz.addView(Vistas.tarjeta(c, false,
                    Vistas.subtitulo(c, d.getTitle()),
                    Vistas.texto(c, (s != null ? s.getName() : "General") + " · " + d.getType() + " · " + d.getDate()),
                    t,
                    Vistas.boton(c, "Eliminar", () -> Vistas.confirmar(c, "¿Eliminar \"" + d.getTitle() + "\"?", () -> {
                        dm.removeImportantDate(d.getId());
                        refrescar();
                    }))));
        }
    }

    private void agregar() {
        Context c = requireContext();
        List<Subject> materias = DataManager.getInstance().getSubjects();
        List<String> nombres = new ArrayList<>();
        for (Subject s : materias) nombres.add(s.getName());
        if (nombres.isEmpty()) nombres.add("General");
        EditText titulo = Vistas.campo(c, "Título del examen o entrega", "", false);
        Spinner materia = Vistas.selector(c, nombres, 0);
        EditText fecha = Vistas.campo(c, "AAAA-MM-DD", LocalDate.now().toString(), false);
        Spinner tipo = Vistas.selector(c, Arrays.asList(TIPOS), 0);
        LinearLayout f = Vistas.columna(c);
        f.addView(Vistas.etiquetado(c, "Título", titulo));
        f.addView(Vistas.etiquetado(c, "Materia", materia));
        f.addView(Vistas.etiquetado(c, "Fecha (AAAA-MM-DD)", fecha));
        f.addView(Vistas.etiquetado(c, "Tipo", tipo));
        Vistas.formulario(c, "Nueva fecha importante", f, () -> {
            String t = titulo.getText().toString().trim();
            if (t.isEmpty()) return "El título no puede estar vacío.";
            String d = fecha.getText().toString().trim();
            if (Countdown.daysUntil(d, LocalDate.now()) == null) {
                return "La fecha no es válida. Usá el formato AAAA-MM-DD (por ejemplo " + LocalDate.now() + ").";
            }
            int i = materia.getSelectedItemPosition();
            String subjectId = materias.isEmpty() || i >= materias.size() ? "general" : materias.get(i).getId();
            DataManager.getInstance().addImportantDate(new ImportantDate("d_" + System.currentTimeMillis(), t, subjectId, d,
                    TIPOS[tipo.getSelectedItemPosition()], ""));
            refrescar();
            return null;
        });
    }
}
