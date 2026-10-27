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

import com.planificador.model.CourseRules;
import com.planificador.model.Evaluation;
import com.planificador.model.PomodoroSettings;
import com.planificador.model.StudyLog;
import com.planificador.model.StudySession;
import com.planificador.model.Subject;

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
    void sinArchivosArrancaVacio() {
        Path nuevo = tmp.resolve("a/b/planificador_datos.json");

        DataManager dm = new DataManager(nuevo, List.of(tmp.resolve("no-existe.json")));

        assertTrue(dm.getSubjects().isEmpty());
        assertTrue(dm.getStudySessions().isEmpty());
        assertNull(dm.getLoadWarning());
    }

    @Test
    void guardaYVuelveACargarDesdeElMismoArchivo() {
        Path nuevo = tmp.resolve("datos/planificador_datos.json");
        DataManager dm = new DataManager(nuevo, List.of());
        dm.addSubject(new Subject("s1", "Física", "#000000", "", ""));
        dm.addSubject(new Subject("s2", "Química", "#000000", "", ""));
        dm.removeSubject("s1");

        DataManager recargado = new DataManager(nuevo, List.of());

        assertEquals(1, recargado.getSubjects().size());
        assertEquals("Química", recargado.getSubjects().get(0).getName());
    }

    @Test
    void unArchivoDanadoSeRespaldaAntesDeReemplazarlo() throws IOException {
        Path nuevo = tmp.resolve("datos/planificador_datos.json");
        Files.createDirectories(nuevo.getParent());
        Files.writeString(nuevo, "{ esto no es json válido", StandardCharsets.UTF_8);

        DataManager dm = new DataManager(nuevo, List.of());
        assertNotNull(dm.getLoadWarning(), "debe avisar que el archivo estaba dañado");
        assertTrue(dm.getSubjects().isEmpty());

        try (var archivos = Files.list(nuevo.getParent())) {
            List<Path> respaldos = archivos.filter(p -> p.getFileName().toString().contains(".danado-")).toList();
            assertEquals(1, respaldos.size(), "debe quedar una copia del archivo dañado");
            assertEquals("{ esto no es json válido", Files.readString(respaldos.get(0), StandardCharsets.UTF_8));
        }
    }

    @Test
    void cargaUnJsonViejoSinLosCamposNuevos() throws IOException {
        Path archivo = tmp.resolve("viejo/planificador_datos.json"); Files.createDirectories(archivo.getParent()); Files.writeString(archivo, DATOS_EJEMPLO, StandardCharsets.UTF_8);
        DataManager dm = new DataManager(archivo, List.of());
        assertTrue(dm.getStudyLogs().isEmpty());
        assertEquals(25, dm.getPomodoroSettings().getWorkMinutes());
        assertNull(dm.getSubjects().get(0).getCourse());
    }

    @Test
    void guardaYRecargaLoNuevo() {
        Path archivo = tmp.resolve("nuevo/planificador_datos.json"); DataManager dm = new DataManager(archivo, List.of());
        dm.addSubject(new Subject("s1", "Física", "#000000", "", ""));
        dm.addStudyLog(new StudyLog("l1", "s1", null, null, "2026-10-25", "10:00", 25));
        dm.setPomodoroSettings(new PomodoroSettings(50, 10, 20, 3));
        Subject s = dm.getSubjects().get(0); s.getOrCreateCourse().getEvaluations().add(new Evaluation("e1", Evaluation.PARCIAL, 1, false, 7.5, null)); dm.saveData();
        DataManager otra = new DataManager(archivo, List.of());
        assertEquals(1, otra.getStudyLogs().size());
        assertEquals(50, otra.getPomodoroSettings().getWorkMinutes());
        assertEquals(7.5, otra.getSubjects().get(0).getCourse().getEvaluations().get(0).getGrade(), 1e-9);
    }

    @Test
    void marcarUnaSesionDeLaGuiaLaDejaEnElHistorial() {
        Path archivo = tmp.resolve("h/planificador_datos.json");
        DataManager dm = new DataManager(archivo, List.of());
        StudySession ss = new StudySession("x1", "lunes", "s1", "Integrales", 60, false);
        dm.addStudySession(ss);

        dm.setSessionCompleted(ss, true);
        assertEquals(1, dm.getStudyLogs().size());
        assertTrue(dm.getStudyLogs().get(0).isFromGuide());
        assertEquals(60, dm.getStudyLogs().get(0).getMinutes());

        dm.setSessionCompleted(ss, true); // no se duplica el mismo día
        assertEquals(1, dm.getStudyLogs().size());

        dm.setSessionCompleted(ss, false);
        assertTrue(dm.getStudyLogs().isEmpty());
    }

    @Test
    void elHechoDeLaGuiaValeSoloEnLaSemanaEnQueSeMarco() {
        StudySession ss = new StudySession("x1", "lunes", "s1", "Integrales", 60, false);
        ss.setCompleted(true);
        assertTrue(ss.isCompleted());
        // simula una marca de la semana pasada
        try {
            var f = StudySession.class.getDeclaredField("completedWeek");
            f.setAccessible(true);
            f.set(ss, java.time.LocalDate.now().minusWeeks(1).toString());
        } catch (ReflectiveOperationException e) { fail(e); }
        assertFalse(ss.isCompleted());
    }

    @Test
    void siSeCronometroLaSesionNoSeDuplicaAlMarcarla() {
        DataManager dm = new DataManager(tmp.resolve("c/planificador_datos.json"), List.of());
        StudySession ss = new StudySession("x1", "lunes", "s1", "Integrales", 60, false);
        dm.addStudySession(ss);
        StudyLog crono = new StudyLog("cron_x1_" + java.time.LocalDate.now() + "_a", "s1", null, "Integrales",
                java.time.LocalDate.now().toString(), "10:00", 30);
        crono.setSource("cronometro");
        dm.addStudyLog(crono);

        dm.setSessionCompleted(ss, true);

        assertEquals(1, dm.getStudyLogs().size());
        assertEquals(30, dm.getStudyLogs().get(0).getMinutes());
    }
}
