<!-- modelo qwen/qwen3.5-9b, temperatura 0.2, max_tokens 4740, razonamiento no -->

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

## 4. Tiempo por materia
- Semana = de lunes a domingo. Estudiado = suma de `StudyLog.minutes` con fecha en esa semana. Planeado = suma de `StudySession.durationMinutes` de la guía semanal (la guía es la misma todas las semanas).

---

# T08 — Tiempo por materia: las dos tablas

## Objetivo
Completar **solo** `buildWeekModel` y `buildHistoryModel` de `StudyTimePanel` y los tests `tablaDeLaSemana` y `tablaDeLasUltimasSemanas`.

## Código que ya existe
- `DataManager.getInstance()`: `dm.getSubjects()` (`List<Subject>`), `dm.getStudyLogs()`, `dm.getStudySessions()`. `Subject`: `s.getId()`, `s.getName()`.
- `StudyTimeStats.compareWeek(subjectIds, logs, guide, weekStart)` (`List<SubjectWeekTime>`, en el orden de `subjectIds`), `StudyTimeStats.minutesBySubject(logs, weekStart)` (`Map<String, Integer>`), `StudyTimeStats.formatMinutes(min)` ("45 min", "2 h", "1 h 30 min").
- `SubjectWeekTime` es un record: `t.plannedMinutes()`, `t.actualMinutes()`.
- En la clase: `readOnlyModel(Object[] columnas)` devuelve un `DefaultTableModel` vacío con esas columnas, y `DIA` = formato `dd/MM`.

## Métodos (pasos en orden)
- `buildWeekModel(weekStart)`:
  1. `DataManager dm = DataManager.getInstance();`
  2. `DefaultTableModel m = readOnlyModel(new Object[]{"Materia", "Planeado", "Estudiado", "Avance"});`
  3. `List<String> ids = new ArrayList<>(); for (Subject s : dm.getSubjects()) ids.add(s.getId());`
  4. `List<SubjectWeekTime> times = StudyTimeStats.compareWeek(ids, dm.getStudyLogs(), dm.getStudySessions(), weekStart);`
  5. `for (int i = 0; i < times.size(); i++) {`
     - `SubjectWeekTime t = times.get(i);`
     - `String avance = t.plannedMinutes() > 0 ? (t.actualMinutes() * 100 / t.plannedMinutes()) + " %" : "—";`
     - `m.addRow(new Object[]{dm.getSubjects().get(i).getName(), StudyTimeStats.formatMinutes(t.plannedMinutes()), StudyTimeStats.formatMinutes(t.actualMinutes()), avance});`
     - `}`
  6. `return m;`
- `buildHistoryModel(lastWeekStart, weeks)`:
  1. `DataManager dm = DataManager.getInstance(); List<Subject> subjects = dm.getSubjects();`
  2. `Object[] cols = new Object[subjects.size() + 2]; cols[0] = "Semana"; for (int i = 0; i < subjects.size(); i++) cols[i + 1] = subjects.get(i).getName(); cols[cols.length - 1] = "Total";`
  3. `DefaultTableModel m = readOnlyModel(cols);`
  4. `for (int w = weeks - 1; w >= 0; w--) {`
     - `LocalDate ws = lastWeekStart.minusWeeks(w);`
     - `Map<String, Integer> mins = StudyTimeStats.minutesBySubject(dm.getStudyLogs(), ws);`
     - `Object[] row = new Object[cols.length]; row[0] = ws.format(DIA); int total = 0;`
     - `for (int i = 0; i < subjects.size(); i++) { int x = mins.getOrDefault(subjects.get(i).getId(), 0); total += x; row[i + 1] = StudyTimeStats.formatMinutes(x); }`
     - `row[cols.length - 1] = StudyTimeStats.formatMinutes(total);`
     - `m.addRow(row);`
     - `}`
  5. `return m;`

## Tests (cada uno empieza con `UiDatos.cargar(tmp);` y `StudyTimePanel p = new StudyTimePanel(null);`. En los datos: "Mecánica de Fluidos" tiene 120 min planeados y 50 estudiados la semana del 2026-10-19; "Física II" 90 planeados y 50 estudiados la semana del 2026-10-12)
- `tablaDeLaSemana`: `var m = p.buildWeekModel(LocalDate.of(2026, 10, 19));` → `assertEquals(2, m.getRowCount());`, `assertEquals("Mecánica de Fluidos", m.getValueAt(0, 0));`, `assertEquals("2 h", m.getValueAt(0, 1));`, `assertEquals("50 min", m.getValueAt(0, 2));`, `assertEquals("41 %", m.getValueAt(0, 3));`
- `tablaDeLasUltimasSemanas`: `var m = p.buildHistoryModel(LocalDate.of(2026, 10, 19), 2);` → `assertEquals(4, m.getColumnCount());`, `assertEquals("Total", m.getColumnName(3));`, `assertEquals("12/10", m.getValueAt(0, 0));`, `assertEquals("50 min", m.getValueAt(0, 2));`, `assertEquals("50 min", m.getValueAt(1, 3));`

---

# Archivos actuales

```java
// src/main/java/com/planificador/ui/StudyTimePanel.java
package com.planificador.ui;

import com.planificador.model.StudyLog;
import com.planificador.model.StudySession;
import com.planificador.model.Subject;
import com.planificador.service.DataManager;
import com.planificador.service.StudyTimeStats;
import com.planificador.service.SubjectWeekTime;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Tiempo por materia: lo estudiado con el Pomodoro contra lo planeado en la guía semanal, y las últimas semanas. */
public class StudyTimePanel extends JPanel {

    static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");
    static final int SEMANAS = 8;

    private final MainWindow mainFrame;
    LocalDate weekStart = StudyTimeStats.weekStart(LocalDate.now());
    final JLabel weekLabel = new JLabel();
    final JTable weekTable = new JTable();
    final JTable historyTable = new JTable();

    public StudyTimePanel(MainWindow mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("⏳ Tiempo por Materia");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        header.add(title, BorderLayout.WEST);
        JButton pomodoroBtn = new JButton("🍅 Pomodoro");
        pomodoroBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        pomodoroBtn.setBackground(new Color(225, 29, 72));
        pomodoroBtn.setForeground(Color.WHITE);
        pomodoroBtn.setFocusPainted(false);
        pomodoroBtn.addActionListener(e -> new PomodoroDialog(mainFrame).setVisible(true));
        header.add(pomodoroBtn, BorderLayout.EAST);

        JPanel weekNav = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton prev = new JButton("◀");
        JButton next = new JButton("▶");
        JButton today = new JButton("Esta semana");
        prev.addActionListener(e -> { weekStart = weekStart.minusWeeks(1); refresh(); });
        next.addActionListener(e -> { weekStart = weekStart.plusWeeks(1); refresh(); });
        today.addActionListener(e -> { weekStart = StudyTimeStats.weekStart(LocalDate.now()); refresh(); });
        weekLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        weekNav.add(prev);
        weekNav.add(weekLabel);
        weekNav.add(next);
        weekNav.add(today);

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.add(header, BorderLayout.NORTH);
        top.add(weekNav, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JPanel tables = new JPanel(new GridLayout(2, 1, 0, 15));
        tables.add(titled("Semana elegida: planeado (guía semanal) y estudiado (Pomodoro)", weekTable));
        tables.add(titled("Últimas " + SEMANAS + " semanas (horas estudiadas)", historyTable));
        add(tables, BorderLayout.CENTER);

        refresh();
    }

    private static JComponent titled(String titulo, JTable tabla) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        JLabel l = new JLabel(titulo);
        l.setFont(new Font("SansSerif", Font.BOLD, 13));
        p.add(l, BorderLayout.NORTH);
        tabla.setRowHeight(24);
        tabla.setFillsViewportHeight(true);
        p.add(new JScrollPane(tabla), BorderLayout.CENTER);
        return p;
    }

    public void refresh() {
        weekLabel.setText("Semana del " + weekStart.format(DIA) + " al " + weekStart.plusDays(6).format(DIA));
        weekTable.setModel(buildWeekModel(weekStart));
        historyTable.setModel(buildHistoryModel(weekStart, SEMANAS));
    }

    /** Una fila por materia: Materia, Planeado, Estudiado, Avance. */
    DefaultTableModel buildWeekModel(LocalDate weekStart) {
        // TODO T08
        throw new UnsupportedOperationException();
    }

    /** Una fila por semana (de la más vieja a lastWeekStart): Semana, una columna por materia y Total. */
    DefaultTableModel buildHistoryModel(LocalDate lastWeekStart, int weeks) {
        // TODO T08
        throw new UnsupportedOperationException();
    }

    /** Modelo de tabla que no se puede editar a mano. */
    static DefaultTableModel readOnlyModel(Object[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }
}
```

```java
// src/test/java/com/planificador/ui/StudyTimePanelTest.java
package com.planificador.ui;

import com.planificador.model.*;
import com.planificador.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StudyTimePanelTest {

    static final LocalDate HOY = LocalDate.of(2026, 10, 25);

    @TempDir
    Path tmp;

    @Test
    void tablaDeLaSemana() throws Exception {
        // TODO T08
    }

    @Test
    void tablaDeLasUltimasSemanas() throws Exception {
        // TODO T08
    }
}
```

Completá los `// TODO` que pide la tarea T08. Devolvé **solo los métodos que completaste**, cada uno entero (firma y cuerpo), sin el resto de la clase: un bloque por archivo, con la ruta en la primera línea y después los métodos (y los imports nuevos, si agregaste). Archivos: src/main/java/com/planificador/ui/StudyTimePanel.java, src/test/java/com/planificador/ui/StudyTimePanelTest.java.
