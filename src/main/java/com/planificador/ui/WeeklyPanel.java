package com.planificador.ui;

import com.planificador.model.StudySession;
import com.planificador.model.Subject;
import com.planificador.service.DataManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class WeeklyPanel extends JPanel {
    private MainWindow mainFrame;
    private JPanel daysGrid;

    private static final String[] DAYS_KEYS = {"lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo"};
    private static final String[] DAYS_LABELS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

    public WeeklyPanel() {
        this(null);
    }

    public WeeklyPanel(MainWindow mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        initUI();
    }

    private void initUI() {
        JPanel headerPanel = new JPanel(new BorderLayout(10, 10));
        JLabel title = new JLabel("Guía de Estudio Semanal");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));

        JButton addSessionBtn = new JButton("Agregar Estudio");
        addSessionBtn.setFocusPainted(false);
        addSessionBtn.addActionListener(e -> showAddSessionDialog(null));

        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(addSessionBtn, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        daysGrid = new JPanel(new GridLayout(1, 7, 10, 10));
        JScrollPane scrollPane = new JScrollPane(daysGrid);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        refresh();
    }

    public void refresh() {
        daysGrid.removeAll();

        List<StudySession> allSessions = DataManager.getInstance().getStudySessions();

        for (int i = 0; i < DAYS_KEYS.length; i++) {
            String dayKey = DAYS_KEYS[i];
            String dayLabel = DAYS_LABELS[i];

            JPanel dayColumn = new JPanel();
            dayColumn.setLayout(new BorderLayout(5, 5));
            dayColumn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1),
                    BorderFactory.createEmptyBorder(10, 8, 10, 8)
            ));

            // Header for Day
            List<StudySession> daySessions = allSessions.stream()
                    .filter(s -> s.getDayOfWeek().equalsIgnoreCase(dayKey))
                    .collect(Collectors.toList());

            int totalMinutes = daySessions.stream().mapToInt(StudySession::getDurationMinutes).sum();

            JPanel dayHeader = new JPanel(new BorderLayout());
            dayHeader.setOpaque(false);

            JLabel label = new JLabel("<html><b>" + dayLabel + "</b><br><font color='#64748b' size='2'>" + totalMinutes + " min planificados</font></html>");

            JButton addBtn = new JButton("");
            addBtn.setToolTipText("Agregar sesión para " + dayLabel);
            addBtn.setMargin(new Insets(2, 6, 2, 6));
            addBtn.addActionListener(e -> showAddSessionDialog(dayKey));

            dayHeader.add(label, BorderLayout.WEST);
            dayHeader.add(addBtn, BorderLayout.EAST);

            dayColumn.add(dayHeader, BorderLayout.NORTH);

            // Container for sessions in this day
            JPanel sessionsList = new JPanel();
            sessionsList.setLayout(new BoxLayout(sessionsList, BoxLayout.Y_AXIS));
            sessionsList.setOpaque(false);

            if (daySessions.isEmpty()) {
                JLabel emptyLbl = new JLabel("<html><font color='#94a3b8'><i>Sin sesiones</i></font></html>");
                emptyLbl.setBorder(BorderFactory.createEmptyBorder(15, 5, 5, 5));
                sessionsList.add(emptyLbl);
            } else {
                for (StudySession s : daySessions) {
                    Subject sub = DataManager.getInstance().getSubjectById(s.getSubjectId());
                    String subName = sub != null ? sub.getName() : "General";

                    JPanel card = new JPanel(new BorderLayout(5, 5));
                    card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
                    card.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(s.isCompleted() ? new Color(0x43A047) : UIManager.getColor("Component.borderColor"), 1),
                            BorderFactory.createEmptyBorder(6, 6, 6, 6)
                    ));

                    JCheckBox chk = new JCheckBox("<html><b>" + subName + "</b><br>" + s.getTopicDescription() + " <font color='gray'>(" + s.getDurationMinutes() + "m)</font></html>");
                    chk.setSelected(s.isCompleted());
                    chk.setOpaque(false);
                    chk.addActionListener(e -> {
                        DataManager.getInstance().setSessionCompleted(s, chk.isSelected());
                        if (mainFrame != null) mainFrame.refreshAllPanels();
                        else refresh();
                    });

                    // Action buttons
                    JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
                    actionsPanel.setOpaque(false);

                    JButton timerBtn = new JButton("▶");
                    timerBtn.setToolTipText("Iniciar temporizador");
                    timerBtn.setMargin(new Insets(1, 4, 1, 4));
                    timerBtn.addActionListener(e -> {
                        TimerDialog dialog = new TimerDialog(mainFrame, s.getDurationMinutes());
                        dialog.setVisible(true);
                    });

                    JButton editBtn = new JButton("");
                    editBtn.setToolTipText("Editar sesión");
                    editBtn.setMargin(new Insets(1, 4, 1, 4));
                    editBtn.addActionListener(e -> showEditSessionDialog(s));

                    JButton delBtn = new JButton("");
                    delBtn.setToolTipText("Eliminar sesión");
                    delBtn.setMargin(new Insets(1, 4, 1, 4));
                    delBtn.addActionListener(e -> {
                        int confirm = JOptionPane.showConfirmDialog(this, "¿Eliminar la sesión: " + s.getTopicDescription() + "?", "Eliminar Sesión", JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION) {
                            DataManager.getInstance().removeStudySession(s.getId());
                            if (mainFrame != null) mainFrame.refreshAllPanels();
                            else refresh();
                        }
                    });

                    actionsPanel.add(timerBtn);
                    actionsPanel.add(editBtn);
                    actionsPanel.add(delBtn);

                    card.add(chk, BorderLayout.CENTER);
                    card.add(actionsPanel, BorderLayout.SOUTH);

                    sessionsList.add(card);
                    sessionsList.add(Box.createRigidArea(new Dimension(0, 8)));
                }
            }

            JScrollPane dayScroll = new JScrollPane(sessionsList);
            dayScroll.setBorder(null);
            dayScroll.setOpaque(false);
            dayScroll.getViewport().setOpaque(false);

            dayColumn.add(dayScroll, BorderLayout.CENTER);

            daysGrid.add(dayColumn);
        }

        daysGrid.revalidate();
        daysGrid.repaint();
    }

    private void showAddSessionDialog(String defaultDayKey) {
        JComboBox<String> dayCombo = new JComboBox<>(DAYS_LABELS);
        if (defaultDayKey != null) {
            for (int i = 0; i < DAYS_KEYS.length; i++) {
                if (DAYS_KEYS[i].equalsIgnoreCase(defaultDayKey)) {
                    dayCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

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
                "Día de la semana:", dayCombo,
                "Materia:", subCombo,
                "Tema / Actividad de estudio:", topicField,
                "Duración en minutos:", durationField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Agregar Estudio a la Guía", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            String topic = topicField.getText().trim();
            if (topic.isEmpty()) {
                JOptionPane.showMessageDialog(this, "El tema de estudio no puede estar vacío.", "Campo requerido", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int minutes = 60;
            try {
                minutes = Integer.parseInt(durationField.getText().trim());
            } catch (Exception ex) {
                minutes = 60;
            }

            String selectedDayKey = DAYS_KEYS[dayCombo.getSelectedIndex()];
            String selectedSubId = (subjects.isEmpty() || subCombo.getSelectedIndex() >= subjects.size())
                    ? "general"
                    : subjects.get(subCombo.getSelectedIndex()).getId();

            StudySession newSession = new StudySession(
                    "ss_" + System.currentTimeMillis(),
                    selectedDayKey,
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

    private void showEditSessionDialog(StudySession session) {
        JComboBox<String> dayCombo = new JComboBox<>(DAYS_LABELS);
        for (int i = 0; i < DAYS_KEYS.length; i++) {
            if (DAYS_KEYS[i].equalsIgnoreCase(session.getDayOfWeek())) {
                dayCombo.setSelectedIndex(i);
                break;
            }
        }

        List<Subject> subjects = DataManager.getInstance().getSubjects();
        String[] subjectItems;
        int selectedSubIndex = 0;
        if (subjects.isEmpty()) {
            subjectItems = new String[]{"General"};
        } else {
            subjectItems = new String[subjects.size()];
            for (int i = 0; i < subjects.size(); i++) {
                subjectItems[i] = subjects.get(i).getName();
                if (subjects.get(i).getId().equals(session.getSubjectId())) {
                    selectedSubIndex = i;
                }
            }
        }

        JComboBox<String> subCombo = new JComboBox<>(subjectItems);
        subCombo.setSelectedIndex(selectedSubIndex);

        JTextField topicField = new JTextField(session.getTopicDescription());
        JTextField durationField = new JTextField(String.valueOf(session.getDurationMinutes()));

        Object[] message = {
                "Día de la semana:", dayCombo,
                "Materia:", subCombo,
                "Tema / Actividad de estudio:", topicField,
                "Duración en minutos:", durationField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Editar Sesión de Estudio", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            String topic = topicField.getText().trim();
            if (topic.isEmpty()) {
                JOptionPane.showMessageDialog(this, "El tema de estudio no puede estar vacío.", "Campo requerido", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int minutes = session.getDurationMinutes();
            try {
                minutes = Integer.parseInt(durationField.getText().trim());
            } catch (Exception ex) {
                // keep previous
            }

            String selectedDayKey = DAYS_KEYS[dayCombo.getSelectedIndex()];
            String selectedSubId = (subjects.isEmpty() || subCombo.getSelectedIndex() >= subjects.size())
                    ? "general"
                    : subjects.get(subCombo.getSelectedIndex()).getId();

            session.setDayOfWeek(selectedDayKey);
            session.setSubjectId(selectedSubId);
            session.setTopicDescription(topic);
            session.setDurationMinutes(minutes);

            DataManager.getInstance().saveData();
            if (mainFrame != null) {
                mainFrame.refreshAllPanels();
            } else {
                refresh();
            }
        }
    }
}