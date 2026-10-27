package com.planificador.ui;

import javax.swing.*;
import java.awt.*;

public class TimerDialog extends JDialog {
    private int secondsRemaining;
    private Timer timer;
    private JLabel timerLabel;
    private JButton startPauseBtn;
    private boolean isRunning = false;
    private long endNanos;
    private final int initialSeconds;
    private final String subjectId;      // puede ser null (cronómetro suelto)
    private final String description;    // puede ser null
    private final String sessionId;      // sesión de la guía de la que salió, o null
    private boolean registrado = false;

    public TimerDialog(Frame owner, int initialMinutes) {
        this(owner, initialMinutes, null, null, null);
    }

    /** Al cerrarse, los minutos cronometrados se guardan en el historial (con la materia y la sesión si las hay). */
    public TimerDialog(Frame owner, int initialMinutes, String subjectId, String description, String sessionId) {
        super(owner, "Sesión de Estudio (Cronómetro)", true);
        this.secondsRemaining = initialMinutes * 60;
        this.initialSeconds = secondsRemaining;
        this.subjectId = subjectId;
        this.description = description;
        this.sessionId = sessionId;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { registrar(owner); }
        });

        setSize(380, 240);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(15, 15));

        JPanel centerPanel = new JPanel(new GridBagLayout());
        timerLabel = new JLabel(formatTime(secondsRemaining));
        timerLabel.setFont(new Font("SansSerif", Font.BOLD, 48));
        centerPanel.add(timerLabel);
        add(centerPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout());
        startPauseBtn = new JButton("▶ Iniciar");
        startPauseBtn.addActionListener(e -> toggleTimer());

        JButton closeBtn = new JButton("Cerrar");
        closeBtn.addActionListener(e -> {
            if (timer != null) timer.stop();
            dispose();
        });

        btnPanel.add(startPauseBtn);
        btnPanel.add(closeBtn);
        add(btnPanel, BorderLayout.SOUTH);

        timer = new Timer(1000, e -> {
            secondsRemaining = (int) Math.max(0, Math.ceil((endNanos - System.nanoTime()) / 1e9));
            if (secondsRemaining > 0) {
                timerLabel.setText(formatTime(secondsRemaining));
            } else {
                timer.stop();
                JOptionPane.showMessageDialog(this, "¡Tiempo cumplido! Excelente sesión de estudio.");
                dispose();
            }
        });
    }

    private void toggleTimer() {
        if (isRunning) {
            timer.stop();
            startPauseBtn.setText("▶ Reanudar");
            isRunning = false;
        } else {
            endNanos = System.nanoTime() + secondsRemaining * 1_000_000_000L;
            timer.start();
            startPauseBtn.setText("⏸Pausar");
            isRunning = true;
        }
    }

    private String formatTime(int totalSecs) {
        int m = totalSecs / 60;
        int s = totalSecs % 60;
        return String.format("%02d:%02d", m, s);
    }

    /** Guarda en el historial lo cronometrado (mínimo 1 minuto). */
    private void registrar(Frame owner) {
        if (registrado) return;
        registrado = true;
        if (timer != null) timer.stop();
        int minutos = (initialSeconds - secondsRemaining) / 60;
        if (minutos < 1) return;
        java.time.LocalDateTime fin = java.time.LocalDateTime.now();
        String id = (sessionId != null ? "cron_" + sessionId + "_" + fin.toLocalDate() + "_" : "cron_") + java.util.UUID.randomUUID();
        com.planificador.model.StudyLog log = com.planificador.service.StudyLogFactory.fromWorkCycle(id, subjectId, null, description, fin, minutos);
        log.setSource("cronometro");
        com.planificador.service.DataManager.getInstance().addStudyLog(log);
        if (owner instanceof MainWindow mw) mw.refreshAllPanels();
    }
}
