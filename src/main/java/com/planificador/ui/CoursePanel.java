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
        if (s.getCourse() == null) return List.of("Todavía no cargaste las reglas de cursada: tocá «Reglas de cursada».");
        CourseStatus st = CourseStatusCalculator.evaluate(s.getCourse());
        return CourseStatusCalculator.describe(st, s.getCourse().getRules());
    }

    /** Columnas: Tipo, N.º, Recuperatorio, Nota, Fecha. Una fila por nota, en el orden de la lista. */
    static DefaultTableModel evaluationsModel(CourseTracking t) {
        DefaultTableModel m = StudyTimePanel.readOnlyModel(new Object[]{"Tipo", "N.º", "Recuperatorio", "Nota", "Fecha"});
        for (Evaluation e : t.getEvaluations()) {
            boolean parcial = Evaluation.PARCIAL.equals(e.getKind());
            m.addRow(new Object[]{parcial ? "Parcial" : "TP", e.getNumber(), parcial ? (e.isRetake() ? "Sí" : "No") : "—", CourseStatusCalculator.formatGrade(e.getGrade()), e.getDate() == null ? "" : e.getDate()});
        }
        return m;
    }

    /** Columnas: Fecha, Asistencia ("Presente" o "Ausente"). Una fila por clase, en el orden de la lista. */
    static DefaultTableModel attendanceModel(CourseTracking t) {
        DefaultTableModel m = StudyTimePanel.readOnlyModel(new Object[]{"Fecha", "Asistencia"});
        for (AttendanceRecord r : t.getAttendance()) m.addRow(new Object[]{r.getDate(), r.isPresent() ? "Presente" : "Ausente"});
        return m;
    }

    /**
     * La asistencia de una clase.
     * @throws IllegalArgumentException si dateText no es AAAA-MM-DD
     */
    static AttendanceRecord attendanceFrom(String dateText, boolean present) {
        try { return new AttendanceRecord(LocalDate.parse(dateText.trim()).toString(), present); } catch (DateTimeParseException e) { throw new IllegalArgumentException("Fecha inválida: usá AAAA-MM-DD"); }
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
