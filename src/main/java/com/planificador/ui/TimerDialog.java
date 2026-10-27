package com.planificador.ui;

import javax.swing.*;
import java.awt.*;

public class TimerDialog extends JDialog {
    private int secondsRemaining;
    private Timer timer;
    private JLabel timerLabel;
    private JButton startPauseBtn;
    private boolean isRunning = false;

    public TimerDialog(Frame owner, int initialMinutes) {
        super(owner, "⏱Sesión de Estudio (Cronómetro)", true);
        this.secondsRemaining = initialMinutes * 60;

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
            if (secondsRemaining > 0) {
                secondsRemaining--;
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
}