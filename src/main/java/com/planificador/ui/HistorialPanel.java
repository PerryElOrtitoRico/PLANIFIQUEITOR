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
        abajo.add(detalleTitulo, BorderLayout.NORTH);
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
        DefaultTableModel m = new DefaultTableModel(new String[]{"Hora", "Materia", "Tema", "Minutos", "Origen"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        int total = 0;
        for (StudyLog l : logs) {
            if (!elegido.toString().equals(l.getDate())) continue;
            Subject s = DataManager.getInstance().getSubjectById(l.getSubjectId());
            m.addRow(new Object[]{l.getStartTime() == null ? "—" : l.getStartTime(),
                    s != null ? s.getName() : "(materia eliminada)",
                    l.getTopicTitle() == null ? "" : l.getTopicTitle(),
                    l.getMinutes(), l.isFromGuide() ? "Guía semanal" : "Pomodoro"});
            total += l.getMinutes();
        }
        detalle.setModel(m);
        detalleTitulo.setText(elegido.getDayOfMonth() + "/" + elegido.getMonthValue() + "/" + elegido.getYear()
                + (total > 0 ? " — total " + formatear(total) : " — no hay estudio registrado"));
    }

    static String formatear(int minutos) {
        return minutos >= 60 ? (minutos / 60) + "h" + (minutos % 60 > 0 ? String.format("%02d", minutos % 60) : "") : minutos + "m";
    }
}
