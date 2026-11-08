<!-- modelo google/gemma-4-e4b-qat, temperatura 0.2, max_tokens 1000, razonamiento no -->

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
Completar los `// TODO T12b` de tres métodos que ya existen: `PomodoroPanel.saveCycle()`, `TimerDialog.registrar(Frame)` y `HistorialPanel.mostrarDetalle(List<StudyLog>)`. Devolvé cada uno de los tres métodos entero; el resto de cada método queda igual. Borrá la línea `// TODO T12b…`.

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
- `HistorialPanel.mostrarDetalle(List<StudyLog> logs)`:
  1. En el lugar del `// TODO T12b`: `filas.clear();`
  2. Los nombres de columna pasan a `new String[]{"Hora", "Materia", "Tema", "Minutos", "Origen", "Nota"}`.
  3. Dentro del `for`, justo antes de `m.addRow(...)`: `filas.add(l);`
  4. En el `m.addRow(new Object[]{...})`, después de `origen(l)` se agrega un elemento más: `l.getNote() == null ? "" : l.getNote()`.

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

```java
// src/main/java/com/planificador/ui/HistorialPanel.java
package com.planificador.ui;

import com.planificador.model.StudyLog;
import com.planificador.model.Subject;
import com.planificador.service.DataManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Calendario de lo que se estudió de verdad: cada día muestra los minutos totales y,
 * al elegirlo, el detalle (materia, tema, minutos y de dónde salió: Pomodoro o guía semanal).
 */
public class HistorialPanel extends JPanel {

    private static final String[] DIAS = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};

    private YearMonth mes = YearMonth.now();
    private LocalDate elegido = LocalDate.now();
    private final JLabel mesLabel = new JLabel("", SwingConstants.CENTER);
    private final JPanel grilla = new JPanel(new GridLayout(0, 7, 4, 4));
    private final JLabel detalleTitulo = new JLabel();
    private final JTable detalle = new JTable();
    private final List<StudyLog> filas = new ArrayList<>();   // sesiones del día elegido, en el orden de la tabla
    final JButton editarNota = new JButton("Editar nota…");

    public HistorialPanel() {
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Historial de estudio");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        JButton prev = new JButton("◀");
        JButton next = new JButton("▶");
        JButton hoy = new JButton("Hoy");
        prev.addActionListener(e -> { mes = mes.minusMonths(1); refresh(); });
        next.addActionListener(e -> { mes = mes.plusMonths(1); refresh(); });
        hoy.addActionListener(e -> { mes = YearMonth.now(); elegido = LocalDate.now(); refresh(); });
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        nav.add(prev);
        mesLabel.setPreferredSize(new Dimension(180, 24));
        nav.add(mesLabel);
        nav.add(next);
        nav.add(hoy);
        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.add(title, BorderLayout.NORTH);
        top.add(nav, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        detalle.setRowHeight(24);
        detalle.setFillsViewportHeight(true);
        JPanel abajo = new JPanel(new BorderLayout(0, 5));
        JPanel tituloDetalle = new JPanel(new BorderLayout());
        tituloDetalle.add(detalleTitulo, BorderLayout.CENTER);
        tituloDetalle.add(editarNota, BorderLayout.EAST);
        abajo.add(tituloDetalle, BorderLayout.NORTH);
        editarNota.setEnabled(false);
        editarNota.addActionListener(e -> editarNota());
        detalle.getSelectionModel().addListSelectionListener(e -> editarNota.setEnabled(detalle.getSelectedRow() >= 0));
        abajo.add(new JScrollPane(detalle), BorderLayout.CENTER);
        abajo.setPreferredSize(new Dimension(0, 220));

        add(grilla, BorderLayout.CENTER);
        add(abajo, BorderLayout.SOUTH);
        refresh();
    }

    public void refresh() {
        List<StudyLog> logs = DataManager.getInstance().getStudyLogs();
        Map<String, Integer> porDia = logs.stream().collect(
                Collectors.groupingBy(StudyLog::getDate, Collectors.summingInt(StudyLog::getMinutes)));

        mesLabel.setText(mes.getMonth().getDisplayName(TextStyle.FULL, new Locale("es")) + " " + mes.getYear());
        grilla.removeAll();
        for (String d : DIAS) {
            JLabel l = new JLabel(d, SwingConstants.CENTER);
            l.setForeground(UIManager.getColor("Label.disabledForeground"));
            grilla.add(l);
        }
        int hueco = mes.atDay(1).getDayOfWeek().getValue() - 1;
        for (int i = 0; i < hueco; i++) grilla.add(new JLabel());
        for (int dia = 1; dia <= mes.lengthOfMonth(); dia++) {
            LocalDate fecha = mes.atDay(dia);
            int min = porDia.getOrDefault(fecha.toString(), 0);
            JButton b = new JButton("<html><center>" + dia + (min > 0 ? "<br><small>" + formatear(min) + "</small>" : "<br><small>&nbsp;</small>") + "</center></html>");
            b.setFocusPainted(false);
            if (min > 0) b.setBorder(BorderFactory.createLineBorder(new Color(0x43A047), 2));
            if (fecha.equals(elegido)) b.setBackground(UIManager.getColor("Button.default.background"));
            if (fecha.equals(elegido)) b.setForeground(UIManager.getColor("Button.default.foreground"));
            b.addActionListener(e -> { elegido = fecha; refresh(); });
            grilla.add(b);
        }
        mostrarDetalle(logs);
        grilla.revalidate();
        grilla.repaint();
    }

    private void mostrarDetalle(List<StudyLog> logs) {
        // TODO T12b: columna Nota y lista filas
        DefaultTableModel m = new DefaultTableModel(new String[]{"Hora", "Materia", "Tema", "Minutos", "Origen"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        int total = 0;
        for (StudyLog l : logs) {
            if (!elegido.toString().equals(l.getDate())) continue;
            Subject s = DataManager.getInstance().getSubjectById(l.getSubjectId());
            m.addRow(new Object[]{l.getStartTime() == null ? "—" : l.getStartTime(),
                    s != null ? s.getName() : (l.getSubjectId() == null ? "General" : "(materia eliminada)"),
                    l.getTopicTitle() == null ? "" : l.getTopicTitle(),
                    l.getMinutes(), origen(l)});
            total += l.getMinutes();
        }
        detalle.setModel(m);
        detalleTitulo.setText(elegido.getDayOfMonth() + "/" + elegido.getMonthValue() + "/" + elegido.getYear()
                + (total > 0 ? " — total " + formatear(total) : " — no hay estudio registrado"));
    }

    /** Pide la nota de la fila elegida (con la actual como valor inicial) y la guarda. */
    private void editarNota() {
        int fila = detalle.getSelectedRow();
        if (fila < 0 || fila >= filas.size()) return;
        StudyLog log = filas.get(fila);
        String nueva = JOptionPane.showInputDialog(this, "Nota de la sesión:", log.getNote() == null ? "" : log.getNote());
        if (nueva == null) return;   // canceló
        DataManager.getInstance().setStudyLogNote(log.getId(), nueva);
        refresh();
    }

    private static String origen(StudyLog l) {
        if (l.isFromGuide()) return "Guía semanal";
        return "cronometro".equals(l.getSource()) ? "Cronómetro" : "Pomodoro";
    }

    static String formatear(int minutos) {
        return minutos >= 60 ? (minutos / 60) + "h" + (minutos % 60 > 0 ? String.format("%02d", minutos % 60) : "") : minutos + "m";
    }
}
```

Completá los `// TODO` que pide la tarea T12b. Devolvé **solo los métodos que completaste**, cada uno entero (firma y cuerpo), sin el resto de la clase: un bloque por archivo, con la ruta en la primera línea y después los métodos (y los imports nuevos, si agregaste). Archivos: src/main/java/com/planificador/ui/PomodoroPanel.java, src/main/java/com/planificador/ui/TimerDialog.java, src/main/java/com/planificador/ui/HistorialPanel.java.
