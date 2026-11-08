package com.planificador.service;

import com.planificador.model.ImportantDate;
import com.planificador.model.PomodoroSettings;
import com.planificador.model.StudyLog;
import com.planificador.model.StudySession;
import com.planificador.model.Subject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** CarpetaDatos: un archivo por entidad, sin pisar lo que cambió en otro dispositivo. */
class CarpetaDatosTest {

    @TempDir
    Path raiz;

    static CarpetaDatos.Datos ejemplo() {
        List<Subject> materias = new ArrayList<>(List.of(new Subject("m1", "Mecánica de Fluidos", "#000000", "Prof", "Lun")));
        List<StudySession> guia = new ArrayList<>(List.of(new StudySession("g1", "lunes", "m1", "Bernoulli", 60, false)));
        List<ImportantDate> fechas = new ArrayList<>(List.of(new ImportantDate("f1", "Primer parcial", "m1", "2026-11-20", "parcial", null)));
        List<StudyLog> sesiones = new ArrayList<>(List.of(
                new StudyLog("l1", "m1", null, null, "2026-11-08", "10:00", 25),
                new StudyLog("l2", "m1", null, null, "2026-11-08", "10:30", 25)));
        return new CarpetaDatos.Datos(materias, guia, fechas, sesiones, new PomodoroSettings(30, 5, 15, 4));
    }

    @Test
    void T10a_nombreArchivo() {
        assertEquals("a_b_c.json", CarpetaDatos.nombreArchivo("a/b c"));
        assertEquals("pl_1-2.x.json", CarpetaDatos.nombreArchivo("pl_1-2.x"));
    }

    @Test
    void T10a_ignorado() {
        assertTrue(CarpetaDatos.ignorado("m1.sync-conflict-20261108-120000-ABCDEFG.json"));
        assertTrue(CarpetaDatos.ignorado("m1.json.tmp"));
        assertTrue(CarpetaDatos.ignorado(".syncthing.m1.json.tmp"));
        assertTrue(CarpetaDatos.ignorado(".m1.json"));
        assertFalse(CarpetaDatos.ignorado("m1.json"));
    }

    @Test
    void T10a_deseadoTieneUnaRutaPorEntidad() {
        var d = new CarpetaDatos(raiz).deseado(ejemplo());
        assertEquals(6, d.size());
        assertTrue(d.containsKey(Path.of("materias", "m1.json")));
        assertTrue(d.containsKey(Path.of("guia", "g1.json")));
        assertTrue(d.containsKey(Path.of("fechas", "f1.json")));
        assertTrue(d.containsKey(Path.of("sesiones", "l1.json")));
        assertTrue(d.containsKey(Path.of("sesiones", "l2.json")));
        assertTrue(d.containsKey(Path.of("ajustes", "pomodoro.json")));
        assertTrue(d.get(Path.of("materias", "m1.json")).contains("Mecánica de Fluidos"));
    }

    @Test
    void T10a_vacia() throws IOException {
        CarpetaDatos c = new CarpetaDatos(raiz);
        assertTrue(c.vacia());
        Files.createDirectories(raiz.resolve("sesiones"));
        Files.writeString(raiz.resolve("sesiones/x.json.tmp"), "{}");
        assertTrue(c.vacia());
        Files.writeString(raiz.resolve("sesiones/x.json"), "{}");
        assertFalse(c.vacia());
    }

    @Test
    void T10b_leeLoQueHayEnLaCarpeta() throws IOException {
        CarpetaDatos c = new CarpetaDatos(raiz);
        for (var e : c.deseado(ejemplo()).entrySet()) {
            Files.createDirectories(raiz.resolve(e.getKey()).getParent());
            Files.writeString(raiz.resolve(e.getKey()), e.getValue(), StandardCharsets.UTF_8);
        }
        CarpetaDatos.Lectura l = c.leer();
        assertEquals(1, l.datos().subjects().size());
        assertEquals("Mecánica de Fluidos", l.datos().subjects().get(0).getName());
        assertEquals(1, l.datos().studySessions().size());
        assertEquals(1, l.datos().importantDates().size());
        assertEquals(2, l.datos().studyLogs().size());
        assertEquals(30, l.datos().pomodoroSettings().getWorkMinutes());
        assertEquals(0, l.conflictos());
        assertTrue(l.ilegibles().isEmpty());
        assertTrue(l.cambio());
        assertFalse(c.leer().cambio());
    }

    @Test
    void T10b_carpetaVaciaDaListasVaciasYAjustesPorDefecto() throws IOException {
        CarpetaDatos.Lectura l = new CarpetaDatos(raiz).leer();
        assertTrue(l.datos().subjects().isEmpty());
        assertTrue(l.datos().studyLogs().isEmpty());
        assertEquals(25, l.datos().pomodoroSettings().getWorkMinutes());
        l.datos().studyLogs().add(new StudyLog("x", "m1", null, null, "2026-11-08", "10:00", 5)); // listas modificables
        assertFalse(l.cambio());
    }

    @Test
    void T10b_ignoraConflictosYTemporales() throws IOException {
        CarpetaDatos c = new CarpetaDatos(raiz);
        c.escribir(ejemplo());
        Files.writeString(raiz.resolve("materias/m1.sync-conflict-20261108-120000-ABCDEFG.json"), "{\"id\":\"m9\"}");
        Files.writeString(raiz.resolve("materias/m1.json.tmp"), "{\"id\":\"m8\"}");
        Files.writeString(raiz.resolve("materias/.syncthing.m1.json.tmp"), "{\"id\":\"m7\"}");
        CarpetaDatos.Lectura l = new CarpetaDatos(raiz).leer();
        assertEquals(1, l.datos().subjects().size());
        assertEquals(1, l.conflictos());
    }

    @Test
    void T10b_unArchivoIlegibleSeSaltea() throws IOException {
        Files.createDirectories(raiz.resolve("sesiones"));
        Files.writeString(raiz.resolve("sesiones/roto.json"), "{no es json");
        CarpetaDatos.Lectura l = new CarpetaDatos(raiz).leer();
        assertEquals(List.of("sesiones/roto.json"), l.ilegibles());
        assertTrue(l.datos().studyLogs().isEmpty());
    }

    @Test
    void T10c_guardaUnArchivoPorEntidadYLoVuelveALeer() throws IOException {
        CarpetaDatos.Escritura e = new CarpetaDatos(raiz).escribir(ejemplo());
        assertEquals(6, e.escritos());
        for (String f : List.of("materias/m1.json", "guia/g1.json", "fechas/f1.json", "sesiones/l1.json", "sesiones/l2.json", "ajustes/pomodoro.json")) {
            assertTrue(Files.exists(raiz.resolve(f)), f);
        }
        CarpetaDatos.Lectura l = new CarpetaDatos(raiz).leer();
        assertEquals(2, l.datos().studyLogs().size());
        assertEquals(30, l.datos().pomodoroSettings().getWorkMinutes());
    }

    @Test
    void T10c_soloEscribeLoQueCambio() throws IOException {
        CarpetaDatos c = new CarpetaDatos(raiz);
        CarpetaDatos.Datos d = ejemplo();
        c.escribir(d);
        assertEquals(0, c.escribir(d).escritos());
        d.subjects().get(0).setName("Fluidos");
        CarpetaDatos.Escritura e = c.escribir(d);
        assertEquals(1, e.escritos());
        assertTrue(e.noPisados().isEmpty());
    }

    @Test
    void T10c_borrarUnaEntidadBorraSuArchivo() throws IOException {
        CarpetaDatos c = new CarpetaDatos(raiz);
        CarpetaDatos.Datos d = ejemplo();
        c.escribir(d);
        d.studyLogs().remove(1);
        assertEquals(1, c.escribir(d).borrados());
        assertFalse(Files.exists(raiz.resolve("sesiones/l2.json")));
    }

    @Test
    void T10c_noPisaUnArchivoQueCambioEnOtroDispositivo() throws IOException {
        CarpetaDatos c = new CarpetaDatos(raiz);
        CarpetaDatos.Datos d = ejemplo();
        c.escribir(d);
        Path m1 = raiz.resolve("materias/m1.json");
        Files.writeString(m1, Files.readString(m1).replace("Mecánica de Fluidos", "Editada en la tablet"));
        d.subjects().get(0).setColor("#ffffff");
        CarpetaDatos.Escritura e = c.escribir(d);
        assertEquals(List.of("materias/m1.json"), e.noPisados());
        assertTrue(Files.readString(m1).contains("Editada en la tablet"));
    }

    @Test
    void T10c_noResucitaUnArchivoBorradoEnOtroDispositivo() throws IOException {
        CarpetaDatos c = new CarpetaDatos(raiz);
        CarpetaDatos.Datos d = ejemplo();
        c.escribir(d);
        Files.delete(raiz.resolve("fechas/f1.json"));
        d.subjects().get(0).setName("Otro");
        CarpetaDatos.Escritura e = c.escribir(d);
        assertFalse(Files.exists(raiz.resolve("fechas/f1.json")));
    }

    @Test
    void T10c_noBorraUnArchivoQueCambioEnOtroDispositivo() throws IOException {
        CarpetaDatos c = new CarpetaDatos(raiz);
        CarpetaDatos.Datos d = ejemplo();
        c.escribir(d);
        Path l2 = raiz.resolve("sesiones/l2.json");
        Files.writeString(l2, Files.readString(l2).replace("25", "40"));
        d.studyLogs().remove(1);
        CarpetaDatos.Escritura e = c.escribir(d);
        assertTrue(Files.exists(l2));
        assertEquals(List.of("sesiones/l2.json"), e.noPisados());
    }

    @Test
    void T10c_sumaSesionesNuevasQueLlegaron() throws IOException {
        CarpetaDatos c = new CarpetaDatos(raiz);
        c.escribir(ejemplo());
        Files.writeString(raiz.resolve("sesiones/l9.json"),
                Files.readString(raiz.resolve("sesiones/l1.json")).replace("\"l1\"", "\"l9\""));
        CarpetaDatos.Lectura l = c.leer();
        assertEquals(3, l.datos().studyLogs().size());
        assertTrue(l.cambio());
        assertFalse(c.leer().cambio());
    }

    @Test
    void T10c_unArchivoIlegibleNoSeTocaNiSeBorra() throws IOException {
        CarpetaDatos c = new CarpetaDatos(raiz);
        c.escribir(ejemplo());
        Files.writeString(raiz.resolve("sesiones/roto.json"), "{no es json");
        CarpetaDatos.Lectura l = c.leer();
        c.escribir(l.datos());
        assertEquals("{no es json", Files.readString(raiz.resolve("sesiones/roto.json")));
    }

    @Test
    void T10c_creaStignoreYRespetaElQueYaExiste() throws IOException {
        new CarpetaDatos(raiz).escribir(ejemplo());
        assertTrue(Files.readAllLines(raiz.resolve(".stignore")).contains("*.tmp"));
        Path otra = raiz.resolve("otra");
        Files.createDirectories(otra);
        Files.writeString(otra.resolve(".stignore"), "propio\n");
        new CarpetaDatos(otra).escribir(ejemplo());
        assertEquals("propio\n", Files.readString(otra.resolve(".stignore")));
    }
    @Test
    void unArchivoSinIdEsIlegibleYNoFrenaElGuardado() throws IOException {
        CarpetaDatos c = new CarpetaDatos(raiz);
        c.escribir(ejemplo());
        Files.writeString(raiz.resolve("sesiones/x.json"), "{\"subjectId\":\"m1\",\"minutes\":30}");
        CarpetaDatos.Lectura l = c.leer();
        assertEquals(List.of("sesiones/x.json"), l.ilegibles());
        assertEquals(2, l.datos().studyLogs().size());
        assertDoesNotThrow(() -> c.escribir(l.datos()));
    }

    @Test
    void unArchivoConNombreDistintoDelIdEsIlegibleYNoSeRenombra() throws IOException {
        CarpetaDatos c = new CarpetaDatos(raiz);
        c.escribir(ejemplo());
        String copia = Files.readString(raiz.resolve("sesiones/l1.json")).replace("25", "45");
        Files.writeString(raiz.resolve("sesiones/a.json"), copia);
        CarpetaDatos.Lectura l = c.leer();
        assertEquals(List.of("sesiones/a.json"), l.ilegibles());
        c.escribir(l.datos());
        assertEquals(copia, Files.readString(raiz.resolve("sesiones/a.json")));
        assertTrue(Files.readString(raiz.resolve("sesiones/l1.json")).contains("25"));
    }

    @Test
    void ajustesIlegiblesNoDanConflictoEnCadaGuardado() throws IOException {
        CarpetaDatos c = new CarpetaDatos(raiz);
        c.escribir(ejemplo());
        Files.writeString(raiz.resolve("ajustes/pomodoro.json"), "{no");
        CarpetaDatos.Lectura l = c.leer();
        assertTrue(c.escribir(l.datos()).noPisados().isEmpty());
        assertEquals("{no", Files.readString(raiz.resolve("ajustes/pomodoro.json")));
    }
}
