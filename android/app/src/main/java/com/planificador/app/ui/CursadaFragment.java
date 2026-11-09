package com.planificador.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.planificador.app.logica.FormularioCursada;
import com.planificador.model.AttendanceRecord;
import com.planificador.model.CourseRules;
import com.planificador.model.Evaluation;
import com.planificador.model.Subject;
import com.planificador.service.CourseCondition;
import com.planificador.service.CourseStatus;
import com.planificador.service.CourseStatusCalculator;
import com.planificador.service.DataManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Cursada de una materia: condición, reglas, notas y asistencia, como CoursePanel de la PC. */
public class CursadaFragment extends PantallaBase {

    private String elegida;   // id de la materia elegida

    @Override
    void dibujar(LinearLayout raiz) {
        Context c = requireContext();
        List<Subject> materias = DataManager.getInstance().getSubjects();
        raiz.addView(Vistas.titulo(c, "Cursada"));
        if (materias.isEmpty()) {
            raiz.addView(Vistas.texto(c, "No hay materias."));
            return;
        }
        int i = 0;
        List<String> nombres = new ArrayList<>();
        for (int k = 0; k < materias.size(); k++) {
            nombres.add(materias.get(k).getName());
            if (materias.get(k).getId().equals(elegida)) i = k;
        }
        Subject s = materias.get(i);
        elegida = s.getId();
        Spinner sel = Vistas.selector(c, nombres, i);
        sel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (!materias.get(pos).getId().equals(elegida)) { elegida = materias.get(pos).getId(); refrescar(); }
            }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });
        raiz.addView(sel);

        // Condición
        if (s.getCourse() == null) {
            raiz.addView(Vistas.texto(c, "Todavía no cargaste las reglas de cursada: tocá «Reglas de cursada»."));
        } else {
            CourseStatus st = CourseStatusCalculator.evaluate(s.getCourse());
            TextView cond = Vistas.subtitulo(c, nombre(st.condition()));
            cond.setTextColor(color(st.condition()));
            raiz.addView(cond);
            for (String linea : CourseStatusCalculator.describe(st, s.getCourse().getRules())) raiz.addView(Vistas.texto(c, linea));
        }
        raiz.addView(Vistas.boton(c, "Reglas de cursada", () -> reglas(s)));

        // Notas
        raiz.addView(Vistas.subtitulo(c, "Notas"));
        if (s.getCourse() != null) {
            for (Evaluation e : new ArrayList<>(s.getCourse().getEvaluations())) {
                boolean parcial = Evaluation.PARCIAL.equals(e.getKind());
                String t = (parcial ? "Parcial " : "TP ") + e.getNumber() + (parcial && e.isRetake() ? " (recuperatorio)" : "")
                        + ": " + CourseStatusCalculator.formatGrade(e.getGrade()) + (e.getDate() != null ? " · " + e.getDate() : "");
                raiz.addView(Vistas.fila(c, Vistas.texto(c, t), Vistas.boton(c, "Borrar", () -> Vistas.confirmar(c,
                        "¿Borrar " + t + "?", () -> {
                            Subject a = actual(s);
                            if (a != null && a.getCourse() != null) a.getCourse().getEvaluations().removeIf(x -> e.getId().equals(x.getId()));
                            guardar();
                        }))));
            }
        }
        raiz.addView(Vistas.boton(c, "Nota", () -> nota(s)));

        // Asistencia
        raiz.addView(Vistas.subtitulo(c, "Asistencia"));
        EditText fecha = Vistas.campo(c, "AAAA-MM-DD", LocalDate.now().toString(), false);
        raiz.addView(Vistas.fila(c, fecha,
                Vistas.boton(c, "Presente", () -> asistencia(s, fecha, true)),
                Vistas.boton(c, "Ausente", () -> asistencia(s, fecha, false))));
        if (s.getCourse() != null) {
            for (AttendanceRecord r : new ArrayList<>(s.getCourse().getAttendance())) {
                String t = r.getDate() + ": " + (r.isPresent() ? "Presente" : "Ausente");
                raiz.addView(Vistas.fila(c, Vistas.texto(c, t), Vistas.boton(c, "Borrar", () -> Vistas.confirmar(c,
                        "¿Borrar la asistencia del " + t + "?", () -> {
                            Subject a = actual(s);
                            if (a != null && a.getCourse() != null) {
                                List<AttendanceRecord> l = a.getCourse().getAttendance();
                                for (int k = 0; k < l.size(); k++) {
                                    AttendanceRecord x = l.get(k);
                                    if (x.isPresent() == r.isPresent() && java.util.Objects.equals(x.getDate(), r.getDate())) {
                                        l.remove(k);
                                        break;
                                    }
                                }
                            }
                            guardar();
                        }))));
            }
        }
    }

    private void reglas(Subject s) {
        Context c = requireContext();
        CourseRules r = s.getCourse() != null ? s.getCourse().getRules() : new CourseRules();
        EditText asist = Vistas.campo(c, "", fmt(r.getMinAttendancePercent()), true);
        EditText regular = Vistas.campo(c, "", fmt(r.getPassGrade()), true);
        EditText promo = Vistas.campo(c, "", fmt(r.getPromotionGrade()), true);
        EditText parciales = Vistas.campo(c, "", String.valueOf(r.getExamCount()), true);
        EditText clases = Vistas.campo(c, "", String.valueOf(r.getPlannedClasses()), true);
        LinearLayout casillas = Vistas.columna(c);
        List<Boolean> previos = r.getRetakes() == null ? new ArrayList<>() : new ArrayList<>(r.getRetakes());
        Runnable armar = () -> {
            List<Boolean> actuales = leer(casillas, previos);
            casillas.removeAllViews();
            int n = Math.max(0, Math.min(10, Vistas.entero(parciales, 0)));
            List<Boolean> rr = FormularioCursada.recuperatorios(n, actuales);
            for (int k = 0; k < n; k++) {
                CheckBox cb = Vistas.casilla(c, "Parcial " + (k + 1) + " con recuperatorio", rr.get(k), m -> { });
                casillas.addView(cb);
            }
        };
        armar.run();
        parciales.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence x, int a, int b, int d) { }
            @Override public void onTextChanged(CharSequence x, int a, int b, int d) { }
            @Override public void afterTextChanged(android.text.Editable x) { armar.run(); }
        });
        LinearLayout f = Vistas.columna(c);
        f.addView(Vistas.etiquetado(c, "Asistencia mínima (%)", asist));
        f.addView(Vistas.etiquetado(c, "Nota para regularizar", regular));
        f.addView(Vistas.etiquetado(c, "Nota para promocionar (0 = no hay)", promo));
        f.addView(Vistas.etiquetado(c, "Cantidad de parciales", parciales));
        f.addView(casillas);
        f.addView(Vistas.etiquetado(c, "Clases del cuatrimestre (0 = no sé)", clases));
        Vistas.formulario(c, "Reglas de cursada — " + s.getName(), f, () -> {
            try {
                CourseRules nuevas = FormularioCursada.reglas(texto(asist), texto(regular), texto(promo), texto(parciales),
                        leer(casillas, previos), texto(clases));
                Subject a = actual(s);
                if (a == null) return BORRADA;
                a.getOrCreateCourse().setRules(nuevas);
                guardar();
                return null;
            } catch (IllegalArgumentException ex) {
                return ex.getMessage();
            }
        });
    }

    private void nota(Subject s) {
        Context c = requireContext();
        Spinner tipo = Vistas.selector(c, Arrays.asList("Parcial", "TP"), 0);
        EditText numero = Vistas.campo(c, "", "1", true);
        CheckBox recup = Vistas.casilla(c, "Es el recuperatorio", false, m -> { });
        EditText nota = Vistas.campo(c, "", "6", true);
        EditText fecha = Vistas.campo(c, "AAAA-MM-DD (opcional)", LocalDate.now().toString(), false);
        LinearLayout f = Vistas.columna(c);
        f.addView(Vistas.etiquetado(c, "Tipo", tipo));
        f.addView(Vistas.etiquetado(c, "Número", numero));
        f.addView(recup);
        f.addView(Vistas.etiquetado(c, "Nota", nota));
        f.addView(Vistas.etiquetado(c, "Fecha (AAAA-MM-DD, opcional)", fecha));
        Vistas.formulario(c, "Nueva nota — " + s.getName(), f, () -> {
            try {
                Evaluation e = FormularioCursada.nota("ev_" + System.currentTimeMillis(), tipo.getSelectedItemPosition() == 0,
                        texto(numero), recup.isChecked(), texto(nota), texto(fecha));
                Subject a = actual(s);
                if (a == null) return BORRADA;
                a.getOrCreateCourse().getEvaluations().add(e);
                guardar();
                return null;
            } catch (IllegalArgumentException ex) {
                return ex.getMessage();
            }
        });
    }

    private void asistencia(Subject s, EditText fecha, boolean presente) {
        try {
            Subject a = actual(s);
            if (a == null) {
                Vistas.aviso(requireContext(), BORRADA);
                return;
            }
            a.getOrCreateCourse().getAttendance().add(FormularioCursada.asistencia(texto(fecha), presente));
            guardar();
        } catch (IllegalArgumentException ex) {
            Vistas.aviso(requireContext(), ex.getMessage());
        }
    }

    private static final String BORRADA = "La materia ya no existe: se borró en otro dispositivo.";

    /** La materia en los datos actuales (puede haberse recargado mientras un diálogo estaba abierto). */
    private static Subject actual(Subject s) {
        return DataManager.getInstance().getSubjectById(s.getId());
    }

    private static List<Boolean> leer(LinearLayout casillas, List<Boolean> previos) {
        if (casillas.getChildCount() == 0) return previos;
        List<Boolean> r = new ArrayList<>();
        for (int k = 0; k < casillas.getChildCount(); k++) r.add(((CheckBox) casillas.getChildAt(k)).isChecked());
        return r;
    }

    private static String texto(EditText e) {
        return e.getText().toString();
    }

    private static String fmt(double d) {
        return d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    private static String nombre(CourseCondition c) {
        switch (c) {
            case PROMOCION: return "Promoción";
            case REGULAR: return "Regular";
            case LIBRE: return "Libre";
            default: return "En curso";
        }
    }

    private static int color(CourseCondition c) {
        switch (c) {
            case PROMOCION: return Color.rgb(0x43, 0xA0, 0x47);
            case REGULAR: return Color.rgb(0x1E, 0x5B, 0xD8);
            case LIBRE: return Color.rgb(0xE5, 0x39, 0x35);
            default: return Color.rgb(0xF9, 0xA8, 0x25);
        }
    }

    private void guardar() {
        DataManager.getInstance().saveData();
        refrescar();
    }
}
