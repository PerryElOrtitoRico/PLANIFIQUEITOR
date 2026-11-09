package com.planificador.app.datos;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.planificador.app.logica.EstadoReloj;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** El estado del reloj en un archivo local (no sincronizado), escrito de forma atómica. */
public class AlmacenReloj {

    private final Path archivo;
    private final Gson gson = new Gson();

    public AlmacenReloj(Path archivo) {
        this.archivo = archivo;
    }

    /** null si no hay estado o el archivo está dañado. */
    public synchronized EstadoReloj leer() throws IOException {
        if (!Files.exists(archivo)) return null;
        try {
            return gson.fromJson(new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8), EstadoReloj.class);
        } catch (JsonParseException e) {
            return null;
        }
    }

    public synchronized void guardar(EstadoReloj e) throws IOException {
        Files.createDirectories(archivo.getParent());
        Path tmp = archivo.resolveSibling(archivo.getFileName() + ".tmp");
        Files.write(tmp, gson.toJson(e).getBytes(StandardCharsets.UTF_8));
        try {
            Files.move(tmp, archivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(tmp, archivo, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public synchronized void borrar() throws IOException {
        Files.deleteIfExists(archivo);
    }
}
