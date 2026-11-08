<!-- modelo google/gemma-4-e4b-qat, temperatura 0.2, max_tokens 5174, razonamiento no -->

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

# T12c — Historial: mostrar la nota

## Objetivo
Completar el `// TODO T12c` de `HistorialPanel.mostrarDetalle(List<StudyLog>)`, que ya existe. Devolvé el método entero; el resto queda igual. Borrá la línea `// TODO T12c…`.

## Cambios
- `HistorialPanel.mostrarDetalle(List<StudyLog> logs)`:
  1. En el lugar del `// TODO T12c`: `filas.clear();`
  2. Los nombres de columna pasan a `new String[]{"Hora", "Materia", "Tema", "Minutos", "Origen", "Nota"}`.
  3. Dentro del `for`, justo antes de `m.addRow(...)`: `filas.add(l);`
  4. En el `m.addRow(new Object[]{...})`, después de `origen(l)` se agrega un elemento más: `l.getNote() == null ? "" : l.getNote()`.

---

# Archivos actuales

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
        // TODO T12c: columna Nota y lista filas
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

Completá los `// TODO` que pide la tarea T12c. Devolvé **solo los métodos que completaste**, cada uno entero (firma y cuerpo), sin el resto de la clase: un bloque por archivo, con la ruta en la primera línea y después los métodos (y los imports nuevos, si agregaste). Archivos: src/main/java/com/planificador/ui/HistorialPanel.java.
