package com.planificador.ui;

import com.planificador.model.ImportantDate;
import com.planificador.model.Subject;
import com.planificador.service.Countdown;
import com.planificador.service.DataManager;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/** Cuenta regresiva de las fechas importantes de los próximos días, para la pantalla de Hoy. */
public class CountdownPanel extends JPanel {

    static final int DIAS = 30;
    static final Color URGENTE = new Color(225, 29, 72);   // 7 días o menos
    static final Color NORMAL = new Color(51, 65, 85);

    public CountdownPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));
        refresh(LocalDate.now());
    }

    /** Vuelve a armar la lista con la fecha de hoy. */
    public void refresh() {
        refresh(LocalDate.now());
    }

    /** Arma una etiqueta por fecha próxima (de hoy a DIAS días); si no hay, una sola etiqueta que lo dice. */
    void refresh(LocalDate today) {
        // TODO T06
        throw new UnsupportedOperationException();
    }

    /** Las etiquetas que hay ahora (para los tests). */
    java.util.List<JLabel> labels() {
        java.util.List<JLabel> out = new java.util.ArrayList<>();
        for (Component c : getComponents()) if (c instanceof JLabel l) out.add(l);
        return out;
    }
}
