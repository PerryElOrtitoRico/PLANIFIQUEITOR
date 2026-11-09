package com.planificador.app.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.planificador.app.R;
import com.planificador.app.datos.Datos;
import com.planificador.app.logica.EstadoReloj;
import com.planificador.app.logica.OpcionesReloj;
import com.planificador.app.logica.Reloj;
import com.planificador.model.PomodoroSettings;
import com.planificador.model.Subject;
import com.planificador.model.Topic;
import com.planificador.service.DataManager;
import com.planificador.service.PomodoroEngine;
import com.planificador.service.PomodoroPhase;

import java.util.ArrayList;
import java.util.List;

/** Pomodoro y cronómetro. La cuenta se calcula con la hora real; acá solo se muestra cada segundo. */
public class RelojFragment extends Fragment {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tic = this::tic;
    private List<Subject> materias = new ArrayList<>();
    private Spinner materia, tema;
    private EditText minutos, nota;
    private RadioButton pomodoro;
    private TextView fase, reloj, ciclos, sinMaterias;
    private Button iniciar, saltear, terminar;
    private boolean mostrando;   // evita que llenar los campos dispare sus oyentes
    private String materiaCargada;   // materia cuyos temas están en el selector (el Spinner avisa tarde)

    @Override
    public View onCreateView(@NonNull LayoutInflater inf, ViewGroup parent, Bundle b) {
        View v = inf.inflate(R.layout.fragment_reloj, parent, false);
        materia = v.findViewById(R.id.materia);
        tema = v.findViewById(R.id.tema);
        minutos = v.findViewById(R.id.minutos);
        nota = v.findViewById(R.id.nota);
        pomodoro = v.findViewById(R.id.modo_pomodoro);
        fase = v.findViewById(R.id.fase);
        reloj = v.findViewById(R.id.reloj);
        ciclos = v.findViewById(R.id.ciclos);
        sinMaterias = v.findViewById(R.id.sin_materias);
        iniciar = v.findViewById(R.id.iniciar);
        saltear = v.findViewById(R.id.saltear);
        terminar = v.findViewById(R.id.terminar);

        v.<android.widget.RadioGroup>findViewById(R.id.modo).setOnCheckedChangeListener((g, id) -> {
            if (mostrando) return;
            ControlReloj.cambiar(requireContext(), e -> new Reloj.Resultado(null, null, false)); // otro modo: de cero
            mostrar();
        });
        materia.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View view, int pos, long id) {
                Subject s = materiaElegida();
                if (!mostrando && s != null && !s.getId().equals(materiaCargada)) cargarTemas();
            }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });
        nota.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b2, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b2, int c) { }
            @Override public void afterTextChanged(Editable s) {
                if (mostrando) return;
                // La nota vive en el estado: así se guarda aunque la fase termine con la app cerrada.
                ControlReloj.cambiar(requireContext(), e -> new Reloj.Resultado(
                        e == null ? null : Reloj.conNota(e, s.toString()), null, false));
            }
        });
        iniciar.setOnClickListener(x -> iniciarOPausar());
        saltear.setOnClickListener(x -> {
            ControlReloj.cambiar(requireContext(), e -> new Reloj.Resultado(e == null ? null
                    : Reloj.saltear(e, ajustes(), System.currentTimeMillis(), SystemClock.elapsedRealtime()), null, false));
            mostrar();
        });
        terminar.setOnClickListener(x -> {
            EstadoReloj actual = ControlReloj.estado(requireContext());
            if (actual != null && fresco(actual)) {   // sin arrancar: se descarta (p. ej. el de una sesión de la guía)
                ControlReloj.cambiar(requireContext(), e -> new Reloj.Resultado(null, null, false));
                mostrar();
                return;
            }
            int[] guardados = {0};
            ControlReloj.cambiar(requireContext(), e -> {
                if (e == null) return new Reloj.Resultado(null, null, false);
                Reloj.Resultado r = Reloj.detener(e, System.currentTimeMillis());
                if (r.sesion() != null) guardados[0] = r.sesion().getMinutes();
                return r;
            });
            Toast.makeText(requireContext(), guardados[0] > 0 ? guardados[0] + " min guardados"
                    : "Menos de 1 minuto: no se guardó", Toast.LENGTH_SHORT).show();
            mostrar();
        });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!Datos.asegurar(requireContext())) return;
        materias = new ArrayList<>(DataManager.getInstance().getSubjects());
        mostrando = true;
        materia.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item,
                OpcionesReloj.materias(materias)));
        mostrando = false;
        cargarTemas();
        ControlReloj.recuperar(requireContext());
        mostrar();
        handler.post(tic);
    }

    @Override
    public void onPause() {
        handler.removeCallbacks(tic);
        super.onPause();
    }

    private void tic() {
        EstadoReloj e = ControlReloj.estado(requireContext());
        if (e != null && e.corriendo && Reloj.restante(e, System.currentTimeMillis()) == 0) {
            ControlReloj.recuperar(requireContext());   // terminó la fase: guarda, avisa y frena
            mostrar();
        } else {
            mostrarReloj(e);
        }
        handler.postDelayed(tic, 1000);
    }

    private void iniciarOPausar() {
        Subject s = materiaElegida();
        if (s == null) return;
        long ahora = System.currentTimeMillis();
        long elapsed = SystemClock.elapsedRealtime();
        ControlReloj.cambiar(requireContext(), e -> {
            if (e != null && e.corriendo) return new Reloj.Resultado(Reloj.pausar(e, ahora), null, false);
            if (e != null && !libre(e)) {
                // A medias, en descanso o ligado a una sesión de la guía: arranca tal cual (con la nota de pantalla).
                EstadoReloj c = fresco(e) ? Reloj.conNota(e, nota.getText().toString()) : e;
                return new Reloj.Resultado(Reloj.iniciar(c, ahora, elapsed), null, false);
            }
            // Fase de estudio sin empezar: se arma con lo elegido en pantalla.
            Topic t = OpcionesReloj.tema(s, tema.getSelectedItemPosition());
            EstadoReloj n = pomodoro.isChecked()
                    ? Reloj.nuevoPomodoro(ajustes(), s.getId(), t != null ? t.getId() : null, t != null ? t.getTitle() : null)
                    : Reloj.nuevoCronometro(minutosElegidos(), s.getId(), t != null ? t.getTitle() : null);
            if (e != null && pomodoro.isChecked() && !e.esCronometro()) n.ciclos = e.ciclos;
            n = Reloj.conNota(n, nota.getText().toString());
            return new Reloj.Resultado(Reloj.iniciar(n, ahora, elapsed), null, false);
        });
        mostrar();
    }

    /** Fase de estudio que todavía no arrancó. */
    private static boolean fresco(EstadoReloj e) {
        return !e.corriendo && e.esEstudio() && e.estudiadoSeg == 0 && e.restanteSeg == e.faseSeg;
    }

    /** Se puede cambiar materia, tema y minutos: fase sin empezar y no ligada a una sesión de la guía. */
    private static boolean libre(EstadoReloj e) {
        return e == null || (fresco(e) && e.prefijoId == null);
    }

    private void mostrar() {
        EstadoReloj e = ControlReloj.estado(requireContext());
        mostrando = true;
        boolean libre = libre(e);
        boolean crono = e != null ? e.esCronometro() : !pomodoro.isChecked();
        pomodoro.setChecked(!crono);
        requireView().<RadioButton>findViewById(R.id.modo_cronometro).setChecked(crono);
        requireView().findViewById(R.id.modo).setEnabled(libre);
        pomodoro.setEnabled(libre);
        requireView().findViewById(R.id.modo_cronometro).setEnabled(libre);
        if (e != null) seleccionar(e);
        materia.setEnabled(libre);
        tema.setEnabled(libre);
        minutos.setEnabled(libre);
        requireView().findViewById(R.id.fila_minutos).setVisibility(crono ? View.VISIBLE : View.GONE);
        if (e != null && !nota.getText().toString().equals(e.note == null ? "" : e.note) && !nota.hasFocus()) {
            nota.setText(e.note == null ? "" : e.note);
        }
        sinMaterias.setVisibility(materias.isEmpty() ? View.VISIBLE : View.GONE);
        iniciar.setEnabled(!materias.isEmpty());
        saltear.setVisibility(crono ? View.GONE : View.VISIBLE);
        terminar.setVisibility(crono ? View.VISIBLE : View.GONE);
        mostrando = false;
        mostrarReloj(e);
    }

    private void mostrarReloj(EstadoReloj e) {
        boolean crono = e != null ? e.esCronometro() : !pomodoro.isChecked();
        int seg = e != null ? Reloj.restante(e, System.currentTimeMillis())
                : crono ? minutosElegidos() * 60 : ajustes().getWorkMinutes() * 60;
        reloj.setText(PomodoroEngine.format(seg));
        fase.setText(crono ? "Cronómetro" : nombre(e == null ? PomodoroPhase.WORK : e.phase));
        ciclos.setText(crono ? "" : "Ciclos completos: " + (e == null ? 0 : e.ciclos));
        iniciar.setText(e != null && e.corriendo ? "⏸ Pausar" : "▶ Iniciar");
    }

    private static String nombre(PomodoroPhase p) {
        switch (p) {
            case SHORT_BREAK: return "Descanso corto";
            case LONG_BREAK: return "Descanso largo";
            default: return "Estudio";
        }
    }

    /** Pone en los selectores la materia y el tema del reloj en curso. */
    private void seleccionar(EstadoReloj e) {
        for (int i = 0; i < materias.size(); i++) {
            if (materias.get(i).getId().equals(e.subjectId)) {
                if (materia.getSelectedItemPosition() != i) {
                    materia.setSelection(i);
                    cargarTemas();
                }
                List<String> temas = OpcionesReloj.temas(materias.get(i));
                for (int j = 1; j < temas.size(); j++) {
                    Topic t = OpcionesReloj.tema(materias.get(i), j);
                    if (t.getId().equals(e.topicId)) tema.setSelection(j);
                }
                return;
            }
        }
    }

    private void cargarTemas() {
        Subject s = materiaElegida();
        List<String> t = s == null ? new ArrayList<>() : OpcionesReloj.temas(s);
        materiaCargada = s == null ? null : s.getId();
        tema.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, t));
    }

    private Subject materiaElegida() {
        int i = materia.getSelectedItemPosition();
        return i >= 0 && i < materias.size() ? materias.get(i) : null;
    }

    private int minutosElegidos() {
        try {
            return Math.max(1, Integer.parseInt(minutos.getText().toString().trim()));
        } catch (NumberFormatException ex) {
            return 60;
        }
    }

    private PomodoroSettings ajustes() {
        return Datos.ajustes(requireContext());
    }
}
