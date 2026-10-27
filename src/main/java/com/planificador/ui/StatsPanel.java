package com.planificador.ui;

import com.planificador.model.StudySession;
import com.planificador.model.Subject;
import com.planificador.model.Topic;
import com.planificador.model.Unit;
import com.planificador.service.DataManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class StatsPanel extends JPanel {
    private MainWindow mainFrame;
    private JPanel statsContent;

    public StatsPanel() {
        this(null);
    }

    public StatsPanel(MainWindow mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        initUI();
    }

    private void initUI() {
        JLabel title = new JLabel("Progreso General y Estadísticas de Estudio");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        add(title, BorderLayout.NORTH);

        statsContent = new JPanel();
        statsContent.setLayout(new BoxLayout(statsContent, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(statsContent);
        scrollPane.setBorder(null);

        add(scrollPane, BorderLayout.CENTER);

        refresh();
    }

    public void refresh() {
        statsContent.removeAll();

        List<Subject> subjects = DataManager.getInstance().getSubjects();
        List<StudySession> sessions = DataManager.getInstance().getStudySessions();

        // Metric summary grid
        JPanel metricsGrid = new JPanel(new GridLayout(1, 4, 10, 10));
        metricsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        int totalSessions = sessions.size();
        int completedSessions = (int) sessions.stream().filter(StudySession::isCompleted).count();
        int totalMinutesPlanned = sessions.stream().mapToInt(StudySession::getDurationMinutes).sum();
        int totalMinutesCompleted = sessions.stream().filter(StudySession::isCompleted).mapToInt(StudySession::getDurationMinutes).sum();

        int totalTopicsCount = 0;
        int completedTopicsCount = 0;
        for (Subject s : subjects) {
            for (Unit u : s.getUnits()) {
                for (Topic t : u.getTopics()) {
                    totalTopicsCount++;
                    if (t.isCompleted()) completedTopicsCount++;
                }
            }
        }
        int overallSyllabusPercent = totalTopicsCount > 0 ? (completedTopicsCount * 100 / totalTopicsCount) : 0;

        metricsGrid.add(createMetricCard("Horas Planificadas", String.format("%.1f hrs", totalMinutesPlanned / 60.0)));
        metricsGrid.add(createMetricCard("Horas Completadas", String.format("%.1f hrs", totalMinutesCompleted / 60.0)));
        metricsGrid.add(createMetricCard("Temas Dominados", completedTopicsCount + " / " + totalTopicsCount + " (" + overallSyllabusPercent + "%)"));
        metricsGrid.add(createMetricCard("Materias Activas", String.valueOf(subjects.size())));

        statsContent.add(metricsGrid);
        statsContent.add(Box.createRigidArea(new Dimension(0, 20)));

        JLabel sectionTitle = new JLabel("Avance Detallado por Materia Activa");
        sectionTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        statsContent.add(sectionTitle);
        statsContent.add(Box.createRigidArea(new Dimension(0, 10)));

        if (subjects.isEmpty()) {
            JPanel emptyCard = new JPanel();
            emptyCard.add(new JLabel("<html><font color='gray'>No hay materias registradas actualmente. Al agregar o borrar materias, esta lista se actualiza automáticamente.</font></html>"));
            statsContent.add(emptyCard);
        } else {
            for (Subject sub : subjects) {
                JPanel card = new JPanel(new BorderLayout(10, 10));
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1),
                        BorderFactory.createEmptyBorder(12, 15, 12, 15)
                ));

                int progress = (int) sub.getProgressPercentage();

                int subTopicsCount = 0;
                int subCompletedTopicsCount = 0;
                for (Unit u : sub.getUnits()) {
                    for (Topic t : u.getTopics()) {
                        subTopicsCount++;
                        if (t.isCompleted()) subCompletedTopicsCount++;
                    }
                }

                int subPlannedMins = sessions.stream()
                        .filter(s -> s.getSubjectId().equals(sub.getId()))
                        .mapToInt(StudySession::getDurationMinutes)
                        .sum();

                JLabel subHeader = new JLabel("<html><b><font size='3'>" + sub.getName() + "</font></b> &nbsp;<font color='gray'>Profesor: " + (sub.getProfessor().isEmpty() ? "Sin asignar" : sub.getProfessor()) + " | " + subPlannedMins + " min/semana</font></html>");

                JProgressBar bar = new JProgressBar(0, 100);
                bar.setValue(progress);
                bar.setStringPainted(true);

                JLabel subInfo = new JLabel("Temas: " + subCompletedTopicsCount + "/" + subTopicsCount + " (" + progress + "%)");

                JPanel centerContainer = new JPanel(new BorderLayout(5, 5));
                centerContainer.setOpaque(false);
                centerContainer.add(bar, BorderLayout.CENTER);
                centerContainer.add(subInfo, BorderLayout.EAST);

                card.add(subHeader, BorderLayout.NORTH);
                card.add(centerContainer, BorderLayout.CENTER);

                statsContent.add(card);
                statsContent.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        }

        statsContent.revalidate();
        statsContent.repaint();
    }

    private JPanel createMetricCard(String title, String value) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JLabel tLbl = new JLabel(title);
        tLbl.setForeground(UIManager.getColor("Label.disabledForeground"));

        JLabel vLbl = new JLabel(value);
        vLbl.setFont(new Font("SansSerif", Font.BOLD, 18));
        

        p.add(tLbl);
        p.add(Box.createRigidArea(new Dimension(0, 4)));
        p.add(vLbl);

        return p;
    }
}