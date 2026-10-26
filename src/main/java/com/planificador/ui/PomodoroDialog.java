package com.planificador.ui;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Ventana (no modal) del Pomodoro: se puede seguir usando el programa mientras corre. */
public class PomodoroDialog extends JDialog {

    public PomodoroDialog(MainWindow owner) {
        super(owner, "Pomodoro", false);
        PomodoroPanel panel = new PomodoroPanel(() -> {
            if (owner != null) owner.refreshAllPanels();
        });
        setContentPane(panel);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                panel.stop();
            }
        });
        setSize(440, 360);
        setLocationRelativeTo(owner);
    }
}
