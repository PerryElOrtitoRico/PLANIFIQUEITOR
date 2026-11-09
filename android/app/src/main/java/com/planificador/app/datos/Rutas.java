package com.planificador.app.datos;

import android.content.Context;
import android.os.Environment;

import java.nio.file.Path;

/** Dónde están los datos en el teléfono. */
public final class Rutas {

    private Rutas() { }

    /** La carpeta que sincroniza Syncthing: Documents/Estudieitor/datos. */
    public static Path carpetaDatos() {
        return carpetaDatos(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toPath());
    }

    static Path carpetaDatos(Path documents) {
        return documents.resolve("Estudieitor").resolve("datos");
    }

    /** Ancla local de DataManager (no existe: no se migra nada). Junto a ella va el estado del reloj. */
    public static Path archivoLocal(Context c) {
        return c.getFilesDir().toPath().resolve("planificador_datos.json");
    }

    /** Si la app tiene "acceso a todos los archivos". */
    public static boolean tienePermiso() {
        return Environment.isExternalStorageManager();
    }
}
