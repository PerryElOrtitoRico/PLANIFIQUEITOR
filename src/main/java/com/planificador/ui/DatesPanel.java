package com.planificador.ui;

import com.planificador.model.ImportantDate;
import com.planificador.model.Subject;
import com.planificador.service.DataManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DatesPanel extends JPanel {
    private MainWindow mainFrame;
    private JPanel datesList;

    public DatesPanel() {
        this(null);
    }

    public DatesPanel(MainWindow mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        initUI();
    }

    private void initUI() {
        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("⏰ Fechas Importantes (Parciales, TPs y Finales)");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));

        JButton addBtn = new JButton("＋ Agregar Fecha");
        addBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        addBtn.setBackground(new Color(79, 70, 229));
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);
        addBtn.addActionListener(e -> showAddDialog());

        header.add(title, BorderLayout.WEST);
        header.add(addBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        datesList = new JPanel();
        datesList.setLayout(new BoxLayout(datesList, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(datesList);
        scrollPane.setBorder(null);

        add(scrollPane, BorderLayout.CENTER);

        refresh();
    }

    public void refresh() {
        datesList.removeAll();
        List<ImportantDate> list = DataManager.getInstance().getImportantDates();

        if (list.isEmpty()) {
            JPanel emptyPanel = new JPanel();
            emptyPanel.add(new JLabel("<html><font color='#64748b'>No hay fechas importantes registradas.</font></html>"));
            datesList.add(emptyPanel);
        } else {
            for (ImportantDate d : list) {
                Subject s = DataManager.getInstance().getSubjectById(d.getSubjectId());
                String subName = s != null ? s.getName() : "General";

                JPanel card = new JPanel(new BorderLayout());
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));
                card.setBackground(Color.WHITE);
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(226, 232, 240)),
                        BorderFactory.createEmptyBorder(10, 12, 10, 12)
                ));

                long daysRem = d.getDaysRemaining();
                String daysText = daysRem == 0 ? "¡Hoy!" : (daysRem > 0 ? "Faltan " + daysRem + " días" : "Pasó hace " + Math.abs(daysRem) + " días");

                JLabel lbl = new JLabel("<html><b>" + d.getTitle() + "</b> (" + subName + ") - Fecha: " + d.getDate() + " | <font color='#e11d48'><b>" + daysText + "</b></font></html>");
                card.add(lbl, BorderLayout.CENTER);

                JButton delBtn = new JButton("🗑️ Eliminar");
                delBtn.addActionListener(e -> {
                    DataManager.getInstance().removeImportantDate(d.getId());
                    if (mainFrame != null) {
                        mainFrame.refreshAllPanels();
                    } else {
                        refresh();
                    }
                });
                card.add(delBtn, BorderLayout.EAST);

                datesList.add(card);
                datesList.add(Box.createRigidArea(new Dimension(0, 8)));
            }
        }

        datesList.revalidate();
        datesList.repaint();
    }

    private void showAddDialog() {
        JTextField titleField = new JTextField();
        JTextField dateField = new JTextField("2026-08-15");
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"parcial", "final", "tp", "entrega"});

        List<Subject> subs = DataManager.getInstance().getSubjects();
        String[] subNames = subs.isEmpty() ? new String[]{"General"} : subs.stream().map(Subject::getName).toArray(String[]::new);
        JComboBox<String> subCombo = new JComboBox<>(subNames);

        Object[] message = {
                "Título del examen/entrega:", titleField,
                "Materia relacionada:", subCombo,
                "Fecha (YYYY-MM-DD):", dateField,
                "Tipo de evaluación:", typeCombo
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Nueva Fecha Importante", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION && !titleField.getText().trim().isEmpty()) {
            String selectedSubId = (subs.isEmpty() || subCombo.getSelectedIndex() >= subs.size())
                    ? "general"
                    : subs.get(subCombo.getSelectedIndex()).getId();

            ImportantDate newDate = new ImportantDate(
                    "d_" + System.currentTimeMillis(),
                    titleField.getText().trim(),
                    selectedSubId,
                    dateField.getText().trim(),
                    (String) typeCombo.getSelectedItem(),
                    ""
            );
            DataManager.getInstance().addImportantDate(newDate);
            if (mainFrame != null) {
                mainFrame.refreshAllPanels();
            } else {
                refresh();
            }
        }
    }
}