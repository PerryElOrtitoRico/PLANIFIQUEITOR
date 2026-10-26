package com.planificador.ui;

import com.planificador.service.DataManager;

import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame {
    private JPanel mainContentPanel;
    private CardLayout cardLayout;

    public MainWindow() {
        setTitle("Planificador de Estudio Desktop - Java");
        setSize(1100, 720);
        setMinimumSize(new Dimension(900, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // Navigation Sidebar
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(15, 23, 42)); // Dark Slate
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));

        // Title Header
        JLabel logoLabel = new JLabel("🎓 Planificador");
        logoLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subLogo = new JLabel("Estudio Desktop (Java)");
        subLogo.setFont(new Font("SansSerif", Font.PLAIN, 11));
        subLogo.setForeground(new Color(148, 163, 184));
        subLogo.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(logoLabel);
        sidebar.add(subLogo);
        sidebar.add(Box.createRigidArea(new Dimension(0, 30)));

        // Cards & Layout
        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);

        TodayPanel todayPanel = new TodayPanel(this);
        WeeklyPanel weeklyPanel = new WeeklyPanel(this);
        SubjectsPanel subjectsPanel = new SubjectsPanel(this);
        DatesPanel datesPanel = new DatesPanel(this);
        StatsPanel statsPanel = new StatsPanel(this);
        StudyTimePanel timePanel = new StudyTimePanel(this);
        CoursePanel coursePanel = new CoursePanel(this);

        mainContentPanel.add(todayPanel, "HOY");
        mainContentPanel.add(weeklyPanel, "SEMANAL");
        mainContentPanel.add(subjectsPanel, "MATERIAS");
        mainContentPanel.add(datesPanel, "FECHAS");
        mainContentPanel.add(statsPanel, "STATS");
        mainContentPanel.add(timePanel, "TIEMPO");
        mainContentPanel.add(coursePanel, "CURSADA");

        // Add Nav Buttons
        sidebar.add(createNavButton("🎯 Hoy", "HOY"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(createNavButton("📅 Guía Semanal", "SEMANAL"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(createNavButton("📚 Materias", "MATERIAS"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(createNavButton("⏰ Fechas Clave", "FECHAS"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(createNavButton("📊 Progreso", "STATS"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(createNavButton("⏳ Tiempo", "TIEMPO"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(createNavButton("🎓 Cursada", "CURSADA"));

        sidebar.add(Box.createVerticalGlue());

        // Reset Data Button
        JButton resetBtn = new JButton("🔄 Datos de Ejemplo");
        resetBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        resetBtn.setFocusPainted(false);
        resetBtn.addActionListener(e -> {
            int opt = JOptionPane.showConfirmDialog(this,
                    "¿Restablecer los datos de ejemplo del sistema?",
                    "Restablecer", JOptionPane.YES_NO_OPTION);
            if (opt == JOptionPane.YES_OPTION) {
                DataManager.getInstance().loadInitialDemoData();
                refreshAllPanels();
            }
        });
        sidebar.add(resetBtn);

        add(sidebar, BorderLayout.WEST);
        add(mainContentPanel, BorderLayout.CENTER);
    }

    private JButton createNavButton(String text, String cardName) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(190, 40));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.addActionListener(e -> cardLayout.show(mainContentPanel, cardName));
        return btn;
    }

    public void refreshAllPanels() {
        for (Component comp : mainContentPanel.getComponents()) {
            if (comp instanceof TodayPanel) ((TodayPanel) comp).refresh();
            if (comp instanceof WeeklyPanel) ((WeeklyPanel) comp).refresh();
            if (comp instanceof SubjectsPanel) ((SubjectsPanel) comp).refresh();
            if (comp instanceof DatesPanel) ((DatesPanel) comp).refresh();
            if (comp instanceof StatsPanel) ((StatsPanel) comp).refresh();
            if (comp instanceof StudyTimePanel) ((StudyTimePanel) comp).refresh();
            if (comp instanceof CoursePanel) ((CoursePanel) comp).refresh();
        }
    }

    public void showCard(String cardName) {
        cardLayout.show(mainContentPanel, cardName);
    }
}