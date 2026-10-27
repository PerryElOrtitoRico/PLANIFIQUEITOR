package com.planificador.ui;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.SwingUtilities;
import java.awt.Window;
import java.util.prefs.Preferences;

/** Modo claro u oscuro (FlatLaf liso en ambos); la elección se recuerda entre ejecuciones. */
public final class Tema {
    private static final String CLAVE = "oscuro";
    private static final Preferences PREFS = Preferences.userNodeForPackage(Tema.class);

    private Tema() { }

    public static boolean esOscuro() { return PREFS.getBoolean(CLAVE, false); }

    /** Instala el tema guardado (antes de crear ventanas). */
    public static void instalar() {
        if (esOscuro()) FlatDarculaLaf.setup(); else FlatLightLaf.setup();
    }

    /** Cambia de tema y repinta las ventanas abiertas. */
    public static void cambiar(boolean oscuro) {
        PREFS.putBoolean(CLAVE, oscuro);
        instalar();
        for (Window w : Window.getWindows()) SwingUtilities.updateComponentTreeUI(w);
    }
}
