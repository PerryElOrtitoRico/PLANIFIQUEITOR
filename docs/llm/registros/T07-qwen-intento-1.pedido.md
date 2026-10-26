<!-- modelo qwen/qwen3.5-9b, temperatura 0.2, max_tokens 3986, razonamiento no -->

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

# PLANIFICADOR, etapa Pomodoro y cursada — Especificación (contrato)

## 1. Reglas de código
- Java 17 (el `pom.xml` compila con `source/target` 17), Maven. Paquete base `com.planificador`: `model` (datos), `service` (lógica y persistencia), `ui` (Swing). No toques `pom.xml` ni agregues dependencias.
- Nombres de clases y métodos en inglés, como el código que ya existe; comentarios en español.
- Las clases de `model` tienen getters: `log.getMinutes()`, `rules.getPassGrade()`, `ev.isRetake()`. Los **records** de `service` se leen con `x()`: `status.condition()`, `a.percent()`. No existen `getCondition()` ni `getPercent()`.
- La lógica no usa `LocalDate.now()`: la fecha de hoy se pasa por parámetro (así se testea).
- Tests con JUnit 5 (`org.junit.jupiter.api.*`); archivos con `@TempDir`. Ningún test lee ni escribe `~/.local/share/planificador-estudio`.

## 3. Pomodoro
- Empieza en `WORK`. Al terminar un `WORK` se suma un ciclo; si la cantidad de ciclos es múltiplo de `cyclesBeforeLongBreak` sigue `LONG_BREAK`, si no `SHORT_BREAK`. Después de un descanso sigue `WORK`.
- Cada `WORK` terminado se guarda como un `StudyLog` (materia, tema opcional, día y hora de inicio, minutos = `workMinutes`). Saltear una fase no guarda nada.

---

# T07 — Pomodoro: el reloj de la ventana

## Objetivo
Completar **solo** `onTick`, `saveCycle` y `updateLabels` de `PomodoroPanel` y los tests `guardaElCicloAlTerminar` y `sinTemaGuardaNull`. El resto de la clase (combos, botones, `toggle`, ajustes) ya está hecho.

## Código que ya existe
- Campos: `engine` (`PomodoroEngine`), `timer` (`javax.swing.Timer`), `running`, `onSaved` (`Runnable`, puede ser null), las etiquetas `timeLabel`, `phaseLabel`, `cyclesLabel` y el botón `startButton`.
- `selectedSubject()` (`Subject` o null) y `selectedTopic()` (`Topic` o null); `phaseName(fase)` devuelve "Estudio", "Descanso corto" o "Descanso largo".
- `PomodoroEngine`: `engine.tick()` (devuelve la fase que terminó o null), `engine.secondsLeft()`, `engine.phase()`, `engine.completedWorkCycles()`, `engine.minutesFor(PomodoroPhase.WORK)`, `PomodoroEngine.format(segundos)` ("MM:SS").
- `StudyLogFactory.fromWorkCycle(id, subjectId, topicId, topicTitle, LocalDateTime end, int minutes)` devuelve el `StudyLog`. `DataManager.getInstance().addStudyLog(log)` lo guarda.
- `Subject`: `s.getId()`. `Topic`: `t.getId()`, `t.getTitle()`.

## Métodos (pasos en orden)
- `onTick()`:
  1. `PomodoroPhase finished = engine.tick();`
  2. `if (finished == PomodoroPhase.WORK) saveCycle();`
  3. `updateLabels();`
  4. `if (finished != null) { timer.stop(); running = false; startButton.setText("▶ Seguir"); Toolkit.getDefaultToolkit().beep(); }`
- `saveCycle()`:
  1. `Subject s = selectedSubject(); if (s == null) return;`
  2. `Topic t = selectedTopic();`
  3. `int minutes = engine.minutesFor(PomodoroPhase.WORK);`
  4. `StudyLog log = StudyLogFactory.fromWorkCycle("pl_" + System.currentTimeMillis(), s.getId(), t != null ? t.getId() : null, t != null ? t.getTitle() : null, LocalDateTime.now(), minutes);`
  5. `DataManager.getInstance().addStudyLog(log);`
  6. `if (onSaved != null) onSaved.run();`
- `updateLabels()`:
  1. `timeLabel.setText(PomodoroEngine.format(engine.secondsLeft()));`
  2. `phaseLabel.setText(phaseName(engine.phase()));`
  3. `cyclesLabel.setText("Ciclos completos: " + engine.completedWorkCycles());`

## Tests (cada uno empieza con `DataManager dm = UiDatos.cargar(tmp); dm.setPomodoroSettings(new PomodoroSettings(1, 1, 2, 2));` y `PomodoroPanel p = new PomodoroPanel(null);`; los datos ya traen 3 `StudyLog`; la materia 0 es "s1" y el tema 1 del combo es "Bernoulli" con id "t1")
- `guardaElCicloAlTerminar`: `p.subjectCombo.setSelectedIndex(0); p.topicCombo.setSelectedIndex(1);` y `for (int i = 0; i < 60; i++) p.onTick();` → `assertEquals(4, dm.getStudyLogs().size());`, `StudyLog l = dm.getStudyLogs().get(3);` `assertEquals("s1", l.getSubjectId());` `assertEquals("t1", l.getTopicId());` `assertEquals(1, l.getMinutes());` y `assertEquals("Descanso corto", p.phaseLabel.getText());`
- `sinTemaGuardaNull`: `p.subjectCombo.setSelectedIndex(0); p.topicCombo.setSelectedIndex(0);` y 60 veces `p.onTick();` → `assertNull(dm.getStudyLogs().get(3).getTopicId());` y `assertEquals("Ciclos completos: 1", p.cyclesLabel.getText());`

---

# Archivos actuales

```java
// src/main/java/com/planificador/ui/PomodoroPanel.java
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
```

```java
// src/test/java/com/planificador/ui/PomodoroPanelTest.java
package com.planificador.ui;

import com.planificador.model.*;
import com.planificador.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PomodoroPanelTest {

    static final LocalDate HOY = LocalDate.of(2026, 10, 25);

    @TempDir
    Path tmp;

    @Test
    void guardaElCicloAlTerminar() throws Exception {
        // TODO T07
    }

    @Test
    void sinTemaGuardaNull() throws Exception {
        // TODO T07
    }
}
```

Completá los `// TODO` que pide la tarea T07. Devolvé **solo los métodos que completaste**, cada uno entero (firma y cuerpo), sin el resto de la clase: un bloque por archivo, con la ruta en la primera línea y después los métodos (y los imports nuevos, si agregaste). Archivos: src/main/java/com/planificador/ui/PomodoroPanel.java, src/test/java/com/planificador/ui/PomodoroPanelTest.java.
