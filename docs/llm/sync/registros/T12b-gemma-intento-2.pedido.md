<!-- modelo google/gemma-4-e4b-qat, temperatura 0.2, max_tokens 1804, razonamiento no -->

## system

Sos un programador Java 17 cuidadoso. Te paso archivos con métodos marcados `// TODO`: completá solo los cuerpos que nombra la tarea; los métodos marcados "lo completa otra subtarea" quedan exactamente como están.
Reglas:
- No cambies package, imports que ya están (aunque todavía no se usen), nombres de clases, firmas ni métodos que ya están hechos. Podés agregar imports y métodos private.
- Las clases de model se leen con getters: log.getMinutes(), rules.getPassGrade(). Los records de service se leen con x(): status.condition(), a.percent(). No existen getCondition() ni getPercent().
- En los tests, copiá las aserciones de la tarea tal cual (mismos valores y fechas) y no escribas comentarios.
- No crees ni modifiques otros archivos.
- Devolvé solo los métodos que completaste, cada uno entero (firma y cuerpo), sin el resto de la clase: un bloque por archivo, con la ruta en la primera línea. Ejemplo:
```java
// src/main/java/com/planificador/service/Ejemplo.java
    public static int doble(int x) {
        return 2 * x;
    }
```
- Sin explicaciones fuera de los bloques.

## user

# Contrato del proyecto (extracto)

# ESTUDIEITOR, etapa carpeta sincronizada — Especificación (contrato)

## 1. Reglas de código
- Java 21, Maven, Gson. Paquete base `com.planificador`: `model` (datos), `service` (lógica y persistencia), `ui` (Swing). No toques `pom.xml` ni agregues dependencias.
- Nombres de clases y métodos como el código que ya existe; comentarios en español.
- Las clases de `model` tienen getters: `log.getId()`, `s.getName()`. Los **records** se leen con `x()`: `datos.subjects()`, `lectura.cambio()`. No existen `getSubjects()` en los records.
- Tests con JUnit 5 (`org.junit.jupiter.api.*`); archivos con `@TempDir`.

## 6. Nota de sesión
- `StudyLog` tiene `getNote()` / `setNote(String)`; null = sin nota. Las sesiones viejas no tienen nota.
- La nota se limpia siempre con `DataManager.limpiarNota(texto)`: recortada; vacía o null → null.
- Interfaz: `PomodoroPanel.noteField` y `TimerDialog.noteField` (`JTextField`); el Historial muestra la columna "Nota".

---

# T12b — Interfaz: guardar y mostrar la nota

## Objetivo
Completar los `// TODO T12b` de dos métodos que ya existen: `PomodoroPanel.saveCycle()` y `TimerDialog.registrar(Frame)`. Devolvé cada uno de los dos métodos entero; el resto de cada método queda igual. Borrá la línea `// TODO T12b…`.

## Cambios
- `PomodoroPanel.saveCycle()`: en el lugar del `// TODO T12b`, estas dos líneas:
```java
        log.setNote(DataManager.limpiarNota(noteField.getText()));
        noteField.setText("");
```
- `TimerDialog.registrar(Frame owner)`: en el lugar del `// TODO T12b`, esta línea:
```java
        log.setNote(com.planificador.service.DataManager.limpiarNota(noteField.getText()));
```

---

# Archivos actuales

```java
// src/main/java/com/planificador/ui/PomodoroPanel.java
package com.planificador.ui;

import com.planificador.model.PomodoroSettings;
import com.planificador.model.StudyLog;
import com.planificador.model.StudySession;
import com.planificador.model.Subject;
import com.planificador.model.Topic;
import com.planificador.model.Unit;
import com.planificador.service.CronometroSesion;
import com.planificador.service.DataManager;
import com.planificador.service.PomodoroEngine;
import com.planificador.service.PomodoroPhase;
import com.planificador.service.StudyLogFactory;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Pomodoro: elijo materia (y tema, opcional) y corre ciclos de estudio y descanso.
 * Cada ciclo de estudio terminado se guarda como StudyLog. Al terminar cada fase el reloj se detiene y suena un aviso.
 */
public class PomodoroPanel extends JPanel {

    private final Runnable onSaved;               // se llama después de guardar un ciclo (para refrescar otras pantallas)
    final JComboBox<String> subjectCombo = new JComboBox<>();
    final JComboBox<String> topicCombo = new JComboBox<>();
    final JComboBox<String> sessionCombo = new JComboBox<>();
    final JLabel sessionLabel = new JLabel(" ", SwingConstants.CENTER);
    final JLabel timeLabel = new JLabel("25:00", SwingConstants.CENTER);
    final JLabel phaseLabel = new JLabel("Estudio", SwingConstants.CENTER);
    final JLabel cyclesLabel = new JLabel("Ciclos completos: 0", SwingConstants.CENTER);
    final JTextField noteField = new JTextField();   // nota opcional que se guarda con el próximo ciclo de estudio
    final JButton startButton = new JButton("▶ Iniciar");
    final JButton skipButton = new JButton("⏭ Saltear");
    final JButton settingsButton = new JButton("Ajustes");
    private final Timer timer = new Timer(1000, e -> onTick());

    private List<Subject> subjects = new ArrayList<>();
    private List<Topic> topics = new ArrayList<>();   // temas de la materia elegida, en el orden del combo (índice 0 = sin tema)
    PomodoroEngine engine;
    boolean running = false;
    private Subject cycleSubject;                 // materia y tema con que arrancó el ciclo de estudio en curso
    private Topic cycleTopic;
    private String cycleDesc;                     // descripción de la sesión ligada con que arrancó el ciclo (o null)
    private List<StudySession> sessions = new ArrayList<>();   // sesiones de hoy, en el orden del combo (índice 0 = ninguna)
    private CronometroSesion cron;                // reloj de la sesión ligada: corre en las fases de estudio y para en los descansos
    private long lastTickNanos = System.nanoTime();
    private Consumer<String> alActualizar;        // recibe "fase mm:ss" cada vez que cambian las etiquetas (reloj flotante)

    public PomodoroPanel(Runnable onSaved) {
        this.onSaved = onSaved;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel choose = new JPanel(new GridLayout(4, 2, 8, 6));
        choose.add(new JLabel("Sesión de hoy (opcional):"));
        choose.add(sessionCombo);
        choose.add(new JLabel("Materia:"));
        choose.add(subjectCombo);
        choose.add(new JLabel("Tema (opcional):"));
        choose.add(topicCombo);
        choose.add(new JLabel("Nota de la sesión (opcional):"));
        choose.add(noteField);
        add(choose, BorderLayout.NORTH);

        timeLabel.setFont(new Font("SansSerif", Font.BOLD, 56));
        phaseLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        JPanel center = new JPanel(new GridLayout(4, 1));
        center.add(phaseLabel);
        center.add(timeLabel);
        center.add(sessionLabel);
        center.add(cyclesLabel);
        add(center, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(startButton);
        buttons.add(skipButton);
        buttons.add(settingsButton);
        add(buttons, BorderLayout.SOUTH);

        startButton.addActionListener(e -> toggle());
        skipButton.addActionListener(e -> skipPhase());
        settingsButton.addActionListener(e -> editSettings());
        subjectCombo.addActionListener(e -> loadTopics());
        sessionCombo.addActionListener(e -> sessionChosen());

        loadSubjects();
        engine = new PomodoroEngine(DataManager.getInstance().getPomodoroSettings());
        cargarSesiones(TodayPanel.getTodayDayKey());
        updateLabels();
    }

    /** Llena el combo de materias con DataManager.getInstance().getSubjects(). */
    private void loadSubjects() {
        subjects = new ArrayList<>(DataManager.getInstance().getSubjects());
        subjectCombo.removeAllItems();
        for (Subject s : subjects) subjectCombo.addItem(s.getName());
        loadTopics();
    }

    /** Llena el combo con las sesiones de la guía de ese día ("lunes", "martes"...). Sin elegir ninguna no se liga nada. */
    void cargarSesiones(String diaClave) {
        sessions = new ArrayList<>();
        sessionCombo.removeAllItems();
        sessionCombo.addItem("(ninguna)");
        for (StudySession ss : DataManager.getInstance().getStudySessions()) {
            if (!ss.getDayOfWeek().equalsIgnoreCase(diaClave)) continue;
            Subject sub = DataManager.getInstance().getSubjectById(ss.getSubjectId());
            sessions.add(ss);
            sessionCombo.addItem((sub != null ? sub.getName() : "General") + " - " + ss.getTopicDescription()
                    + " (" + ss.getDurationMinutes() + " min)");
        }
    }

    /** La sesión ligada, o null si está elegido "(ninguna)". */
    StudySession selectedSession() {
        int i = sessionCombo.getSelectedIndex() - 1;
        return (i >= 0 && i < sessions.size()) ? sessions.get(i) : null;
    }

    /** Al elegir una sesión: arranca su reloj y se alinea la materia; "(ninguna)" lo apaga. */
    private void sessionChosen() {
        StudySession ss = selectedSession();
        cron = ss != null ? new CronometroSesion(ss.getDurationMinutes()) : null;
        if (ss != null) {
            for (int i = 0; i < subjects.size(); i++) {
                if (subjects.get(i).getId().equals(ss.getSubjectId())) { subjectCombo.setSelectedIndex(i); break; }
            }
        }
        updateLabels();
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
        // Se cuenta contra el reloj del sistema: si el equipo se suspendió o la interfaz se trabó, no se pierde tiempo.
        long now = System.nanoTime();
        int n = (int) Math.max(1, Math.min(3600, Math.round((now - lastTickNanos) / 1e9)));
        lastTickNanos += n * 1_000_000_000L;
        PomodoroPhase finished = null;
        for (int i = 0; i < n && finished == null; i++) {
            if (cron != null && engine.phase() == PomodoroPhase.WORK) cron.avanzar(1);   // el reloj de la sesión solo corre estudiando
            finished = engine.tick();
        }
        if (finished == PomodoroPhase.WORK) saveCycle();
        if (finished != null) cycleSubject = null;
        updateLabels();
        if (finished != null) {
            timer.stop();
            running = false;
            startButton.setText("▶ Seguir");
            sessionCombo.setEnabled(true);
            subjectCombo.setEnabled(selectedSession() == null);
            topicCombo.setEnabled(selectedSession() == null);
            Toolkit.getDefaultToolkit().beep();
        }
    }

    /** Guarda el ciclo de estudio que acaba de terminar como StudyLog. */
    void saveCycle() {
        Subject s = cycleSubject != null ? cycleSubject : selectedSubject();
        if (s == null) return;
        Topic t = cycleSubject != null ? cycleTopic : selectedTopic();
        String titulo = t != null ? t.getTitle() : (cycleSubject != null ? cycleDesc : null);
        int minutes = engine.minutesFor(PomodoroPhase.WORK);
        StudyLog log = StudyLogFactory.fromWorkCycle("pl_" + java.util.UUID.randomUUID(), s.getId(), t != null ? t.getId() : null, titulo, java.time.LocalDateTime.now(), minutes);
        // TODO T12b: nota del campo
        DataManager.getInstance().addStudyLog(log);
        if (onSaved != null) onSaved.run();
    }

    /** Pone en las etiquetas el tiempo, la fase y los ciclos del engine. */
    void updateLabels() {
        timeLabel.setText(PomodoroEngine.format(engine.secondsLeft()));
        phaseLabel.setText(phaseName(engine.phase()));
        cyclesLabel.setText("Ciclos completos: " + engine.completedWorkCycles());
        String sesion = cron == null ? null
                : cron.cumplida() ? "Sesión cumplida" : "Sesión: " + PomodoroEngine.format(cron.restantes());
        sessionLabel.setText(sesion != null ? sesion : " ");
        if (alActualizar != null) {
            alActualizar.accept(phaseLabel.getText() + "  " + timeLabel.getText() + (sesion != null ? "  |  " + sesion : ""));
        }
    }

    /** Avisa el texto del reloj en cada actualización; se llama una vez al registrar para mostrar el valor actual. */
    public void setAlActualizar(Consumer<String> alActualizar) {
        this.alActualizar = alActualizar;
        updateLabels();
    }

    /** Iniciar / pausar. */
    void toggle() {
        if (running) {
            timer.stop();
            running = false;
            startButton.setText("▶ Seguir");
            sessionCombo.setEnabled(engine.phase() != PomodoroPhase.WORK || cycleSubject == null);
        } else {
            if (selectedSubject() == null) {
                JOptionPane.showMessageDialog(this, "Primero agregá una materia.", "Pomodoro", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (engine.phase() == PomodoroPhase.WORK && cycleSubject == null) {
                cycleSubject = selectedSubject();
                cycleTopic = selectedSession() != null ? null : selectedTopic();
                cycleDesc = selectedSession() != null ? selectedSession().getTopicDescription() : null;
            }
            subjectCombo.setEnabled(cycleSubject == null && selectedSession() == null);
            topicCombo.setEnabled(cycleSubject == null && selectedSession() == null);
            sessionCombo.setEnabled(false);
            lastTickNanos = System.nanoTime();
            timer.start();
            running = true;
            startButton.setText("⏸ Pausar");
        }
    }

    /** Termina la fase actual sin guardarla. */
    void skipPhase() {
        engine.skip();
        cycleSubject = null;
        subjectCombo.setEnabled(selectedSession() == null);
        topicCombo.setEnabled(selectedSession() == null);
        sessionCombo.setEnabled(true);
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
```

```java
// src/main/java/com/planificador/ui/TimerDialog.java
package com.planificador.ui;

import javax.swing.*;
import java.awt.*;

public class TimerDialog extends JDialog {
    private int secondsRemaining;
    private Timer timer;
    private JLabel timerLabel;
    private JButton startPauseBtn;
    private boolean isRunning = false;
    private long endNanos;
    private final int initialSeconds;
    private final String subjectId;      // puede ser null (cronómetro suelto)
    private final String description;    // puede ser null
    private final String sessionId;      // sesión de la guía de la que salió, o null
    private boolean registrado = false;
    final JTextField noteField = new JTextField(20);   // nota opcional de la sesión

    public TimerDialog(Frame owner, int initialMinutes) {
        this(owner, initialMinutes, null, null, null);
    }

    /** Al cerrarse, los minutos cronometrados se guardan en el historial (con la materia y la sesión si las hay). */
    public TimerDialog(Frame owner, int initialMinutes, String subjectId, String description, String sessionId) {
        super(owner, "Sesión de Estudio (Cronómetro)", true);
        this.secondsRemaining = initialMinutes * 60;
        this.initialSeconds = secondsRemaining;
        this.subjectId = subjectId;
        this.description = description;
        this.sessionId = sessionId;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { registrar(owner); }
        });

        setSize(380, 240);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(15, 15));

        JPanel centerPanel = new JPanel(new GridBagLayout());
        timerLabel = new JLabel(formatTime(secondsRemaining));
        timerLabel.setFont(new Font("SansSerif", Font.BOLD, 48));
        centerPanel.add(timerLabel);
        add(centerPanel, BorderLayout.CENTER);
        JPanel notaPanel = new JPanel(new BorderLayout(6, 0));
        notaPanel.add(new JLabel("Nota (opcional):"), BorderLayout.WEST);
        notaPanel.add(noteField, BorderLayout.CENTER);
        notaPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 15));
        add(notaPanel, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new FlowLayout());
        startPauseBtn = new JButton("▶ Iniciar");
        startPauseBtn.addActionListener(e -> toggleTimer());

        JButton closeBtn = new JButton("Cerrar");
        closeBtn.addActionListener(e -> {
            if (timer != null) timer.stop();
            dispose();
        });

        btnPanel.add(startPauseBtn);
        btnPanel.add(closeBtn);
        add(btnPanel, BorderLayout.SOUTH);

        timer = new Timer(1000, e -> {
            secondsRemaining = (int) Math.max(0, Math.ceil((endNanos - System.nanoTime()) / 1e9));
            if (secondsRemaining > 0) {
                timerLabel.setText(formatTime(secondsRemaining));
            } else {
                timer.stop();
                JOptionPane.showMessageDialog(this, "¡Tiempo cumplido! Excelente sesión de estudio.");
                dispose();
            }
        });
    }

    private void toggleTimer() {
        if (isRunning) {
            timer.stop();
            startPauseBtn.setText("▶ Reanudar");
            isRunning = false;
        } else {
            endNanos = System.nanoTime() + secondsRemaining * 1_000_000_000L;
            timer.start();
            startPauseBtn.setText("⏸Pausar");
            isRunning = true;
        }
    }

    private String formatTime(int totalSecs) {
        int m = totalSecs / 60;
        int s = totalSecs % 60;
        return String.format("%02d:%02d", m, s);
    }

    /** Guarda en el historial lo cronometrado (mínimo 1 minuto). */
    private void registrar(Frame owner) {
        if (registrado) return;
        registrado = true;
        if (timer != null) timer.stop();
        int minutos = (initialSeconds - secondsRemaining) / 60;
        if (minutos < 1) return;
        java.time.LocalDateTime fin = java.time.LocalDateTime.now();
        String id = (sessionId != null ? "cron_" + sessionId + "_" + fin.toLocalDate() + "_" : "cron_") + java.util.UUID.randomUUID();
        com.planificador.model.StudyLog log = com.planificador.service.StudyLogFactory.fromWorkCycle(id, subjectId, null, description, fin, minutos);
        log.setSource("cronometro");
        // TODO T12b: nota del campo
        com.planificador.service.DataManager.getInstance().addStudyLog(log);
        if (owner instanceof MainWindow mw) mw.refreshAllPanels();
    }
}
```

Completá los `// TODO` que pide la tarea T12b. Devolvé **solo los métodos que completaste**, cada uno entero (firma y cuerpo), sin el resto de la clase: un bloque por archivo, con la ruta en la primera línea y después los métodos (y los imports nuevos, si agregaste). Archivos: src/main/java/com/planificador/ui/PomodoroPanel.java, src/main/java/com/planificador/ui/TimerDialog.java.
