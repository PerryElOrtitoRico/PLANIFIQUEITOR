<!-- modelo google/gemma-4-e4b-qat, temperatura 0.2, max_tokens 2840, razonamiento no -->

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

## 5. Cursada (por materia)
- **Asistencia**: `porcentaje = presentes * 100 / registradas` (100 si no hay ninguna). Si se sabe la cantidad de clases (`plannedClasses > 0`): `faltasPermitidas = floor(plannedClasses * (100 - minAttendancePercent) / 100)`, `quedan = faltasPermitidas - ausentes`, y queda libre por faltas si `quedan < 0`. Si no se sabe: libre por faltas si hay clases registradas y `porcentaje < minAttendancePercent`.
- **Parciales** 1…`examCount`. Nota efectiva = la del recuperatorio si hay; si no, la del parcial. Estado: sin nota → `PENDIENTE`; efectiva ≥ `passGrade` → `APROBADO`; menor, sin recuperatorio rendido y con recuperatorio permitido → `A_RECUPERAR`; si no → `DESAPROBADO`. Si hay dos notas del mismo parcial (o del mismo recuperatorio), vale la última cargada.
- Los **TP** se cargan y se muestran, pero no cambian la condición.
- **Condición**, en este orden: `LIBRE` si quedó libre por faltas o hay un parcial `DESAPROBADO`; `EN_CURSO` si hay un parcial `PENDIENTE` o `A_RECUPERAR`, o si `plannedClasses > 0` y todavía hay menos clases registradas; `PROMOCION` si `promotionGrade > 0` y todas las notas efectivas ≥ `promotionGrade`; si no, `REGULAR`.
- Todavía puede promocionar si no está libre, `promotionGrade > 0` y cada parcial está sin nota, tiene efectiva ≥ `promotionGrade` o está `A_RECUPERAR`. Todavía puede regularizar si no está libre.

---

# T09b — Cursada: textos y tablas del panel

## Objetivo
Completar **solo** `statusLines`, `evaluationsModel`, `attendanceModel` y `attendanceFrom` de `CoursePanel` y los tests `textosYTablas` y `asistenciaConFechaMala`. El resto del panel ya está hecho.

## Código que ya existe
- `Subject`: `s.getCourse()` (`CourseTracking` o null). `CourseTracking`: `t.getRules()`, `t.getEvaluations()` (`List<Evaluation>`), `t.getAttendance()` (`List<AttendanceRecord>`); `new CourseTracking(rules, evaluations, attendance)`.
- `Evaluation`: `e.getKind()` (`Evaluation.PARCIAL` o `Evaluation.TP`), `e.getNumber()`, `e.isRetake()`, `e.getGrade()`, `e.getDate()` (puede ser null). `AttendanceRecord`: `r.getDate()`, `r.isPresent()`; `new AttendanceRecord(date, present)`.
- `CourseStatusCalculator.evaluate(tracking)` (`CourseStatus`), `CourseStatusCalculator.describe(status, rules)` (`List<String>`), `CourseStatusCalculator.formatGrade(nota)` ("7", "6,5").
- `StudyTimePanel.readOnlyModel(Object[] columnas)` devuelve un `DefaultTableModel` vacío que no se edita.

## Métodos (pasos en orden)
- `statusLines(s)`:
  1. `if (s.getCourse() == null) return List.of("Todavía no cargaste las reglas de cursada: tocá «Reglas de cursada».");`
  2. `CourseStatus st = CourseStatusCalculator.evaluate(s.getCourse());`
  3. `return CourseStatusCalculator.describe(st, s.getCourse().getRules());`
- `evaluationsModel(t)`:
  1. `DefaultTableModel m = StudyTimePanel.readOnlyModel(new Object[]{"Tipo", "N.º", "Recuperatorio", "Nota", "Fecha"});`
  2. `for (Evaluation e : t.getEvaluations()) {`
     - `boolean parcial = Evaluation.PARCIAL.equals(e.getKind());`
     - `m.addRow(new Object[]{parcial ? "Parcial" : "TP", e.getNumber(), parcial ? (e.isRetake() ? "Sí" : "No") : "—", CourseStatusCalculator.formatGrade(e.getGrade()), e.getDate() == null ? "" : e.getDate()});`
     - `}`
  3. `return m;`
- `attendanceModel(t)`:
  1. `DefaultTableModel m = StudyTimePanel.readOnlyModel(new Object[]{"Fecha", "Asistencia"});`
  2. `for (AttendanceRecord r : t.getAttendance()) m.addRow(new Object[]{r.getDate(), r.isPresent() ? "Presente" : "Ausente"});`
  3. `return m;`
- `attendanceFrom(dateText, present)`:
  1. `try { return new AttendanceRecord(LocalDate.parse(dateText.trim()).toString(), present); } catch (DateTimeParseException e) { throw new IllegalArgumentException("Fecha inválida: usá AAAA-MM-DD"); }`

## Tests (no necesitan `UiDatos`: arman los objetos a mano)
- `textosYTablas`:
  1. `Subject s = new Subject("s1", "Física II", "#000000", "", "");`
  2. `assertEquals(List.of("Todavía no cargaste las reglas de cursada: tocá «Reglas de cursada»."), CoursePanel.statusLines(s));`
  3. `CourseTracking t = s.getOrCreateCourse(); t.getEvaluations().add(new Evaluation("e1", Evaluation.PARCIAL, 1, false, 7, "2026-09-01")); t.getEvaluations().add(new Evaluation("e2", Evaluation.TP, 1, false, 8.5, null)); t.getAttendance().add(new AttendanceRecord("2026-08-10", false));`
  4. `assertEquals("Condición: En curso", CoursePanel.statusLines(s).get(0));`
  5. `var m = CoursePanel.evaluationsModel(t);` → `assertEquals("Parcial", m.getValueAt(0, 0));`, `assertEquals("No", m.getValueAt(0, 2));`, `assertEquals("8,5", m.getValueAt(1, 3));`, `assertEquals("", m.getValueAt(1, 4));`
  6. `assertEquals("Ausente", CoursePanel.attendanceModel(t).getValueAt(0, 1));`
- `asistenciaConFechaMala`: `assertEquals("2026-10-25", CoursePanel.attendanceFrom(" 2026-10-25 ", true).getDate());` y `assertThrows(IllegalArgumentException.class, () -> CoursePanel.attendanceFrom("25/10", true));`

---

# Archivos actuales

```java
// src/main/java/com/planificador/ui/CoursePanel.java
package com.planificador.ui;

import com.planificador.model.AttendanceRecord;
import com.planificador.model.CourseTracking;
import com.planificador.model.Evaluation;
import com.planificador.model.Subject;
import com.planificador.service.CourseCondition;
import com.planificador.service.CourseStatus;
import com.planificador.service.CourseStatusCalculator;
import com.planificador.service.DataManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Seguimiento de cursada: reglas, notas, asistencias y condición de cada materia. */
public class CoursePanel extends JPanel {

    private final MainWindow mainFrame;
    private List<Subject> subjects = new ArrayList<>();
    final DefaultListModel<String> subjectNames = new DefaultListModel<>();
    final JList<String> subjectList = new JList<>(subjectNames);
    final JLabel conditionLabel = new JLabel(" ");
    final JPanel statusPanel = new JPanel();
    final JTable evaluationsTable = new JTable();
    final JTable attendanceTable = new JTable();
    final JTextField attendanceDate = new JTextField(LocalDate.now().toString(), 10);

    public CoursePanel(MainWindow mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("🎓 Seguimiento de Cursada");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        add(title, BorderLayout.NORTH);

        subjectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        subjectList.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) showSubject(); });
        JScrollPane listScroll = new JScrollPane(subjectList);
        listScroll.setPreferredSize(new Dimension(220, 0));
        add(listScroll, BorderLayout.WEST);

        conditionLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
        JPanel status = new JPanel(new BorderLayout(0, 6));
        status.add(conditionLabel, BorderLayout.NORTH);
        status.add(statusPanel, BorderLayout.CENTER);

        JButton rulesBtn = new JButton("⚙ Reglas de cursada");
        rulesBtn.addActionListener(e -> editRules());
        JButton addGradeBtn = new JButton("＋ Nota");
        addGradeBtn.addActionListener(e -> addEvaluation());
        JButton delGradeBtn = new JButton("🗑 Borrar nota");
        delGradeBtn.addActionListener(e -> deleteEvaluation());
        JButton presentBtn = new JButton("✔ Presente");
        presentBtn.addActionListener(e -> addAttendance(true));
        JButton absentBtn = new JButton("✘ Ausente");
        absentBtn.addActionListener(e -> addAttendance(false));
        JButton delAttBtn = new JButton("🗑 Borrar clase");
        delAttBtn.addActionListener(e -> deleteAttendance());

        JPanel gradesBox = new JPanel(new BorderLayout(0, 4));
        JPanel gradesBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        gradesBtns.add(new JLabel("Notas de parciales y TP"));
        gradesBtns.add(addGradeBtn);
        gradesBtns.add(delGradeBtn);
        gradesBox.add(gradesBtns, BorderLayout.NORTH);
        gradesBox.add(new JScrollPane(evaluationsTable), BorderLayout.CENTER);

        JPanel attBox = new JPanel(new BorderLayout(0, 4));
        JPanel attBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        attBtns.add(new JLabel("Asistencia — fecha:"));
        attBtns.add(attendanceDate);
        attBtns.add(presentBtn);
        attBtns.add(absentBtn);
        attBtns.add(delAttBtn);
        attBox.add(attBtns, BorderLayout.NORTH);
        attBox.add(new JScrollPane(attendanceTable), BorderLayout.CENTER);

        JPanel tables = new JPanel(new GridLayout(2, 1, 0, 10));
        tables.add(gradesBox);
        tables.add(attBox);

        JPanel right = new JPanel(new BorderLayout(0, 12));
        JPanel statusWithBtn = new JPanel(new BorderLayout());
        statusWithBtn.add(status, BorderLayout.CENTER);
        statusWithBtn.add(rulesBtn, BorderLayout.EAST);
        right.add(statusWithBtn, BorderLayout.NORTH);
        right.add(tables, BorderLayout.CENTER);
        add(right, BorderLayout.CENTER);

        refresh();
    }

    public void refresh() {
        int sel = Math.max(0, subjectList.getSelectedIndex());
        subjects = new ArrayList<>(DataManager.getInstance().getSubjects());
        subjectNames.clear();
        for (Subject s : subjects) subjectNames.addElement(s.getName());
        if (!subjects.isEmpty()) subjectList.setSelectedIndex(Math.min(sel, subjects.size() - 1));
        showSubject();
    }

    Subject selectedSubject() {
        int i = subjectList.getSelectedIndex();
        return (i >= 0 && i < subjects.size()) ? subjects.get(i) : null;
    }

    /** Muestra condición, textos y tablas de la materia elegida. */
    void showSubject() {
        Subject s = selectedSubject();
        statusPanel.removeAll();
        if (s == null) {
            conditionLabel.setText("Agregá una materia en «Materias» para seguir su cursada.");
            evaluationsTable.setModel(evaluationsModel(new CourseTracking()));
            attendanceTable.setModel(attendanceModel(new CourseTracking()));
        } else {
            CourseTracking t = s.getCourse() != null ? s.getCourse() : new CourseTracking();
            conditionLabel.setText(s.getName());
            List<String> lines = statusLines(s);
            for (int i = 0; i < lines.size(); i++) {
                JLabel l = new JLabel(lines.get(i));
                if (i == 0) {
                    l.setFont(new Font("SansSerif", Font.BOLD, 15));
                    if (s.getCourse() != null) l.setForeground(conditionColor(CourseStatusCalculator.evaluate(t).condition()));
                }
                statusPanel.add(l);
            }
            evaluationsTable.setModel(evaluationsModel(t));
            attendanceTable.setModel(attendanceModel(t));
        }
        statusPanel.revalidate();
        statusPanel.repaint();
    }

    /** Textos de la condición de la materia, o un aviso si todavía no tiene reglas cargadas. */
    static List<String> statusLines(Subject s) {
        // TODO T09b
        throw new UnsupportedOperationException();
    }

    /** Columnas: Tipo, N.º, Recuperatorio, Nota, Fecha. Una fila por nota, en el orden de la lista. */
    static DefaultTableModel evaluationsModel(CourseTracking t) {
        // TODO T09b
        throw new UnsupportedOperationException();
    }

    /** Columnas: Fecha, Asistencia ("Presente" o "Ausente"). Una fila por clase, en el orden de la lista. */
    static DefaultTableModel attendanceModel(CourseTracking t) {
        // TODO T09b
        throw new UnsupportedOperationException();
    }

    /**
     * La asistencia de una clase.
     * @throws IllegalArgumentException si dateText no es AAAA-MM-DD
     */
    static AttendanceRecord attendanceFrom(String dateText, boolean present) {
        // TODO T09b
        throw new UnsupportedOperationException();
    }

    static Color conditionColor(CourseCondition c) {
        return switch (c) {
            case PROMOCION -> new Color(16, 185, 129);
            case REGULAR -> new Color(2, 132, 199);
            case LIBRE -> new Color(225, 29, 72);
            case EN_CURSO -> new Color(217, 119, 6);
        };
    }

    private void saveAndRefresh() {
        DataManager.getInstance().saveData();
        if (mainFrame != null) mainFrame.refreshAllPanels();
        else refresh();
    }

    private void editRules() {
        Subject s = selectedSubject();
        if (s == null) return;
        CourseRulesForm form = new CourseRulesForm(s.getOrCreateCourse().getRules());
        int op = JOptionPane.showConfirmDialog(this, form, "Reglas de cursada — " + s.getName(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (op != JOptionPane.OK_OPTION) return;
        s.getCourse().setRules(form.toRules());
        saveAndRefresh();
    }

    private void addEvaluation() {
        Subject s = selectedSubject();
        if (s == null) return;
        EvaluationForm form = new EvaluationForm();
        int op = JOptionPane.showConfirmDialog(this, form, "Nueva nota — " + s.getName(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (op != JOptionPane.OK_OPTION) return;
        try {
            s.getOrCreateCourse().getEvaluations().add(form.toEvaluation("ev_" + System.currentTimeMillis()));
            saveAndRefresh();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Atención", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void deleteEvaluation() {
        Subject s = selectedSubject();
        int row = evaluationsTable.getSelectedRow();
        if (s == null || s.getCourse() == null || row < 0) return;
        s.getCourse().getEvaluations().remove(row);
        saveAndRefresh();
    }

    private void addAttendance(boolean present) {
        Subject s = selectedSubject();
        if (s == null) return;
        try {
            s.getOrCreateCourse().getAttendance().add(attendanceFrom(attendanceDate.getText(), present));
            saveAndRefresh();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Atención", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void deleteAttendance() {
        Subject s = selectedSubject();
        int row = attendanceTable.getSelectedRow();
        if (s == null || s.getCourse() == null || row < 0) return;
        s.getCourse().getAttendance().remove(row);
        saveAndRefresh();
    }
}
```

```java
// src/test/java/com/planificador/ui/CoursePanelTest.java
package com.planificador.ui;

import com.planificador.model.*;
import com.planificador.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CoursePanelTest {

    static final LocalDate HOY = LocalDate.of(2026, 10, 25);

    @TempDir
    Path tmp;

    @Test
    void textosYTablas() throws Exception {
        // TODO T09b
    }

    @Test
    void asistenciaConFechaMala() throws Exception {
        // TODO T09b
    }
}
```

Completá los `// TODO` que pide la tarea T09b. Devolvé **solo los métodos que completaste**, cada uno entero (firma y cuerpo), sin el resto de la clase: un bloque por archivo, con la ruta en la primera línea y después los métodos (y los imports nuevos, si agregaste). Archivos: src/main/java/com/planificador/ui/CoursePanel.java, src/test/java/com/planificador/ui/CoursePanelTest.java.
