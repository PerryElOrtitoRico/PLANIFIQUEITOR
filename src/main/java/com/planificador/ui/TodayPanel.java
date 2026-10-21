package com.planificador.ui;

import com.planificador.model.StudySession;
import com.planificador.model.Subject;
import com.planificador.service.DataManager;

import javax.swing.*;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class TodayPanel extends JPanel {
    private MainWindow mainFrame;
    private JPanel sessionsListPanel;
    private JLabel headerLabel;

    public TodayPanel() {
        this(null);
    }

    public TodayPanel(MainWindow mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        initUI();
    }

    private void initUI() {
        JPanel topHeader = new JPanel(new BorderLayout());
        headerLabel = new JLabel("🎯 Plan de Estudio de Hoy");
        headerLabel.setFont(new Font("SansSerif", Font.BOLD, 22));

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

        JButton addTodayBtn = new JButton("＋ Agregar Estudio Hoy");
        addTodayBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        addTodayBtn.setBackground(new Color(16, 185, 129));
        addTodayBtn.setForeground(Color.WHITE);
        addTodayBtn.setFocusPainted(false);
        addTodayBtn.addActionListener(e -> showAddTodaySessionDialog());

        JButton timerBtn = new JButton("⏱️ Cronómetro");
        timerBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        timerBtn.setBackground(new Color(79, 70, 229));
        timerBtn.setForeground(Color.WHITE);
        timerBtn.setFocusPainted(false);
        timerBtn.addActionListener(e -> {
            TimerDialog dialog = new TimerDialog(mainFrame, 25);
            dialog.setVisible(true);
        });

        rightBtns.add(addTodayBtn);
        rightBtns.add(timerBtn);

        topHeader.add(headerLabel, BorderLayout.WEST);
        topHeader.add(rightBtns, BorderLayout.EAST);
        add(topHeader, BorderLayout.NORTH);

        sessionsListPanel = new JPanel();
        sessionsListPanel.setLayout(new BoxLayout(sessionsListPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(sessionsListPanel);
        scrollPane.setBorder(null);

        add(scrollPane, BorderLayout.CENTER);

        refresh();
    }

    private String getTodayDayKey() {
        DayOfWeek dow = LocalDate.now().getDayOfWeek();
        return switch (dow) {
            case MONDAY -> "lunes";
            case TUESDAY -> "martes";
            case WEDNESDAY -> "miercoles";
            case THURSDAY -> "jueves";
            case FRIDAY -> "viernes";
            case SATURDAY -> "sabado";
            case SUNDAY -> "domingo";
        };
    }

    public void refresh() {
        sessionsListPanel.removeAll();

        String todayName = getTodayDayKey();

        List<StudySession> todaySessions = DataManager.getInstance().getStudySessions().stream()
                .filter(s -> s.getDayOfWeek().equalsIgnoreCase(todayName))
                .collect(Collectors.toList());

        if (todaySessions.isEmpty()) {
            JPanel emptyBox = new JPanel();
            emptyBox.add(new JLabel("🎉 ¡No tienes sesiones programadas para hoy (" + todayName + ")! Presiona '＋ Agregar Estudio Hoy' para agregar una."));
            sessionsListPanel.add(emptyBox);
        } else {
            for (StudySession session : todaySessions) {
                Subject sub = DataManager.getInstance().getSubjectById(session.getSubjectId());
                String subName = sub != null ? sub.getName() : "General";

                JPanel card = new JPanel(new BorderLayout(10, 10));
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
                card.setBackground(Color.WHITE);
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(session.isCompleted() ? new Color(16, 185, 129) : new Color(226, 232, 240), 1),
                        BorderFactory.createEmptyBorder(10, 15, 10, 15)
                ));

                JCheckBox chk = new JCheckBox("<html><b>" + subName + "</b> - " + session.getTopicDescription() + " <i>(" + session.getDurationMinutes() + " min)</i></html>");
                chk.setSelected(session.isCompleted());
                chk.setOpaque(false);
                chk.addActionListener(e -> {
                    session.setCompleted(chk.isSelected());
                    DataManager.getInstance().saveData();
                    if (mainFrame != null) mainFrame.refreshAllPanels();
                    else refresh();
                });

                JButton startBtn = new JButton("▶ Iniciar Cronómetro");
                startBtn.addActionListener(e -> {
                    TimerDialog dialog = new TimerDialog(mainFrame, session.getDurationMinutes());
                    dialog.setVisible(true);
                });

                card.add(chk, BorderLayout.CENTER);
                card.add(startBtn, BorderLayout.EAST);
                sessionsListPanel.add(card);
                sessionsListPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        }

        sessionsListPanel.revalidate();
        sessionsListPanel.repaint();
    }

    private void showAddTodaySessionDialog() {
        List<Subject> subjects = DataManager.getInstance().getSubjects();
        String[] subjectItems;
        if (subjects.isEmpty()) {
            subjectItems = new String[]{"General"};
        } else {
            subjectItems = subjects.stream().map(Subject::getName).toArray(String[]::new);
        }

        JComboBox<String> subCombo = new JComboBox<>(subjectItems);
        JTextField topicField = new JTextField();
        JTextField durationField = new JTextField("60");

        Object[] message = {
                "Materia:", subCombo,
                "Tema / Actividad de hoy:", topicField,
                "Duración (minutos):", durationField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Agregar Sesión para Hoy (" + getTodayDayKey() + ")", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            String topic = topicField.getText().trim();
            if (topic.isEmpty()) {
                JOptionPane.showMessageDialog(this, "El tema de estudio no puede estar vacío.", "Atención", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int minutes = 60;
            try {
                minutes = Integer.parseInt(durationField.getText().trim());
            } catch (Exception ex) {
                minutes = 60;
            }

            String selectedSubId = (subjects.isEmpty() || subCombo.getSelectedIndex() >= subjects.size())
                    ? "general"
                    : subjects.get(subCombo.getSelectedIndex()).getId();

            StudySession newSession = new StudySession(
                    "ss_" + System.currentTimeMillis(),
                    getTodayDayKey(),
                    selectedSubId,
                    topic,
                    minutes,
                    false
            );

            DataManager.getInstance().addStudySession(newSession);
            if (mainFrame != null) {
                mainFrame.refreshAllPanels();
            } else {
                refresh();
            }
        }
    }
}