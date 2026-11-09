package com.planificador.app.ui;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.planificador.app.R;
import com.planificador.app.datos.Datos;
import com.planificador.app.logica.ResumenHistorial;
import com.planificador.model.StudyLog;
import com.planificador.model.Subject;
import com.planificador.service.DataManager;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Calendario del mes con los minutos de cada día y, abajo, las sesiones del día elegido con su nota. */
public class HistorialFragment extends Fragment {

    private static final String[] DIAS = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
    private YearMonth mes = YearMonth.now();
    private LocalDate elegido = LocalDate.now();

    @Override
    public View onCreateView(@NonNull LayoutInflater inf, ViewGroup parent, Bundle b) {
        View v = inf.inflate(R.layout.fragment_historial, parent, false);
        v.findViewById(R.id.anterior).setOnClickListener(x -> { mes = mes.minusMonths(1); refrescar(); });
        v.findViewById(R.id.siguiente).setOnClickListener(x -> { mes = mes.plusMonths(1); refrescar(); });
        v.findViewById(R.id.hoy).setOnClickListener(x -> { mes = YearMonth.now(); elegido = LocalDate.now(); refrescar(); });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Datos.asegurar(requireContext())) DataManager.getInstance().recargar();   // lo que llegó por Syncthing
        refrescar();
    }

    private void refrescar() {
        if (!Datos.asegurar(requireContext())) return;
        List<StudyLog> logs = DataManager.getInstance().getStudyLogs();
        View v = requireView();
        String nombreMes = mes.getMonth().getDisplayName(TextStyle.FULL, new Locale("es"));
        v.<TextView>findViewById(R.id.mes).setText(nombreMes + " " + mes.getYear());

        GridLayout g = v.findViewById(R.id.grilla);
        g.removeAllViews();
        for (String d : DIAS) g.addView(celda(d, false, false));
        int hueco = mes.atDay(1).getDayOfWeek().getValue() - 1;
        for (int i = 0; i < hueco; i++) g.addView(celda("", false, false));
        Map<LocalDate, Integer> porDia = ResumenHistorial.minutosPorDia(logs, mes);
        for (int dia = 1; dia <= mes.lengthOfMonth(); dia++) {
            LocalDate fecha = mes.atDay(dia);
            Integer min = porDia.get(fecha);
            TextView c = celda(dia + "\n" + (min != null ? ResumenHistorial.formatear(min) : " "), min != null,
                    fecha.equals(elegido));
            c.setOnClickListener(x -> { elegido = fecha; refrescar(); });
            g.addView(c);
        }
        mostrarDia(logs);
    }

    private TextView celda(String texto, boolean conEstudio, boolean elegida) {
        TextView t = new TextView(requireContext());
        t.setText(texto);
        t.setGravity(Gravity.CENTER);
        t.setPadding(4, 12, 4, 12);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED),
                GridLayout.spec(GridLayout.UNDEFINED, 1f));
        lp.width = 0;
        lp.setMargins(2, 2, 2, 2);
        t.setLayoutParams(lp);
        if (conEstudio || elegida) {
            GradientDrawable fondo = new GradientDrawable();
            fondo.setCornerRadius(6);
            if (conEstudio) fondo.setStroke(4, Color.rgb(0x43, 0xA0, 0x47));
            if (elegida) fondo.setColor(Color.argb(60, 0x43, 0xA0, 0x47));
            t.setBackground(fondo);
        }
        return t;
    }

    private void mostrarDia(List<StudyLog> logs) {
        List<StudyLog> delDia = ResumenHistorial.delDia(logs, elegido);
        int total = delDia.stream().mapToInt(StudyLog::getMinutes).sum();
        requireView().<TextView>findViewById(R.id.titulo_dia).setText(elegido.getDayOfMonth() + "/"
                + elegido.getMonthValue() + "/" + elegido.getYear()
                + (total > 0 ? " — total " + ResumenHistorial.formatear(total) : " — no hay estudio registrado"));
        LinearLayout lista = requireView().findViewById(R.id.sesiones);
        lista.removeAllViews();
        for (StudyLog l : delDia) {
            Subject s = DataManager.getInstance().getSubjectById(l.getSubjectId());
            String materia = s != null ? s.getName() : (l.getSubjectId() == null ? "General" : "(materia eliminada)");
            TextView t = new TextView(requireContext());
            t.setPadding(8, 16, 8, 16);
            t.setText((l.getStartTime() == null ? "—" : l.getStartTime()) + "  " + materia
                    + (l.getTopicTitle() != null ? " · " + l.getTopicTitle() : "")
                    + "  " + l.getMinutes() + " min · " + origen(l)
                    + "\n" + (l.getNote() != null ? "📝 " + l.getNote() : "Tocá para agregar una nota"));
            t.setOnClickListener(x -> editarNota(l));
            lista.addView(t);
        }
    }

    private void editarNota(StudyLog l) {
        EditText campo = new EditText(requireContext());
        campo.setText(l.getNote() == null ? "" : l.getNote());
        new AlertDialog.Builder(requireContext())
                .setTitle("Nota de la sesión")
                .setView(campo)
                .setPositiveButton("Guardar", (d, w) -> {
                    DataManager.getInstance().setStudyLogNote(l.getId(), campo.getText().toString());
                    refrescar();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private static String origen(StudyLog l) {
        if (l.isFromGuide()) return "Guía semanal";
        return "cronometro".equals(l.getSource()) ? "Cronómetro" : "Pomodoro";
    }
}
