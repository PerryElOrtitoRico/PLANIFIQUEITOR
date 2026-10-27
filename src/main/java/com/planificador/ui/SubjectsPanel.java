package com.planificador.ui;

import com.planificador.model.Subject;
import com.planificador.model.Topic;
import com.planificador.model.Unit;
import com.planificador.service.DataManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class SubjectsPanel extends JPanel {
    private MainWindow mainFrame;
    private JPanel subjectsList;

    public SubjectsPanel() {
        this(null);
    }

    public SubjectsPanel(MainWindow mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        initUI();
    }

    private void initUI() {
        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Materias, Unidades y Temas");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));

        JButton addSubjectBtn = new JButton("Nueva Materia");
        addSubjectBtn.setFocusPainted(false);
        addSubjectBtn.addActionListener(e -> showAddSubjectDialog());

        header.add(title, BorderLayout.WEST);
        header.add(addSubjectBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        subjectsList = new JPanel();
        subjectsList.setLayout(new BoxLayout(subjectsList, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(subjectsList);
        scrollPane.setBorder(null);

        add(scrollPane, BorderLayout.CENTER);

        refresh();
    }

    public void refresh() {
        subjectsList.removeAll();
        List<Subject> list = DataManager.getInstance().getSubjects();

        if (list.isEmpty()) {
            JPanel emptyPanel = new JPanel();
            emptyPanel.add(new JLabel("<html><font color='gray'>No hay materias registradas. ¡Haz clic en '<b>Nueva Materia</b>' para agregar la primera!</font></html>"));
            subjectsList.add(emptyPanel);
        } else {
            for (Subject subject : list) {
                JPanel card = new JPanel(new BorderLayout(10, 10));
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1),
                        BorderFactory.createEmptyBorder(12, 15, 12, 15)
                ));

                // Top Info
                JPanel topInfo = new JPanel(new BorderLayout());
                topInfo.setOpaque(false);

                double pct = subject.getProgressPercentage();
                JLabel nameLbl = new JLabel("<html><b><font size='4'>" + subject.getName() + "</font></b> &nbsp;<font color='#4f46e5'>(" + String.format("%.0f", pct) + "% completado)</font></html>");
                JLabel subDetails = new JLabel("<html><font color='gray'>Profesor: " + (subject.getProfessor().isEmpty() ? "Sin asignar" : subject.getProfessor()) + " | Horario: " + (subject.getSchedule().isEmpty() ? "Sin asignar" : subject.getSchedule()) + "</font></html>");

                JPanel textContainer = new JPanel();
                textContainer.setLayout(new BoxLayout(textContainer, BoxLayout.Y_AXIS));
                textContainer.setOpaque(false);
                textContainer.add(nameLbl);
                textContainer.add(subDetails);

                JButton deleteBtn = new JButton("Eliminar Materia");
                deleteBtn.addActionListener(e -> {
                    int confirm = JOptionPane.showConfirmDialog(this,
                            "¿Eliminar la materia '" + subject.getName() + "'?\n\n" +
                            "¡Nota: Se eliminará la materia y también se actualizará y limpiará su avance en la sección de Progreso y Guía Semanal!",
                            "Eliminar Materia", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (confirm == JOptionPane.YES_OPTION) {
                        DataManager.getInstance().removeSubject(subject.getId());
                        if (mainFrame != null) {
                            mainFrame.refreshAllPanels();
                        } else {
                            refresh();
                        }
                    }
                });

                topInfo.add(textContainer, BorderLayout.WEST);
                topInfo.add(deleteBtn, BorderLayout.EAST);

                // Units & Topics list
                JPanel unitsContainer = new JPanel();
                unitsContainer.setLayout(new BoxLayout(unitsContainer, BoxLayout.Y_AXIS));
                unitsContainer.setOpaque(false);
                unitsContainer.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

                if (subject.getUnits().isEmpty()) {
                    JLabel noUnits = new JLabel("<html><font color='#94a3b8'><i>Sin unidades creadas</i></font></html>");
                    unitsContainer.add(noUnits);
                } else {
                    for (Unit u : subject.getUnits()) {
                        JLabel uLabel = new JLabel("Unidad " + u.getNumber() + ": " + u.getTitle());
                        unitsContainer.add(uLabel);

                        for (Topic topic : u.getTopics()) {
                            JCheckBox topicChk = new JCheckBox(topic.getTitle());
                            topicChk.setSelected(topic.isCompleted());
                            topicChk.setOpaque(false);
                            topicChk.addActionListener(ev -> {
                                topic.setCompleted(topicChk.isSelected());
                                DataManager.getInstance().saveData();
                                if (mainFrame != null) {
                                    mainFrame.refreshAllPanels();
                                } else {
                                    refresh();
                                }
                            });
                            unitsContainer.add(topicChk);
                        }
                    }
                }

                card.add(topInfo, BorderLayout.NORTH);
                card.add(unitsContainer, BorderLayout.CENTER);

                subjectsList.add(card);
                subjectsList.add(Box.createRigidArea(new Dimension(0, 12)));
            }
        }

        subjectsList.revalidate();
        subjectsList.repaint();
    }

    private void showAddSubjectDialog() {
        JTextField nameField = new JTextField();
        JTextField profField = new JTextField();
        JTextField scheduleField = new JTextField();

        Object[] message = {
                "Nombre de la Materia:", nameField,
                "Profesor/a:", profField,
                "Horario de clases:", scheduleField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Agregar Nueva Materia", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION && !nameField.getText().trim().isEmpty()) {
            Subject newSubject = new Subject(
                    "s_" + System.currentTimeMillis(),
                    nameField.getText().trim(),
                    "#4f46e5",
                    profField.getText().trim(),
                    scheduleField.getText().trim()
            );

            // Add default unit & topic so user can track progress immediately
            Unit u1 = new Unit("u_" + System.currentTimeMillis(), 1, "Unidad 1: Introducción");
            u1.addTopic(new Topic("t_" + System.currentTimeMillis() + "_1", "Conceptos Fundamentales", false));
            u1.addTopic(new Topic("t_" + System.currentTimeMillis() + "_2", "Ejercicios Prácticos", false));
            newSubject.getUnits().add(u1);

            DataManager.getInstance().addSubject(newSubject);
            if (mainFrame != null) {
                mainFrame.refreshAllPanels();
            } else {
                refresh();
            }
        }
    }
}