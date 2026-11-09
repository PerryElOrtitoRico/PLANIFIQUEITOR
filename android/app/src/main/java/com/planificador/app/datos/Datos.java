package com.planificador.app.datos;

import android.content.Context;

import com.planificador.model.PomodoroSettings;
import com.planificador.service.DataManager;

/** Inicia DataManager una vez por proceso (la app puede arrancar por la pantalla, la alarma o el arranque). */
public final class Datos {

    private static boolean iniciado;

    private Datos() { }

    /** false si no hay permiso de archivos: entonces no se lee ni se escribe nada. */
    public static synchronized boolean asegurar(Context c) {
        if (!Rutas.tienePermiso()) return false;
        if (!iniciado) {
            DataManager.iniciar(Rutas.archivoLocal(c), Rutas.carpetaDatos());
            DataManager.getInstance().setSaveErrorListener(m -> android.util.Log.w("Datos", m));
            iniciado = true;
        }
        return true;
    }

    /** Ajustes del Pomodoro de la carpeta, o los de siempre si no hay permiso. */
    public static PomodoroSettings ajustes(Context c) {
        return asegurar(c) ? DataManager.getInstance().getPomodoroSettings() : new PomodoroSettings();
    }
}
