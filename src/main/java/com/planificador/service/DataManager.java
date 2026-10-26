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
        return studyLogs;
    }

    public void addStudyLog(StudyLog log) {
        studyLogs.add(log);
        saveData();
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
        studyLogs.removeIf(l -> subjectId.equals(l.getSubjectId()));
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
        this.subjects = data.subjects != null ? data.subjects : new ArrayList<>();
        this.studySessions = data.studySessions != null ? data.studySessions : new ArrayList<>();
        this.importantDates = data.importantDates != null ? data.importantDates : new ArrayList<>();
        this.studyLogs = data.studyLogs != null ? data.studyLogs : new ArrayList<>();
        this.pomodoroSettings = data.pomodoroSettings != null ? data.pomodoroSettings : new PomodoroSettings();
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
        studyLogs.clear();
        pomodoroSettings = new PomodoroSettings();

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