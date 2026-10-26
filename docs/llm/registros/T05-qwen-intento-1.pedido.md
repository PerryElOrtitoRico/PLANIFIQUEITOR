<!-- modelo qwen/qwen3.5-9b, temperatura 0.2, max_tokens 1575, razonamiento no -->

## system

Sos un programador Java 17 cuidadoso. Te paso archivos con métodos marcados `// TODO`: completá solo los cuerpos que nombra la tarea; los métodos marcados "lo completa otra subtarea" quedan exactamente como están.
Reglas:
- No cambies package, imports que ya están (aunque todavía no se usen), nombres de clases, firmas ni métodos que ya están hechos. Podés agregar imports y métodos private.
- Las clases de model se leen con getters: log.getMinutes(), rules.getPassGrade(). Los records de service se leen con x(): status.condition(), a.percent(). No existen getCondition() ni getPercent().
- En los tests, copiá las aserciones de la tarea tal cual (mismos valores y fechas) y no escribas comentarios.
- No crees ni modifiques otros archivos.
- Devolvé solo los métodos que completaste, cada uno entero (firma y cuerpo), sin el resto de la clase: un bloque por archivo, con la ruta en la primera línea. Ejemplo:
```java
// src/main/java/com/planificador/service/Ejemplo.java
    public static int doble(int x) {
        return 2 * x;
    }
```
- Sin explicaciones fuera de los bloques.

## user

# Contrato del proyecto (extracto)

# PLANIFICADOR, etapa Pomodoro y cursada — Especificación (contrato)

## 1. Reglas de código
- Java 17 (el `pom.xml` compila con `source/target` 17), Maven. Paquete base `com.planificador`: `model` (datos), `service` (lógica y persistencia), `ui` (Swing). No toques `pom.xml` ni agregues dependencias.
- Nombres de clases y métodos en inglés, como el código que ya existe; comentarios en español.
- Las clases de `model` tienen getters: `log.getMinutes()`, `rules.getPassGrade()`, `ev.isRetake()`. Los **records** de `service` se leen con `x()`: `status.condition()`, `a.percent()`. No existen `getCondition()` ni `getPercent()`.
- La lógica no usa `LocalDate.now()`: la fecha de hoy se pasa por parámetro (así se testea).
- Tests con JUnit 5 (`org.junit.jupiter.api.*`); archivos con `@TempDir`. Ningún test lee ni escribe `~/.local/share/planificador-estudio`.

## 7. Datos (JSON)
- Mismo archivo de siempre. Campos nuevos, todos opcionales: `studyLogs` (lista), `pomodoroSettings` (objeto) en la raíz, y `course` en cada materia. Un JSON viejo, sin esos campos, carga igual (listas vacías y ajustes por defecto).

---

# T05 — DataManager: guardar y cargar los datos nuevos

## Objetivo
Completar los `// TODO T05` de `DataManager` (`getStudyLogs`, `addStudyLog`, `getPomodoroSettings`, `setPomodoroSettings`, `applyLoaded`, y una línea en `removeSubject` y en `loadInitialDemoData`) y los tests `cargaUnJsonViejoSinLosCamposNuevos` y `guardaYRecargaLoNuevo`. Todo lo demás de `DataManager` ya funciona: no lo cambies.

## Código que ya existe
- Campos `studyLogs` (`List<StudyLog>`) y `pomodoroSettings` (`PomodoroSettings`) en `DataManager` y en `AppData`; `saveData()` ya los guarda.
- `AppData` tiene `data.subjects`, `data.studySessions`, `data.importantDates`, `data.studyLogs`, `data.pomodoroSettings` (campos, sin getters; pueden ser null).
- En el test ya están `DATOS_EJEMPLO` (un JSON viejo, sin los campos nuevos) y `@TempDir Path tmp`. El constructor es `new DataManager(archivo, List.of())`.

## Métodos (pasos en orden)
- `getStudyLogs()`: `return studyLogs;`
- `addStudyLog(log)`: `studyLogs.add(log);` y `saveData();`
- `getPomodoroSettings()`: `return pomodoroSettings;`
- `setPomodoroSettings(settings)`: `this.pomodoroSettings = settings;` y `saveData();`
- `applyLoaded(data)`:
  1. `this.subjects = data.subjects != null ? data.subjects : new ArrayList<>();`
  2. `this.studySessions = data.studySessions != null ? data.studySessions : new ArrayList<>();`
  3. `this.importantDates = data.importantDates != null ? data.importantDates : new ArrayList<>();`
  4. `this.studyLogs = data.studyLogs != null ? data.studyLogs : new ArrayList<>();`
  5. `this.pomodoroSettings = data.pomodoroSettings != null ? data.pomodoroSettings : new PomodoroSettings();`
- En `removeSubject`, en el lugar del `// TODO T05`: `studyLogs.removeIf(l -> subjectId.equals(l.getSubjectId()));`
- En `loadInitialDemoData`, en el lugar del `// TODO T05`: `studyLogs.clear();` y `pomodoroSettings = new PomodoroSettings();`

## Tests
- `cargaUnJsonViejoSinLosCamposNuevos`:
  1. `Path archivo = tmp.resolve("viejo/planificador_datos.json"); Files.createDirectories(archivo.getParent()); Files.writeString(archivo, DATOS_EJEMPLO, StandardCharsets.UTF_8);`
  2. `DataManager dm = new DataManager(archivo, List.of());`
  3. `assertTrue(dm.getStudyLogs().isEmpty());` · `assertEquals(25, dm.getPomodoroSettings().getWorkMinutes());` · `assertNull(dm.getSubjects().get(0).getCourse());`
- `guardaYRecargaLoNuevo`:
  1. `Path archivo = tmp.resolve("nuevo/planificador_datos.json"); DataManager dm = new DataManager(archivo, List.of());`
  2. `dm.addStudyLog(new StudyLog("l1", "s1", null, null, "2026-10-25", "10:00", 25));`
  3. `dm.setPomodoroSettings(new PomodoroSettings(50, 10, 20, 3));`
  4. `Subject s = dm.getSubjects().get(0); s.getOrCreateCourse().getEvaluations().add(new Evaluation("e1", Evaluation.PARCIAL, 1, false, 7.5, null)); dm.saveData();`
  5. `DataManager otra = new DataManager(archivo, List.of());`
  6. `assertEquals(1, otra.getStudyLogs().size());` · `assertEquals(50, otra.getPomodoroSettings().getWorkMinutes());` · `assertEquals(7.5, otra.getSubjects().get(0).getCourse().getEvaluations().get(0).getGrade(), 1e-9);`

---

# Archivos actuales

```java
// src/main/java/com/planificador/service/DataManager.java
package com.planificador.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.planificador.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DataManager {
    private static final String FILE_NAME = "planificador_datos.json";
    private static final String APP_DIR = "planificador-estudio";
    private static DataManager instance;

    private final Path dataFile;
    private List<Subject> subjects = new ArrayList<>();
    private List<StudySession> studySessions = new ArrayList<>();
    private List<ImportantDate> importantDates = new ArrayList<>();
    private List<StudyLog> studyLogs = new ArrayList<>();
    private PomodoroSettings pomodoroSettings = new PomodoroSettings();

    /**
     * @param dataFile    archivo donde se guardan y leen los datos
     * @param legacyFiles ubicaciones viejas: si dataFile no existe, se copia la modificada más recientemente
     */
    DataManager(Path dataFile, List<Path> legacyFiles) {
        this.dataFile = dataFile;
        migrateLegacyFile(legacyFiles);
        loadData();
    }

    public static synchronized DataManager getInstance() {
        if (instance == null) {
            Path home = Path.of(System.getProperty("user.home"));
            // Antes los datos se guardaban en la carpeta desde donde se abría la app.
            List<Path> legacy = List.of(
                    Path.of(System.getProperty("user.dir")).resolve(FILE_NAME),
                    home.resolve(FILE_NAME));
            instance = new DataManager(resolveDataFile(System.getenv(), home), legacy);
        }
        return instance;
    }

    /** Solo para tests: el singleton pasa a usar este archivo (así ningún test toca los datos reales). */
    public static synchronized void initForTests(Path dataFile) {
        instance = new DataManager(dataFile, List.of());
    }

    /** Ubicación fija según el estándar XDG: $XDG_DATA_HOME o ~/.local/share. */
    static Path resolveDataFile(Map<String, String> env, Path home) {
        String xdg = env.get("XDG_DATA_HOME");
        Path base = (xdg != null && !xdg.isBlank()) ? Path.of(xdg) : home.resolve(".local/share");
        return base.resolve(APP_DIR).resolve(FILE_NAME);
    }

    public Path getDataFile() { return dataFile; }

    private void migrateLegacyFile(List<Path> legacyFiles) {
        if (Files.exists(dataFile)) {
            return;
        }
        // Puede haber varias copias viejas (p. ej. una plantilla vacía en la carpeta del proyecto):
        // la más reciente es la que tiene los datos en uso.
        Path newest = null;
        for (Path legacy : legacyFiles) {
            if (Files.isRegularFile(legacy) && (newest == null || lastModified(legacy) > lastModified(newest))) {
                newest = legacy;
            }
        }
        if (newest == null) {
            return;
        }
        try {
            Files.createDirectories(dataFile.getParent());
            // Se copia (no se mueve): el archivo viejo queda como respaldo.
            Files.copy(newest, dataFile);
            System.err.println("Datos migrados de " + newest + " a " + dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static long lastModified(Path file) {
        try {
            return Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            return Long.MIN_VALUE;
        }
    }

    public List<Subject> getSubjects() { return subjects; }
    public List<StudySession> getStudySessions() { return studySessions; }
    public List<ImportantDate> getImportantDates() { return importantDates; }

    /** Sesiones de estudio hechas (Pomodoro). Nunca null. */
    public List<StudyLog> getStudyLogs() {
        // TODO T05
        throw new UnsupportedOperationException();
    }

    public void addStudyLog(StudyLog log) {
        // TODO T05
        throw new UnsupportedOperationException();
    }

    /** Ajustes del Pomodoro. Nunca null. */
    public PomodoroSettings getPomodoroSettings() {
        // TODO T05
        throw new UnsupportedOperationException();
    }

    public void setPomodoroSettings(PomodoroSettings settings) {
        // TODO T05
        throw new UnsupportedOperationException();
    }

    public Subject getSubjectById(String id) {
        return subjects.stream().filter(s -> s.getId().equals(id)).findFirst().orElse(null);
    }

    public void addSubject(Subject subject) {
        subjects.add(subject);
        saveData();
    }

    public void removeSubject(String subjectId) {
        subjects.removeIf(s -> s.getId().equals(subjectId));
        studySessions.removeIf(sess -> sess.getSubjectId().equals(subjectId));
        importantDates.removeIf(d -> d.getSubjectId().equals(subjectId));
        // TODO T05: borrar también los studyLogs de esa materia
        saveData();
    }

    public void addStudySession(StudySession session) {
        studySessions.add(session);
        saveData();
    }

    public void removeStudySession(String sessionId) {
        studySessions.removeIf(s -> s.getId().equals(sessionId));
        saveData();
    }

    public void addImportantDate(ImportantDate date) {
        importantDates.add(date);
        saveData();
    }

    public void removeImportantDate(String dateId) {
        importantDates.removeIf(d -> d.getId().equals(dateId));
        saveData();
    }

    public void saveData() {
        try {
            Files.createDirectories(dataFile.getParent());
            // Se escribe a un temporal y se reemplaza: si se corta a mitad, el archivo anterior queda entero.
            Path tmp = dataFile.resolveSibling(FILE_NAME + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                AppData data = new AppData(subjects, studySessions, importantDates, studyLogs, pomodoroSettings);
                gson.toJson(data, writer);
            }
            Files.move(tmp, dataFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadData() {
        if (!Files.exists(dataFile)) {
            loadInitialDemoData();
            saveData();
            return;
        }

        try (Reader reader = Files.newBufferedReader(dataFile, StandardCharsets.UTF_8)) {
            Gson gson = new Gson();
            AppData data = gson.fromJson(reader, AppData.class);
            if (data != null) {
                applyLoaded(data);
            } else {
                loadInitialDemoData();
            }
        } catch (Exception e) {
            e.printStackTrace();
            backupDamagedFile();
            loadInitialDemoData();
        }
    }

    /** Copia a los campos lo leído del JSON. Un campo que falta (JSON viejo) queda vacío o con su valor por defecto. */
    private void applyLoaded(AppData data) {
        // TODO T05
        throw new UnsupportedOperationException();
    }

    /** Guarda una copia del archivo ilegible antes de que el próximo guardado lo pise. */
    private void backupDamagedFile() {
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path backup = dataFile.resolveSibling(FILE_NAME + ".danado-" + stamp);
        try {
            Files.copy(dataFile, backup, StandardCopyOption.REPLACE_EXISTING);
            System.err.println("El archivo de datos no se pudo leer; copia guardada en " + backup);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadInitialDemoData() {
        subjects.clear();
        studySessions.clear();
        importantDates.clear();
        // TODO T05: vaciar también studyLogs y volver los ajustes del Pomodoro a los de por defecto

        Subject math = new Subject("s1", "Análisis Matemático II", "#4f46e5", "Ing. Pérez", "Lun y Mié 18:00 hs");
        Unit u1 = new Unit("u1", 1, "Derivadas Parciales y Integrales Múltiples");
        u1.addTopic(new Topic("t1", "Límites dobles y continuidad", true));
        u1.addTopic(new Topic("t2", "Derivadas direccionales y Gradiente", true));
        u1.addTopic(new Topic("t3", "Integrales dobles sobre regiones generales", false));
        math.getUnits().add(u1);

        Subject algo = new Subject("s2", "Algoritmos y Estructuras de Datos", "#0284c7", "Dra. Gómez", "Mar y Jue 14:00 hs");
        Unit u2 = new Unit("u2", 1, "Estructuras de Datos Avanzadas");
        u2.addTopic(new Topic("t4", "Árboles AVL y Árboles B", true));
        u2.addTopic(new Topic("t5", "Grafos: Dijkstra y BFS/DFS", false));
        algo.getUnits().add(u2);

        subjects.add(math);
        subjects.add(algo);

        studySessions.add(new StudySession("ss1", "lunes", "s1", "Repasar Integrales Dobles", 60, false));
        studySessions.add(new StudySession("ss2", "martes", "s2", "Práctica de Árboles AVL en Java", 45, true));
        studySessions.add(new StudySession("ss3", "miercoles", "s1", "Ejercicios de Gradientes", 90, false));

        LocalDate today = LocalDate.now();
        importantDates.add(new ImportantDate("d1", "Primer Parcial Análisis II", "s1", today.plusDays(5).toString(), "parcial", "Capítulos 1 al 4"));
        importantDates.add(new ImportantDate("d2", "Entrega TP Grafos", "s2", today.plusDays(12).toString(), "tp", "Subir en GitHub"));
    }

    private static class AppData {
        List<Subject> subjects;
        List<StudySession> studySessions;
        List<ImportantDate> importantDates;
        List<StudyLog> studyLogs;               // nuevo: puede faltar en un JSON viejo
        PomodoroSettings pomodoroSettings;      // nuevo: puede faltar en un JSON viejo

        AppData(List<Subject> subjects, List<StudySession> studySessions, List<ImportantDate> importantDates,
                List<StudyLog> studyLogs, PomodoroSettings pomodoroSettings) {
            this.subjects = subjects;
            this.studySessions = studySessions;
            this.importantDates = importantDates;
            this.studyLogs = studyLogs;
            this.pomodoroSettings = pomodoroSettings;
        }
    }
}
```

```java
// src/test/java/com/planificador/service/DataManagerTest.java
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

    @Test
    void cargaUnJsonViejoSinLosCamposNuevos() throws IOException {
        // TODO T05
    }

    @Test
    void guardaYRecargaLoNuevo() {
        // TODO T05
    }
}
```

Completá los `// TODO` que pide la tarea T05. Devolvé **solo los métodos que completaste**, cada uno entero (firma y cuerpo), sin el resto de la clase: un bloque por archivo, con la ruta en la primera línea y después los métodos (y los imports nuevos, si agregaste). Archivos: src/main/java/com/planificador/service/DataManager.java, src/test/java/com/planificador/service/DataManagerTest.java.
