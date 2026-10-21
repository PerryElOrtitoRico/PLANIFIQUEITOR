package com.planificador;

import com.formdev.flatlaf.FlatIntelliJLaf;
import com.planificador.ui.MainWindow;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        // Configurar Look & Feel moderno estilo FlatLaf
        try {
            UIManager.setLookAndFeel(new FlatIntelliJLaf());
            // Personalizar bordes y fuentes globales
            UIManager.put("Button.arc", 12);
            UIManager.put("Component.arc", 12);
            UIManager.put("ProgressBar.arc", 12);
            UIManager.put("TextComponent.arc", 10);
        } catch (Exception ex) {
            System.err.println("No se pudo cargar FlatLaf, usando estilo por defecto del sistema.");
        }

        // Ejecutar la ventana principal en el Hilo de Eventos de AWT (EDT)
        SwingUtilities.invokeLater(() -> {
            MainWindow frame = new MainWindow();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}