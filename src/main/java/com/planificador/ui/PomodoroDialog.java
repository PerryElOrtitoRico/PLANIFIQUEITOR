package com.planificador.ui;

import javax.swing.*;

/**
 * Ventana (no modal) del Pomodoro: se puede seguir usando el programa mientras corre. Hay una sola: cerrarla o
 * minimizarla no frena el reloj (solo la oculta) y el {@link RelojFlotante} sigue arriba a la izquierda de la pantalla.
 */
public class PomodoroDialog extends JDialog {

    private static PomodoroDialog unica;

    private final RelojFlotante reloj = new RelojFlotante();

    public PomodoroDialog(MainWindow owner) {
        super(owner, "Pomodoro", false);
        PomodoroPanel panel = new PomodoroPanel(() -> {
            if (owner != null) owner.refreshAllPanels();
        });
        setContentPane(panel);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        panel.setAlActualizar(reloj::mostrar);
        setSize(460, 420);
        setLocationRelativeTo(owner);
    }

    /** Abre la ventana del Pomodoro (la crea la primera vez) y muestra el reloj flotante. */
    public static void mostrar(MainWindow owner) {
        if (unica == null) unica = new PomodoroDialog(owner);
        unica.reloj.setVisible(true);
        unica.setVisible(true);
        unica.toFront();
    }
}
