package com.planificador.ui;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Ventana (no modal) del Pomodoro: se puede seguir usando el programa mientras corre. Además muestra el {@link RelojFlotante} arriba a la izquierda de la pantalla. */
public class PomodoroDialog extends JDialog {

    public PomodoroDialog(MainWindow owner) {
        super(owner, "Pomodoro", false);
        PomodoroPanel panel = new PomodoroPanel(() -> {
            if (owner != null) owner.refreshAllPanels();
        });
        setContentPane(panel);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        RelojFlotante reloj = new RelojFlotante();
        panel.setAlActualizar(reloj::mostrar);
        reloj.setVisible(true);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                panel.stop();
                reloj.dispose();
            }
        });
        setSize(440, 360);
        setLocationRelativeTo(owner);
    }
}
