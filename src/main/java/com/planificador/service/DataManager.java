package com.planificador.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.planificador.model.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DataManager {
    private static final String FILE_PATH = "planificador_datos.json";
    private static DataManager instance;

    private List<Subject> subjects = new ArrayList<>();
    private List<StudySession> studySessions = new ArrayList<>();
    private List<ImportantDate> importantDates = new ArrayList<>();

    private DataManager() {
        loadData();
    }

    public static synchronized DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    public List<Subject> getSubjects() { return subjects; }
    public List<StudySession> getStudySessions() { return studySessions; }
    public List<ImportantDate> getImportantDates() { return importantDates; }

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
        try (Writer writer = new FileWriter(FILE_PATH)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            AppData data = new AppData(subjects, studySessions, importantDates);
            gson.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadData() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            loadInitialDemoData();
            saveData();
            return;
        }

        try (Reader reader = new FileReader(FILE_PATH)) {
            Gson gson = new Gson();
            AppData data = gson.fromJson(reader, AppData.class);
            if (data != null) {
                this.subjects = data.subjects != null ? data.subjects : new ArrayList<>();
                this.studySessions = data.studySessions != null ? data.studySessions : new ArrayList<>();
                this.importantDates = data.importantDates != null ? data.importantDates : new ArrayList<>();
            } else {
                loadInitialDemoData();
            }
        } catch (Exception e) {
            e.printStackTrace();
            loadInitialDemoData();
        }
    }

    public void loadInitialDemoData() {
        subjects.clear();
        studySessions.clear();
        importantDates.clear();

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

        AppData(List<Subject> subjects, List<StudySession> studySessions, List<ImportantDate> importantDates) {
            this.subjects = subjects;
            this.studySessions = studySessions;
            this.importantDates = importantDates;
        }
    }
}