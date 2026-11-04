package com.planificador.ui;

import javax.swing.*;
import java.awt.*;

/** Mini-ventana sin bordes, siempre encima de todo, arriba a la izquierda de la pantalla: muestra el tiempo del Pomodoro. */
final class RelojFlotante extends JWindow {

    private final JLabel texto = new JLabel(" ", SwingConstants.CENTER);

    RelojFlotante() {
        super();
        setAlwaysOnTop(true);
        setFocusableWindowState(false);   // no le saca el foco a lo que se esté usando
        texto.setFont(new Font("SansSerif", Font.BOLD, 22));
        texto.setOpaque(true);
        texto.setBackground(new Color(30, 30, 30, 235));
        texto.setForeground(Color.WHITE);
        texto.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        setContentPane(texto);
    }

    void mostrar(String t) {
        texto.setText(t);
        pack();
        Rectangle pantalla = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .getDefaultConfiguration().getBounds();
        Insets margen = Toolkit.getDefaultToolkit().getScreenInsets(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration());
        setLocation(pantalla.x + margen.left + 8, pantalla.y + margen.top + 8);
    }
}
