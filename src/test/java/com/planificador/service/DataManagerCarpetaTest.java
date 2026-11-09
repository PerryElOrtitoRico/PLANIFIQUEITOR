package com.planificador.service;

import com.planificador.model.StudyLog;
import com.planificador.model.Subject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** DataManager sobre la carpeta sincronizada: migración, recarga y no pisar. */
class DataManagerCarpetaTest {

    static final String JSON = """
            {
              "subjects": [
                { "id": "m1", "name": "Mecánica de Fluidos", "color": "#000000", "professor": "P", "schedule": "Lun", "units": [],
                  "course": { "rules": { "minAttendancePercent": 75, "passGrade": 6, "promotionGrade": 8, "examCount": 2,
                                         "retakes": [true, true], "plannedClasses": 0 },
                              "evaluations": [ { "id": "e1", "kind": "parcial", "number": 1, "retake": false, "grade": 7 },
                                               { "id": "e2", "kind": "parcial", "number": 2, "retake": false, "grade": 5 } ],
                              "attendance": [ { "date": "2026-10-01", "present": true }, { "date": "2026-10-08", "present": false },
                                              { "date": "2026-10-15", "present": true } ] } },
                { "id": "m2", "name": "Análisis II", "color": "#111111", "professor": "Q", "schedule": "Mar", "units": [] }
              ],
              "studySessions": [
                { "id": "g1", "dayOfWeek": "lunes", "subjectId": "m1", "topicDescription": "Bernoulli", "durationMinutes": 60, "completed": false },
                { "id": "g2", "dayOfWeek": "martes", "subjectId": "m2", "topicDescription": "Series", "durationMinutes": 45, "completed": false }
              ],
              "importantDates": [ { "id": "f1", "title": "Primer parcial", "subjectId": "m1", "date": "2026-11-20", "type": "parcial" } ],
              "studyLogs": [
                { "id": "l1", "subjectId": "m1", "date": "2026-10-20", "startTime": "10:00", "minutes": 25 },
                { "id": "l2", "subjectId": "m1", "date": "2026-10-21", "startTime": "10:00", "minutes": 25 },
                { "id": "l3", "subjectId": "m2", "date": "2026-10-13", "startTime": "10:00", "minutes": 50 }
              ],
              "pomodoroSettings": { "workMinutes": 30, "shortBreakMinutes": 5, "longBreakMinutes": 15, "cyclesBeforeLongBreak": 4 }
            }
            """;

    @TempDir
    Path tmp;

    Path json() throws IOException {
        Path f = tmp.resolve("share/planificador_datos.json");
        Files.createDirectories(f.getParent());
        Files.writeString(f, JSON, StandardCharsets.UTF_8);
        return f;
    }

    Path carpeta() {
        return tmp.resolve("Datos");
    }

    static int archivos(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return 0;
        try (var s = Files.list(dir)) {
            return (int) s.filter(p -> p.toString().endsWith(".json")).count();
        }
    }

    @Test
    void T11a_carpetaPorDefecto() {
        assertEquals(Path.of("/home/u/Documentos/Carpeta personal/Estudieitor/Datos"),
                DataManager.resolveDataFolder(Path.of("/home/u")));
    }

    @Test
    void T11a_migraElJsonALaCarpetaSinPerderNada() throws IOException {
        Path f = json();
        new DataManager(f, carpeta(), List.of());
        assertEquals(2, archivos(carpeta().resolve("materias")));
        assertEquals(2, archivos(carpeta().resolve("guia")));
        assertEquals(1, archivos(carpeta().resolve("fechas")));
        assertEquals(3, archivos(carpeta().resolve("sesiones")));
        assertTrue(Files.exists(carpeta().resolve("ajustes/pomodoro.json")));
        assertEquals(JSON, Files.readString(f, StandardCharsets.UTF_8));

        DataManager otra = new DataManager(tmp.resolve("otro/planificador_datos.json"), carpeta(), List.of());
        assertEquals(2, otra.getSubjects().size());
        assertEquals(2, otra.getStudySessions().size());
        assertEquals(1, otra.getImportantDates().size());
        assertEquals(100, otra.getStudyLogs().stream().mapToInt(StudyLog::getMinutes).sum());
        Subject m1 = otra.getSubjectById("m1");
        assertEquals(2, m1.getCourse().getEvaluations().size());
        assertEquals(3, m1.getCourse().getAttendance().size());
        assertEquals(30, otra.getPomodoroSettings().getWorkMinutes());
        assertNull(otra.getLoadWarning());
    }

    @Test
    void T11a_noMigraDosVeces() throws IOException {
        Path f = json();
        new DataManager(f, carpeta(), List.of());
        assertTrue(Files.exists(f.resolveSibling("migrado-a-carpeta")));
        for (String sub : List.of("materias", "guia", "fechas", "sesiones", "ajustes")) {
            try (var s = Files.list(carpeta().resolve(sub))) {
                for (Path p : s.toList()) Files.delete(p);
            }
        }
        DataManager dm = new DataManager(f, carpeta(), List.of());
        assertTrue(dm.getSubjects().isEmpty());
        assertTrue(dm.getStudyLogs().isEmpty());
    }

    @Test
    void T11a_jsonDanadoNoMigraYAvisa() throws IOException {
        Path f = tmp.resolve("share/planificador_datos.json");
        Files.createDirectories(f.getParent());
        Files.writeString(f, "{ esto no es json", StandardCharsets.UTF_8);
        java.io.PrintStream err = System.err;
        System.setErr(new java.io.PrintStream(java.io.OutputStream.nullOutputStream()));
        DataManager dm;
        try {
            dm = new DataManager(f, carpeta(), List.of());
        } finally {
            System.setErr(err);
        }
        assertNotNull(dm.getLoadWarning());
        assertFalse(Files.exists(f.resolveSibling("migrado-a-carpeta")));
        assertEquals(0, archivos(carpeta().resolve("materias")));
    }

    @Test
    void T11b_guardaEnLaCarpetaYRecarga() throws IOException {
        DataManager dm = new DataManager(json(), carpeta(), List.of());
        dm.addSubject(new Subject("m3", "Física", "#000000", "", ""));
        assertTrue(Files.exists(carpeta().resolve("materias/m3.json")));
        dm.removeSubject("m2");
        assertFalse(Files.exists(carpeta().resolve("materias/m2.json")));
        assertFalse(Files.exists(carpeta().resolve("guia/g2.json")));
        assertTrue(Files.exists(carpeta().resolve("sesiones/l3.json")));
    }

    @Test
    void T11b_recargarVeLoQueLlegoDeOtroDispositivo() throws IOException {
        DataManager dm = new DataManager(json(), carpeta(), List.of());
        AtomicInteger llamadas = new AtomicInteger();
        dm.setAlGuardar(llamadas::incrementAndGet);
        Files.writeString(carpeta().resolve("sesiones/x.json"),
                "{ \"id\": \"x\", \"subjectId\": \"m1\", \"date\": \"2026-11-08\", \"startTime\": \"09:00\", \"minutes\": 40 }");
        assertTrue(dm.recargar());
        assertEquals(4, dm.getStudyLogs().size());
        assertEquals(1, llamadas.get());
        assertFalse(dm.recargar());
        assertEquals(1, llamadas.get());
    }

    @Test
    void T11b_noPisaLoQueCambioEnOtroDispositivo() throws IOException {
        DataManager dm = new DataManager(json(), carpeta(), List.of());
        List<String> avisos = new ArrayList<>();
        dm.setSaveErrorListener(avisos::add);
        Path m1 = carpeta().resolve("materias/m1.json");
        Files.writeString(m1, Files.readString(m1).replace("Mecánica de Fluidos", "Editada en la tablet"));
        dm.getSubjectById("m1").setColor("#ffffff");
        dm.saveData();
        assertTrue(Files.readString(m1).contains("Editada en la tablet"));
        assertEquals(1, avisos.size());
        assertTrue(avisos.get(0).contains("otro dispositivo"));
        assertEquals("Editada en la tablet", dm.getSubjectById("m1").getName());
    }

    @Test
    void T11b_unArchivoIlegibleDejaElAviso() throws IOException {
        DataManager dm0 = new DataManager(json(), carpeta(), List.of());
        Files.writeString(carpeta().resolve("sesiones/roto.json"), "{no es json");
        DataManager dm = new DataManager(tmp.resolve("share/planificador_datos.json"), carpeta(), List.of());
        assertNotNull(dm.getLoadWarning());
        assertTrue(dm.getLoadWarning().contains("sesiones/roto.json"));
        assertNull(dm0.getLoadWarning());
    }

    @Test
    void T11b_cuentaLasCopiasDeConflicto() throws IOException {
        new DataManager(json(), carpeta(), List.of());
        Files.writeString(carpeta().resolve("materias/m1.sync-conflict-20261108-120000-ABCDEFG.json"), "{}");
        DataManager dm = new DataManager(tmp.resolve("share/planificador_datos.json"), carpeta(), List.of());
        assertEquals(1, dm.getConflictCount());
    }
    static java.io.PrintStream silenciar() {
        java.io.PrintStream err = System.err;
        System.setErr(new java.io.PrintStream(java.io.OutputStream.nullOutputStream()));
        return err;
    }

    @Test
    void unaMigracionFallidaSeDeshaceYSeTerminaAlRecargar() throws IOException {
        Path f = json();
        Files.createDirectories(carpeta());
        Files.writeString(carpeta().resolve("sesiones"), "un archivo donde va una carpeta");
        java.io.PrintStream err = silenciar();
        DataManager dm;
        try {
            dm = new DataManager(f, carpeta(), List.of());
            assertNotNull(dm.getLoadWarning());
            assertFalse(Files.exists(f.resolveSibling("migrado-a-carpeta")));
            assertEquals(0, archivos(carpeta().resolve("materias")));
            assertEquals(2, dm.getSubjects().size(), "los datos del JSON siguen en memoria");
            assertFalse(dm.recargar());
            assertEquals(2, dm.getSubjects().size(), "recargar con la carpeta rota no suelta los datos");
        } finally {
            System.setErr(err);
        }
        Files.delete(carpeta().resolve("sesiones"));
        dm.recargar();
        assertEquals(2, archivos(carpeta().resolve("materias")));
        assertEquals(3, archivos(carpeta().resolve("sesiones")));
        assertEquals(2, dm.getSubjects().size());
    }

    @Test
    void recargarSinCambiosConservaLosObjetosQueTienenLasPantallas() throws IOException {
        DataManager dm = new DataManager(json(), carpeta(), List.of());
        Subject enPantalla = dm.getSubjectById("m1");
        assertFalse(dm.recargar());
        enPantalla.setName("Química");
        dm.saveData();
        assertTrue(Files.readString(carpeta().resolve("materias/m1.json")).contains("Química"));
    }

    @Test
    void unGuardadoFallidoNoSePierdeAlRecargar() throws IOException {
        DataManager dm = new DataManager(json(), carpeta(), List.of());
        dm.setSaveErrorListener(m -> { });
        Path sesiones = carpeta().resolve("sesiones");
        var permisos = Files.getPosixFilePermissions(sesiones);
        Files.setPosixFilePermissions(sesiones, java.nio.file.attribute.PosixFilePermissions.fromString("r-xr-xr-x"));
        java.io.PrintStream err = silenciar();
        try {
            dm.addStudyLog(new StudyLog("l9", "m1", null, null, "2026-11-08", "10:00", 25));
            assertFalse(dm.recargar());
            assertEquals(4, dm.getStudyLogs().size(), "la sesión no guardada sigue en memoria");
        } finally {
            Files.setPosixFilePermissions(sesiones, permisos);
            System.setErr(err);
        }
        dm.recargar();
        assertTrue(Files.exists(sesiones.resolve("l9.json")));
        assertEquals(4, dm.getStudyLogs().size());
    }
    @Test
    void iniciarUsaEsaCarpeta() throws IOException {
        new DataManager(tmp.resolve("pc/planificador_datos.json"), carpeta(), List.of())
                .addSubject(new Subject("m1", "Física", "#000000", "", ""));
        DataManager.iniciar(tmp.resolve("local/planificador_datos.json"), carpeta());
        assertEquals(1, DataManager.getInstance().getSubjects().size());
    }
}
