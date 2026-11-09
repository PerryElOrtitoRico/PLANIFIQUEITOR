package com.planificador.app.logica;

import java.time.LocalDate;
import java.util.Locale;

/** Los días de la guía semanal, con las mismas claves que la PC ("lunes" … "domingo", sin tildes). */
public final class DiasSemana {

    public static final String[] CLAVES = {"lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo"};
    public static final String[] NOMBRES = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

    private DiasSemana() { }

    public static String clave(LocalDate d) {
        return CLAVES[d.getDayOfWeek().getValue() - 1];
    }

    /** "Miércoles" para "miercoles"; la clave tal cual si no es un día conocido. */
    public static String nombre(String clave) {
        for (int i = 0; i < CLAVES.length; i++) if (CLAVES[i].equalsIgnoreCase(clave)) return NOMBRES[i];
        return clave;
    }
}
