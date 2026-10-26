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
