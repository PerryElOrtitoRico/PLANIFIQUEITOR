package com.planificador;

import com.planificador.ui.MainWindow;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        com.planificador.ui.Tema.instalar();

        // Ejecutar la ventana principal en el Hilo de Eventos de AWT (EDT)
        SwingUtilities.invokeLater(() -> {
            MainWindow frame = new MainWindow();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
