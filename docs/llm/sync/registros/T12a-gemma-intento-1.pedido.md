<!-- modelo google/gemma-4-e4b-qat, temperatura 0.2, max_tokens 2359, razonamiento no -->

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

# ESTUDIEITOR, etapa carpeta sincronizada — Especificación (contrato)

## 1. Reglas de código
- Java 21, Maven, Gson. Paquete base `com.planificador`: `model` (datos), `service` (lógica y persistencia), `ui` (Swing). No toques `pom.xml` ni agregues dependencias.
- Nombres de clases y métodos como el código que ya existe; comentarios en español.
- Las clases de `model` tienen getters: `log.getId()`, `s.getName()`. Los **records** se leen con `x()`: `datos.subjects()`, `lectura.cambio()`. No existen `getSubjects()` en los records.
- Tests con JUnit 5 (`org.junit.jupiter.api.*`); archivos con `@TempDir`.

## 6. Nota de sesión
- `StudyLog` tiene `getNote()` / `setNote(String)`; null = sin nota. Las sesiones viejas no tienen nota.
- La nota se limpia siempre con `DataManager.limpiarNota(texto)`: recortada; vacía o null → null.
- Interfaz: `PomodoroPanel.noteField` y `TimerDialog.noteField` (`JTextField`); el Historial muestra la columna "Nota".

---

# T12a — DataManager: nota de una sesión

## Objetivo
Completar los `// TODO T12a` de `DataManager`: `limpiarNota` y `setStudyLogNote`. No cambies nada más.

## Métodos (pasos en orden)
- `limpiarNota(String texto)`:
  1. `if (texto == null) return null;`
  2. `String t = texto.strip();`
  3. `return t.isEmpty() ? null : t;`
- `setStudyLogNote(String logId, String note)`:
```java
for (StudyLog l : studyLogs) {
    if (l.getId().equals(logId)) {
        l.setNote(limpiarNota(note));
        saveData();
        return;
    }
}
```

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

    private final Path dataFile;                     // JSON viejo (origen de la migración) y ancla local de los archivos del Panel
    private final CarpetaDatos carpeta;              // carpeta sincronizada por Syncthing: un archivo por entidad
    private String avisoMigracion;                   // no null si el JSON viejo estaba dañado o no se pudo migrar
    private String avisoCarpeta;                     // no null si hay archivos ilegibles en la carpeta
    private int conflictos;                          // copias *.sync-conflict-* de la última lectura
    private List<Subject> subjects = new ArrayList<>();
    private List<StudySession> studySessions = new ArrayList<>();
    private List<ImportantDate> importantDates = new ArrayList<>();
    private List<StudyLog> studyLogs = new ArrayList<>();
    private PomodoroSettings pomodoroSettings = new PomodoroSettings();
    private String loadWarning;                      // lo pone loadData() si el JSON viejo no se pudo leer
    private java.util.function.Consumer<String> saveErrorListener;
    private volatile Runnable alGuardar;             // se llama tras cada guardado exitoso; null = ninguno

    /**
     * @param dataFile    archivo donde se guardan y leen los datos
     * @param legacyFiles ubicaciones viejas: si dataFile no existe, se copia la modificada más recientemente
     */
    DataManager(Path dataFile, List<Path> legacyFiles) {
        this(dataFile, dataFile.getParent().resolve("sync"), legacyFiles);
    }

    /**
     * @param dataFile    JSON viejo: se migra una sola vez a la carpeta; junto a él quedan los archivos del Panel
     * @param carpeta     carpeta sincronizada (un archivo por entidad)
     * @param legacyFiles ubicaciones aún más viejas del JSON
     */
    DataManager(Path dataFile, Path carpeta, List<Path> legacyFiles) {
        this.dataFile = dataFile;
        this.carpeta = new CarpetaDatos(carpeta);
        migrateLegacyFile(legacyFiles);
        migrarACarpeta();
        cargarCarpeta();
    }

    public static synchronized DataManager getInstance() {
        if (instance == null) {
            Path home = Path.of(System.getProperty("user.home"));
            // Antes los datos se guardaban en la carpeta desde donde se abría la app.
            List<Path> legacy = List.of(
                    Path.of(System.getProperty("user.dir")).resolve(FILE_NAME),
                    home.resolve(FILE_NAME));
            instance = new DataManager(resolveDataFile(System.getenv(), home), resolveDataFolder(home), legacy);
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

    /** Carpeta sincronizada: ~/Documentos/Carpeta personal/Estudieitor/Datos. */
    static Path resolveDataFolder(Path home) {
        return home.resolve("Documentos").resolve("Carpeta personal").resolve("Estudieitor").resolve("Datos");
    }

    public Path getDataFile() { return dataFile; }

    public Path getDataFolder() { return carpeta.raiz(); }

    /** Copias de conflicto de Syncthing en la última lectura de la carpeta. */
    public int getConflictCount() { return conflictos; }

    /** Aviso para mostrar al usuario si hay datos que no se pudieron leer; null si todo se leyó bien. */
    public String getLoadWarning() {
        if (avisoMigracion == null) return avisoCarpeta;
        if (avisoCarpeta == null) return avisoMigracion;
        return avisoMigracion + "\n\n" + avisoCarpeta;
    }

    /** Los datos en memoria, para CarpetaDatos. */
    CarpetaDatos.Datos datos() {
        return new CarpetaDatos.Datos(subjects, studySessions, importantDates, studyLogs, pomodoroSettings);
    }

    /**
     * Pasa el JSON viejo a la carpeta, una sola vez: solo si no existe la marca "migrado-a-carpeta" junto a
     * dataFile, la carpeta está vacía y el JSON existe y se lee bien. El JSON no se borra.
     */
    void migrarACarpeta() {
        Path marca = dataFile.resolveSibling("migrado-a-carpeta");
        try {
            if (Files.exists(marca) || !carpeta.vacia() || !Files.exists(dataFile)) return;
            loadData();
            if (loadWarning != null) {
                avisoMigracion = loadWarning;
                return;
            }
            carpeta.escribir(datos());
            Files.writeString(marca, LocalDate.now().toString(), StandardCharsets.UTF_8);
            System.err.println("Datos migrados de " + dataFile + " a " + carpeta.raiz());
        } catch (IOException e) {
            e.printStackTrace();
            avisoMigracion = "No se pudieron pasar los datos de " + dataFile + " a " + carpeta.raiz() + ":\n" + e.getMessage();
        }
    }

    /** Lee la carpeta y reemplaza los datos en memoria. Devuelve si cambió algo desde la lectura o escritura anterior. */
    private boolean cargarCarpeta() {
        try {
            CarpetaDatos.Lectura l = carpeta.leer();
            subjects = l.datos().subjects();
            studySessions = l.datos().studySessions();
            importantDates = l.datos().importantDates();
            studyLogs = l.datos().studyLogs();
            pomodoroSettings = l.datos().pomodoroSettings();
            conflictos = l.conflictos();
            avisoCarpeta = l.ilegibles().isEmpty() ? null
                    : "Estos archivos de " + carpeta.raiz() + " no se pudieron leer y no se van a tocar:\n"
                      + String.join("\n", l.ilegibles());
            return l.cambio();
        } catch (IOException e) {
            e.printStackTrace();
            avisoCarpeta = "No se pudo leer la carpeta de datos " + carpeta.raiz() + ":\n" + e.getMessage();
            return false;
        }
    }

    /** Relee la carpeta (lo que trajo Syncthing). Si cambió algo, avisa al oyente alGuardar y devuelve true. */
    public boolean recargar() {
        boolean cambio = cargarCarpeta();
        if (cambio) notificar();
        return cambio;
    }

    /** Llama al oyente alGuardar; si falla, no rompe nada. */
    private void notificar() {
        Runnable oyente = alGuardar;
        if (oyente != null) {
            try {
                oyente.run();
            } catch (RuntimeException e) {
                // un oyente que falla nunca rompe el guardado
            }
        }
    }

    /** Quién se entera cuando no se puede guardar (la interfaz muestra un diálogo). */
    public void setSaveErrorListener(java.util.function.Consumer<String> listener) { this.saveErrorListener = listener; }

    /** Un único oyente que se avisa después de cada guardado exitoso (nunca si falló). null lo quita. */
    public void setAlGuardar(Runnable oyente) { this.alGuardar = oyente; }

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
        return studyLogs;
    }

    public void addStudyLog(StudyLog log) {
        studyLogs.add(log);
        saveData();
    }

    /** Texto de una nota: recortado; vacío o null → null. */
    public static String limpiarNota(String texto) {
        // TODO T12a
        throw new UnsupportedOperationException();
    }

    /** Cambia la nota de la sesión con ese id (limpiada con limpiarNota) y guarda. Si no existe, no hace nada. */
    public void setStudyLogNote(String logId, String note) {
        // TODO T12a
        throw new UnsupportedOperationException();
    }

    /** Ajustes del Pomodoro. Nunca null. */
    public PomodoroSettings getPomodoroSettings() {
        return pomodoroSettings;
    }

    public void setPomodoroSettings(PomodoroSettings settings) {
        this.pomodoroSettings = settings;
        saveData();
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
        saveData();
    }

    /**
     * Marca o desmarca una sesión de la guía. Al marcarla queda en el historial (calendario) con la fecha de hoy;
     * al desmarcarla, se quita la entrada de hoy.
     */
    public void setSessionCompleted(StudySession session, boolean done) {
        session.setCompleted(done);
        LocalDate today = LocalDate.now();
        String logId = "guia_" + session.getId() + "_" + today;
        studyLogs.removeIf(l -> logId.equals(l.getId()));
        // Si ya se cronometró esta sesión hoy, esos minutos ya están en el historial: no se duplican.
        String cronoPrefix = "cron_" + session.getId() + "_" + today + "_";
        boolean cronometrada = studyLogs.stream().anyMatch(l -> l.getId().startsWith(cronoPrefix));
        if (done && !cronometrada) {
            StudyLog log = new StudyLog(logId, session.getSubjectId(), null, session.getTopicDescription(),
                    today.toString(), null, session.getDurationMinutes());
            log.setSource("guia");
            studyLogs.add(log);
        }
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

    /** Escribe en la carpeta solo lo que cambió; lo que cambió en otro dispositivo no se pisa y se recarga. */
    public void saveData() {
        try {
            CarpetaDatos.Escritura e = carpeta.escribir(datos());
            if (!e.noPisados().isEmpty()) {
                cargarCarpeta();
                if (saveErrorListener != null) {
                    saveErrorListener.accept("Algunos datos cambiaron en otro dispositivo mientras la app estaba abierta y no se pisaron ("
                            + e.noPisados().size() + "). Se cargó la versión más nueva; repetí el cambio si hacía falta.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (saveErrorListener != null) {
                saveErrorListener.accept("No se pudieron guardar los datos en " + carpeta.raiz() + ":\n" + e.getMessage());
            }
            return;
        }
        notificar();
    }

    public void loadData() {
        if (!Files.exists(dataFile)) {
            clearAll(); // primera vez: arranca vacío
            return;
        }

        try (Reader reader = Files.newBufferedReader(dataFile, StandardCharsets.UTF_8)) {
            Gson gson = new Gson();
            AppData data = gson.fromJson(reader, AppData.class);
            if (data != null) {
                applyLoaded(data);
            } else {
                clearAll();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Path backup = backupDamagedFile();
            clearAll();
            loadWarning = "No se pudo leer el archivo de datos (" + dataFile + ").\n"
                    + (backup != null ? "Se guardó una copia en:\n" + backup + "\n" : "No se pudo guardar una copia.\n")
                    + "El programa arranca vacío; al guardar algo nuevo se reemplaza el archivo.";
        }
    }

    private void clearAll() {
        subjects = new ArrayList<>();
        studySessions = new ArrayList<>();
        importantDates = new ArrayList<>();
        studyLogs = new ArrayList<>();
        pomodoroSettings = new PomodoroSettings();
    }

    /** Copia a los campos lo leído del JSON. Un campo que falta (JSON viejo) queda vacío o con su valor por defecto. */
    private void applyLoaded(AppData data) {
        this.subjects = data.subjects != null ? data.subjects : new ArrayList<>();
        this.studySessions = data.studySessions != null ? data.studySessions : new ArrayList<>();
        this.importantDates = data.importantDates != null ? data.importantDates : new ArrayList<>();
        this.studyLogs = data.studyLogs != null ? data.studyLogs : new ArrayList<>();
        this.pomodoroSettings = data.pomodoroSettings != null ? data.pomodoroSettings : new PomodoroSettings();
    }

    /** Guarda una copia del archivo ilegible antes de que el próximo guardado lo pise. */
    private Path backupDamagedFile() {
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path backup = dataFile.resolveSibling(FILE_NAME + ".danado-" + stamp);
        try {
            Files.copy(dataFile, backup, StandardCopyOption.REPLACE_EXISTING);
            System.err.println("El archivo de datos no se pudo leer; copia guardada en " + backup);
            return backup;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
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

Completá los `// TODO` que pide la tarea T12a. Devolvé **solo los métodos que completaste**, cada uno entero (firma y cuerpo), sin el resto de la clase: un bloque por archivo, con la ruta en la primera línea y después los métodos (y los imports nuevos, si agregaste). Archivos: src/main/java/com/planificador/service/DataManager.java.
