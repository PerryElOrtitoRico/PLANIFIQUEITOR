```java
// src/main/java/com/planificador/service/DataManager.java
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

    public void removeSubject(String subjectId) {
        subjects.removeIf(s -> s.getId().equals(subjectId));
        studySessions.removeIf(sess -> sess.getSubjectId().equals(subjectId));
        importantDates.removeIf(d -> d.getSubjectId().equals(subjectId));
        studyLogs.removeIf(l -> subjectId.equals(l.getSubjectId()));
        saveData();
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

    /** Copia a los campos lo leído del JSON. Un campo que falta (JSON viejo) queda vacío o con su valor por defecto. */
    private void applyLoaded(AppData data) {
        this.subjects = data.subjects != null ? data.subjects : new ArrayList<>();
        this.studySessions = data.studySessions != null ? data.studySessions : new ArrayList<>();
        this.importantDates = data.importantDates != null ? data.importantDates : new ArrayList<>();
        this.studyLogs = data.studyLogs != null ? data.studyLogs : new ArrayList<>();
        this.pomodoroSettings = data.pomodoroSettings != null ? data.pomodoroSettings : new PomodoroSettings();
    }
```

```java
// src/test/java/com/planificador/service/DataManagerTest.java
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
        dm.addStudyLog(new StudyLog("l1", "s1", null, null, "2026-10-25", "10:00", 25));
        dm.setPomodoroSettings(new PomodoroSettings(50, 10, 20, 3));
        Subject s = dm.getSubjects().get(0); s.getOrCreateCourse().getEvaluations().add(new Evaluation("e1", Evaluation.PARCIAL, 1, false, 7.5, null)); dm.saveData();
        DataManager otra = new DataManager(archivo, List.of());
        assertEquals(1, otra.getStudyLogs().size());
        assertEquals(50, otra.getPomodoroSettings().getWorkMinutes());
        assertEquals(7.5, otra.getSubjects().get(0).getCourse().getEvaluations().get(0).getGrade(), 1e-9);
    }
```

<!-- 142 s · entrada 5814 · salida 1317 · finish_reason stop -->
