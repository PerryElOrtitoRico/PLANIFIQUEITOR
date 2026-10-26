package com.planificador.ui;

import com.planificador.model.PomodoroSettings;
import com.planificador.model.StudyLog;
import com.planificador.model.Subject;
import com.planificador.model.Topic;
import com.planificador.model.Unit;
import com.planificador.service.DataManager;
import com.planificador.service.PomodoroEngine;
import com.planificador.service.PomodoroPhase;
import com.planificador.service.StudyLogFactory;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pomodoro: elijo materia (y tema, opcional) y corre ciclos de estudio y descanso.
 * Cada ciclo de estudio terminado se guarda como StudyLog. Al terminar cada fase el reloj se detiene y suena un aviso.
 */
public class PomodoroPanel extends JPanel {

    private final Runnable onSaved;               // se llama después de guardar un ciclo (para refrescar otras pantallas)
    final JComboBox<String> subjectCombo = new JComboBox<>();
    final JComboBox<String> topicCombo = new JComboBox<>();
    final JLabel timeLabel = new JLabel("25:00", SwingConstants.CENTER);
    final JLabel phaseLabel = new JLabel("Estudio", SwingConstants.CENTER);
    final JLabel cyclesLabel = new JLabel("Ciclos completos: 0", SwingConstants.CENTER);
    final JButton startButton = new JButton("▶ Iniciar");
    final JButton skipButton = new JButton("⏭ Saltear");
    final JButton settingsButton = new JButton("⚙ Ajustes");
    private final Timer timer = new Timer(1000, e -> onTick());

    private List<Subject> subjects = new ArrayList<>();
    private List<Topic> topics = new ArrayList<>();   // temas de la materia elegida, en el orden del combo (índice 0 = sin tema)
    PomodoroEngine engine;
    boolean running = false;

    public PomodoroPanel(Runnable onSaved) {
        this.onSaved = onSaved;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel choose = new JPanel(new GridLayout(2, 2, 8, 6));
        choose.add(new JLabel("Materia:"));
        choose.add(subjectCombo);
        choose.add(new JLabel("Tema (opcional):"));
        choose.add(topicCombo);
        add(choose, BorderLayout.NORTH);

        timeLabel.setFont(new Font("SansSerif", Font.BOLD, 56));
        phaseLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        JPanel center = new JPanel(new GridLayout(3, 1));
        center.add(phaseLabel);
        center.add(timeLabel);
        center.add(cyclesLabel);
        add(center, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout());
        startButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        buttons.add(startButton);
        buttons.add(skipButton);
        buttons.add(settingsButton);
        add(buttons, BorderLayout.SOUTH);

        startButton.addActionListener(e -> toggle());
        skipButton.addActionListener(e -> skipPhase());
        settingsButton.addActionListener(e -> editSettings());
        subjectCombo.addActionListener(e -> loadTopics());

        loadSubjects();
        engine = new PomodoroEngine(DataManager.getInstance().getPomodoroSettings());
        updateLabels();
    }

    /** Llena el combo de materias con DataManager.getInstance().getSubjects(). */
    private void loadSubjects() {
        subjects = new ArrayList<>(DataManager.getInstance().getSubjects());
        subjectCombo.removeAllItems();
        for (Subject s : subjects) subjectCombo.addItem(s.getName());
        loadTopics();
    }

    /** Llena el combo de temas con "(sin tema)" y los temas de todas las unidades de la materia elegida. */
    private void loadTopics() {
        topics = new ArrayList<>();
        topicCombo.removeAllItems();
        topicCombo.addItem("(sin tema)");
        Subject s = selectedSubject();
        if (s == null || s.getUnits() == null) return;
        for (Unit u : s.getUnits()) {
            for (Topic t : u.getTopics()) {
                topics.add(t);
                topicCombo.addItem(t.getTitle());
            }
        }
    }

    Subject selectedSubject() {
        int i = subjectCombo.getSelectedIndex();
        return (i >= 0 && i < subjects.size()) ? subjects.get(i) : null;
    }

    /** El tema elegido, o null si está elegido "(sin tema)". */
    Topic selectedTopic() {
        int i = topicCombo.getSelectedIndex() - 1;
        return (i >= 0 && i < topics.size()) ? topics.get(i) : null;
    }

    /** Un segundo del reloj (lo llama el Timer). */
    void onTick() {
        // TODO T07
        throw new UnsupportedOperationException();
    }

    /** Guarda el ciclo de estudio que acaba de terminar como StudyLog. */
    void saveCycle() {
        // TODO T07
        throw new UnsupportedOperationException();
    }

    /** Pone en las etiquetas el tiempo, la fase y los ciclos del engine. */
    void updateLabels() {
        // TODO T07
        throw new UnsupportedOperationException();
    }

    /** Iniciar / pausar. */
    void toggle() {
        if (running) {
            timer.stop();
            running = false;
            startButton.setText("▶ Seguir");
        } else {
            if (selectedSubject() == null) {
                JOptionPane.showMessageDialog(this, "Primero agregá una materia.", "Pomodoro", JOptionPane.WARNING_MESSAGE);
                return;
            }
            timer.start();
            running = true;
            startButton.setText("⏸ Pausar");
        }
    }

    /** Termina la fase actual sin guardarla. */
    void skipPhase() {
        engine.skip();
        updateLabels();
    }

    /** Detiene el reloj (al cerrar la ventana). */
    public void stop() {
        timer.stop();
        running = false;
    }

    /** Diálogo para cambiar los minutos; se guardan en DataManager y valen desde el próximo ciclo de estudio. */
    private void editSettings() {
        PomodoroSettings actual = DataManager.getInstance().getPomodoroSettings();
        JSpinner work = new JSpinner(new SpinnerNumberModel(actual.getWorkMinutes(), 1, 180, 1));
        JSpinner shortBreak = new JSpinner(new SpinnerNumberModel(actual.getShortBreakMinutes(), 1, 60, 1));
        JSpinner longBreak = new JSpinner(new SpinnerNumberModel(actual.getLongBreakMinutes(), 1, 120, 1));
        JSpinner every = new JSpinner(new SpinnerNumberModel(actual.getCyclesBeforeLongBreak(), 1, 12, 1));
        Object[] campos = {"Minutos de estudio:", work, "Descanso corto (min):", shortBreak,
                "Descanso largo (min):", longBreak, "Descanso largo cada (ciclos):", every};
        int op = JOptionPane.showConfirmDialog(this, campos, "Ajustes del Pomodoro", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (op != JOptionPane.OK_OPTION) return;
        PomodoroSettings nuevos = new PomodoroSettings((Integer) work.getValue(), (Integer) shortBreak.getValue(),
                (Integer) longBreak.getValue(), (Integer) every.getValue());
        DataManager.getInstance().setPomodoroSettings(nuevos);
        if (!running && engine.completedWorkCycles() == 0 && engine.phase() == PomodoroPhase.WORK) {
            engine = new PomodoroEngine(nuevos);
        }
        updateLabels();
    }

    /** Nombre de la fase para mostrar. */
    static String phaseName(PomodoroPhase p) {
        return switch (p) {
            case WORK -> "Estudio";
            case SHORT_BREAK -> "Descanso corto";
            case LONG_BREAK -> "Descanso largo";
        };
    }
}
