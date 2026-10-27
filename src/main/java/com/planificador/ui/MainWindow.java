package com.planificador.ui;

import com.planificador.service.DataManager;

import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame {
    private JTabbedPane tabs;

    public MainWindow() {
        setTitle("Planificador de estudio");
        setSize(1100, 720);
        setMinimumSize(new Dimension(900, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        java.net.URL icono = MainWindow.class.getResource("/icono.png");
        if (icono != null) {
            setIconImage(new ImageIcon(icono).getImage());
        }

        initUI();
    }

    private void initUI() {
        // Pestañas arriba, como RUTINEITOR
        tabs = new JTabbedPane();
        tabs.addTab("Hoy", new TodayPanel(this));
        tabs.addTab("Guía semanal", new WeeklyPanel(this));
        tabs.addTab("Materias", new SubjectsPanel(this));
        tabs.addTab("Fechas clave", new DatesPanel(this));
        tabs.addTab("Progreso", new StatsPanel(this));
        tabs.addTab("Tiempo", new StudyTimePanel(this));
        tabs.addTab("Cursada", new CoursePanel(this));
        setContentPane(tabs);

        JMenuBar barra = new JMenuBar();
        JMenu menu = new JMenu("Datos");
        JMenuItem ejemplo = new JMenuItem("Restablecer datos de ejemplo…");
        ejemplo.addActionListener(e -> {
            int opt = JOptionPane.showConfirmDialog(this,
                    "¿Restablecer los datos de ejemplo del sistema?",
                    "Restablecer", JOptionPane.YES_NO_OPTION);
            if (opt == JOptionPane.YES_OPTION) {
                DataManager.getInstance().loadInitialDemoData();
                refreshAllPanels();
            }
        });
        menu.add(ejemplo);
        barra.add(menu);
        setJMenuBar(barra);
    }

    public void refreshAllPanels() {
        for (Component comp : tabs.getComponents()) {
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
        int i = switch (cardName) {
            case "HOY" -> 0; case "SEMANAL" -> 1; case "MATERIAS" -> 2; case "FECHAS" -> 3;
            case "STATS" -> 4; case "TIEMPO" -> 5; case "CURSADA" -> 6; default -> -1;
        };
        if (i >= 0) tabs.setSelectedIndex(i);
    }
}