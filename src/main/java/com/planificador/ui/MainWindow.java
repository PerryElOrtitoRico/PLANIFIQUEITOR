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
        tabs.addTab("Historial", new HistorialPanel());
        setContentPane(tabs);

        DataManager dm = DataManager.getInstance();
        dm.setSaveErrorListener(msg -> SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, msg, "Error al guardar", JOptionPane.ERROR_MESSAGE)));
        if (dm.getLoadWarning() != null) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, dm.getLoadWarning(), "Datos dañados", JOptionPane.WARNING_MESSAGE));
        }
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
            if (comp instanceof HistorialPanel) ((HistorialPanel) comp).refresh();
        }
    }

    public void showCard(String cardName) {
        int i = switch (cardName) {
            case "HOY" -> 0; case "SEMANAL" -> 1; case "MATERIAS" -> 2; case "FECHAS" -> 3;
            case "STATS" -> 4; case "TIEMPO" -> 5; case "CURSADA" -> 6; case "HISTORIAL" -> 7; default -> -1;
        };
        if (i >= 0) tabs.setSelectedIndex(i);
    }
}