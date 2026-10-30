package com.planificador.panel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import panel.core.Registro;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lo que el sincronizador ya mandó al panel (último registro por id) y las fechas recordadas de las notas
 * sin fecha. Vive en {@code panel-estado.json}, junto al archivo de datos. Nunca lanza.
 */
public final class EstadoPanel {

    private static final Logger LOG = Logger.getLogger(EstadoPanel.class.getName());
    private static final String NOMBRE = "panel-estado.json";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public record Estado(Map<String, Registro> registros, Map<String, String> fechasNotas) {
        public static Estado vacio() {
            return new Estado(new LinkedHashMap<>(), new LinkedHashMap<>());
        }
    }

    /** Forma del archivo en disco. */
    private static final class Archivo {
        Map<String, Registro> registros;
        Map<String, String> fechasNotas;
    }

    private final Path archivo;

    public EstadoPanel(Path archivo) {
        this.archivo = archivo;
    }

    /** El archivo de estado va en la misma carpeta que el de datos. */
    public static EstadoPanel junto(Path archivoDeDatos) {
        Path carpeta = archivoDeDatos.toAbsolutePath().getParent();
        return new EstadoPanel(carpeta.resolve(NOMBRE));
    }

    public Path archivo() {
        return archivo;
    }

    /** Ausente, ilegible o dañado: estado vacío. */
    public Estado cargar() {
        if (!Files.isRegularFile(archivo)) return Estado.vacio();
        try (Reader r = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            Archivo a = GSON.fromJson(r, Archivo.class);
            if (a == null) return Estado.vacio();
            Map<String, Registro> registros = new LinkedHashMap<>();
            if (a.registros != null) {
                for (Map.Entry<String, Registro> e : a.registros.entrySet()) {
                    Registro reg = e.getValue();
                    // Una entrada sin id, o con un id distinto de su clave, es basura: todo el archivo es dudoso.
                    if (e.getKey() == null || reg == null || !e.getKey().equals(reg.idEnFuente())) {
                        LOG.warning("El estado del panel tiene entradas inválidas; se vuelve a mandar todo.");
                        return Estado.vacio();
                    }
                    registros.put(e.getKey(), reg);
                }
            }
            Map<String, String> fechas = new LinkedHashMap<>();
            if (a.fechasNotas != null) {
                a.fechasNotas.forEach((id, f) -> {
                    if (id != null && f != null) fechas.put(id, f);
                });
            }
            return new Estado(registros, fechas);
        } catch (IOException | RuntimeException e) {
            LOG.log(Level.WARNING, "No se pudo leer el estado del panel ({0}); se vuelve a mandar todo.",
                    e.getClass().getSimpleName());
            return Estado.vacio();
        }
    }

    /** Escritura atómica (temporal en la misma carpeta y mover). Un fallo solo se registra. */
    public void guardar(Estado estado) {
        Path tmp = null;
        try {
            Path carpeta = archivo.toAbsolutePath().getParent();
            Files.createDirectories(carpeta);
            Archivo a = new Archivo();
            a.registros = estado.registros();
            a.fechasNotas = estado.fechasNotas();
            tmp = Files.createTempFile(carpeta, "panel-estado", ".tmp");
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(a, w);
            }
            try {
                Files.move(tmp, archivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, archivo, StandardCopyOption.REPLACE_EXISTING);
            }
            tmp = null;
        } catch (IOException | RuntimeException e) {
            LOG.log(Level.WARNING, "No se pudo guardar el estado del panel: {0}", e.getClass().getSimpleName());
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignorada) {
                    // era un temporal
                }
            }
        }
    }

    /** Olvida lo mandado (para "Reenviar todo") pero conserva las fechas recordadas de las notas. */
    public void vaciar() {
        Estado actual = cargar();
        guardar(new Estado(new LinkedHashMap<>(), actual.fechasNotas()));
    }
}
