package com.planificador.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DataManagerTest {

    private static final String DATOS_EJEMPLO = """
            {
              "subjects": [
                { "id": "m1", "name": "Mecánica de Fluidos", "color": "#000000",
                  "professor": "Prof", "schedule": "Lun", "units": [] }
              ],
              "studySessions": [],
              "importantDates": []
            }
            """;

    @TempDir
    Path tmp;

    @Test
    void usaXdgDataHomeCuandoEstaDefinido() {
        Path archivo = DataManager.resolveDataFile(Map.of("XDG_DATA_HOME", "/datos/xdg"), Path.of("/home/u"));
        assertEquals(Path.of("/datos/xdg/planificador-estudio/planificador_datos.json"), archivo);
    }

    @Test
    void usaLocalShareDelHomeSiNoHayXdg() {
        Path archivo = DataManager.resolveDataFile(Map.of(), Path.of("/home/u"));
        assertEquals(Path.of("/home/u/.local/share/planificador-estudio/planificador_datos.json"), archivo);
    }

    @Test
    void ignoraXdgDataHomeVacio() {
        Path archivo = DataManager.resolveDataFile(Map.of("XDG_DATA_HOME", " "), Path.of("/home/u"));
        assertEquals(Path.of("/home/u/.local/share/planificador-estudio/planificador_datos.json"), archivo);
    }

    @Test
    void migraElArchivoViejoSinBorrarlo() throws IOException {
        Path viejo = tmp.resolve("home/planificador_datos.json");
        Files.createDirectories(viejo.getParent());
        Files.writeString(viejo, DATOS_EJEMPLO, StandardCharsets.UTF_8);
        Path nuevo = tmp.resolve("share/planificador-estudio/planificador_datos.json");

        DataManager dm = new DataManager(nuevo, List.of(viejo));

        assertEquals("Mecánica de Fluidos", dm.getSubjects().get(0).getName());
        assertTrue(Files.exists(nuevo), "debe crear el archivo en la ubicación fija");
        assertEquals(DATOS_EJEMPLO, Files.readString(viejo, StandardCharsets.UTF_8), "el archivo viejo queda intacto");
    }

    @Test
    void entreVariosArchivosViejosMigraElMasReciente() throws IOException {
        Path plantillaVacia = tmp.resolve("proyecto/planificador_datos.json");
        Files.createDirectories(plantillaVacia.getParent());
        Files.writeString(plantillaVacia, "{\"subjects\": [], \"studySessions\": [], \"importantDates\": []}",
                StandardCharsets.UTF_8);
        Files.setLastModifiedTime(plantillaVacia, FileTime.fromMillis(1_000_000L));
        Path datosReales = tmp.resolve("home/planificador_datos.json");
        Files.createDirectories(datosReales.getParent());
        Files.writeString(datosReales, DATOS_EJEMPLO, StandardCharsets.UTF_8);
        Files.setLastModifiedTime(datosReales, FileTime.fromMillis(2_000_000L));
        Path nuevo = tmp.resolve("share/planificador_datos.json");

        DataManager dm = new DataManager(nuevo, List.of(plantillaVacia, datosReales));

        assertEquals("Mecánica de Fluidos", dm.getSubjects().get(0).getName());
    }

    @Test
    void prefiereElArchivoNuevoSiYaExiste() throws IOException {
        Path viejo = tmp.resolve("viejo.json");
        Files.writeString(viejo, DATOS_EJEMPLO.replace("Mecánica de Fluidos", "Vieja"), StandardCharsets.UTF_8);
        Path nuevo = tmp.resolve("nuevo/planificador_datos.json");
        Files.createDirectories(nuevo.getParent());
        Files.writeString(nuevo, DATOS_EJEMPLO, StandardCharsets.UTF_8);

        DataManager dm = new DataManager(nuevo, List.of(viejo));

        assertEquals("Mecánica de Fluidos", dm.getSubjects().get(0).getName());
    }

    @Test
    void sinArchivosCreaDatosDeEjemploEnLaUbicacionFija() {
        Path nuevo = tmp.resolve("a/b/planificador_datos.json");

        DataManager dm = new DataManager(nuevo, List.of(tmp.resolve("no-existe.json")));

        assertFalse(dm.getSubjects().isEmpty());
        assertTrue(Files.exists(nuevo));
    }

    @Test
    void guardaYVuelveACargarDesdeElMismoArchivo() {
        Path nuevo = tmp.resolve("datos/planificador_datos.json");
        DataManager dm = new DataManager(nuevo, List.of());
        int antes = dm.getSubjects().size();
        dm.removeSubject(dm.getSubjects().get(0).getId());

        DataManager recargado = new DataManager(nuevo, List.of());

        assertEquals(antes - 1, recargado.getSubjects().size());
    }

    @Test
    void unArchivoDanadoSeRespaldaAntesDeReemplazarlo() throws IOException {
        Path nuevo = tmp.resolve("datos/planificador_datos.json");
        Files.createDirectories(nuevo.getParent());
        Files.writeString(nuevo, "{ esto no es json válido", StandardCharsets.UTF_8);

        new DataManager(nuevo, List.of());

        try (var archivos = Files.list(nuevo.getParent())) {
            List<Path> respaldos = archivos.filter(p -> p.getFileName().toString().contains(".danado-")).toList();
            assertEquals(1, respaldos.size(), "debe quedar una copia del archivo dañado");
            assertEquals("{ esto no es json válido", Files.readString(respaldos.get(0), StandardCharsets.UTF_8));
        }
    }
}
