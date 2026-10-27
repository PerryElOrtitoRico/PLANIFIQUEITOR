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

                JButton addUnitBtn = new JButton("Agregar unidad");
                addUnitBtn.addActionListener(e -> addUnit(subject));
                JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
                botones.setOpaque(false);
                botones.add(addUnitBtn);
                botones.add(deleteBtn);

                topInfo.add(textContainer, BorderLayout.WEST);
                topInfo.add(botones, BorderLayout.EAST);

                // Units & Topics list
                JPanel unitsContainer = new JPanel();
                unitsContainer.setLayout(new BoxLayout(unitsContainer, BoxLayout.Y_AXIS));
                unitsContainer.setOpaque(false);
                unitsContainer.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

                if (subject.getUnits().isEmpty()) {
                    JLabel noUnits = new JLabel("<html><font color='#94a3b8'><i>Sin unidades: usá 'Agregar unidad'</i></font></html>");
                    unitsContainer.add(noUnits);
                } else {
                    for (Unit u : subject.getUnits()) {
                        JPanel uRow = new JPanel(new BorderLayout());
                        uRow.setOpaque(false);
                        JLabel uLabel = new JLabel(u.getTitle());
                        uLabel.setFont(uLabel.getFont().deriveFont(Font.BOLD));
                        JPanel uBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
                        uBtns.setOpaque(false);
                        uBtns.add(smallButton("Agregar temas", () -> addTopics(u)));
                        uBtns.add(smallButton("Renombrar", () -> renameUnit(u)));
                        uBtns.add(smallButton("Subir", () -> moveUnit(subject, u, -1)));
                        uBtns.add(smallButton("Bajar", () -> moveUnit(subject, u, 1)));
                        uBtns.add(smallButton("Eliminar", () -> removeUnit(subject, u)));
                        uRow.add(uLabel, BorderLayout.WEST);
                        uRow.add(uBtns, BorderLayout.EAST);
                        uRow.setBorder(BorderFactory.createEmptyBorder(6, 0, 2, 0));
                        unitsContainer.add(uRow);

                        for (Topic topic : u.getTopics()) {
                            JPanel tRow = new JPanel(new BorderLayout());
                            tRow.setOpaque(false);
                            JCheckBox topicChk = new JCheckBox(topic.getTitle());
                            topicChk.setSelected(topic.isCompleted());
                            topicChk.setOpaque(false);
                            topicChk.addActionListener(ev -> {
                                topic.setCompleted(topicChk.isSelected());
                                DataManager.getInstance().saveData();
                                changed();
                            });
                            JPanel tBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
                            tBtns.setOpaque(false);
                            tBtns.add(smallButton("Renombrar", () -> renameTopic(topic)));
                            tBtns.add(smallButton("Eliminar", () -> { u.getTopics().remove(topic); DataManager.getInstance().saveData(); changed(); }));
                            tRow.add(topicChk, BorderLayout.WEST);
                            tRow.add(tBtns, BorderLayout.EAST);
                            tRow.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
                            unitsContainer.add(tRow);
                        }
                    }
                }

                card.add(topInfo, BorderLayout.NORTH);
                card.add(unitsContainer, BorderLayout.CENTER);

                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
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
                    "s_" + java.util.UUID.randomUUID(),
                    nameField.getText().trim(),
                    "#4f46e5",
                    profField.getText().trim(),
                    scheduleField.getText().trim()
            );

            DataManager.getInstance().addSubject(newSubject);
            if (mainFrame != null) {
                mainFrame.refreshAllPanels();
            } else {
                refresh();
            }
        }
    }

    private void changed() {
        if (mainFrame != null) mainFrame.refreshAllPanels();
        else refresh();
    }

    private static JButton smallButton(String text, Runnable action) {
        JButton b = new JButton(text);
        b.setMargin(new Insets(1, 6, 1, 6));
        b.setFocusPainted(false);
        b.addActionListener(e -> action.run());
        return b;
    }

    /** Pide un texto; devuelve null si cancela o queda vacío. */
    private String ask(String message, String title, String initial) {
        JTextField f = new JTextField(initial, 28);
        int op = JOptionPane.showConfirmDialog(this, new Object[]{message, f}, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        String t = f.getText().trim();
        return op == JOptionPane.OK_OPTION && !t.isEmpty() ? t : null;
    }

    private void addUnit(Subject subject) {
        String title = ask("Nombre de la unidad (escribilo como quieras, ej. \"Unidad 1: Cinemática\" o \"Anexos\"):",
                "Agregar unidad", "Unidad " + (subject.getUnits().size() + 1) + ": ");
        if (title == null) return;
        subject.getUnits().add(new Unit("u_" + java.util.UUID.randomUUID(), subject.getUnits().size() + 1, title));
        DataManager.getInstance().saveData();
        changed();
    }

    private void renameUnit(Unit u) {
        String title = ask("Nuevo nombre de la unidad:", "Renombrar unidad", u.getTitle());
        if (title == null) return;
        u.setTitle(title);
        DataManager.getInstance().saveData();
        changed();
    }

    private void removeUnit(Subject subject, Unit u) {
        int n = u.getTopics().size();
        int op = JOptionPane.showConfirmDialog(this,
                "¿Eliminar \"" + u.getTitle() + "\"" + (n > 0 ? " y sus " + n + " temas" : "") + "?",
                "Eliminar unidad", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (op != JOptionPane.YES_OPTION) return;
        subject.getUnits().remove(u);
        DataManager.getInstance().saveData();
        changed();
    }

    private void moveUnit(Subject subject, Unit u, int delta) {
        List<Unit> units = subject.getUnits();
        int i = units.indexOf(u), j = i + delta;
        if (i < 0 || j < 0 || j >= units.size()) return;
        units.remove(i);
        units.add(j, u);
        DataManager.getInstance().saveData();
        changed();
    }

    private void addTopics(Unit u) {
        JTextArea area = new JTextArea(8, 32);
        int op = JOptionPane.showConfirmDialog(this, new Object[]{"Temas de \"" + u.getTitle() + "\" (uno por línea):", new JScrollPane(area)},
                "Agregar temas", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (op != JOptionPane.OK_OPTION) return;
        for (String line : area.getText().split("\\R")) {
            if (!line.trim().isEmpty()) u.addTopic(new Topic("t_" + java.util.UUID.randomUUID(), line.trim(), false));
        }
        DataManager.getInstance().saveData();
        changed();
    }

    private void renameTopic(Topic t) {
        String title = ask("Nuevo nombre del tema:", "Renombrar tema", t.getTitle());
        if (title == null) return;
        t.setTitle(title);
        DataManager.getInstance().saveData();
        changed();
    }
}
