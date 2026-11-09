package com.planificador.app.logica;

import com.planificador.model.PomodoroSettings;

/** Ajustes del Pomodoro con los rangos del diálogo de la PC (PomodoroPanel.editSettings). */
public final class FormularioAjustes {

    private FormularioAjustes() { }

    public static PomodoroSettings ajustes(String estudio, String corto, String largo, String cada) {
        return new PomodoroSettings(entero(estudio, 1, 180, "El estudio"), entero(corto, 1, 60, "El descanso corto"),
                entero(largo, 1, 120, "El descanso largo"), entero(cada, 1, 12, "Los ciclos antes del descanso largo"));
    }

    private static int entero(String texto, int min, int max, String que) {
        int v;
        try {
            v = Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(que + " tiene que ser un número entero.");
        }
        if (v < min || v > max) throw new IllegalArgumentException(que + " tiene que estar entre " + min + " y " + max + ".");
        return v;
    }
}
