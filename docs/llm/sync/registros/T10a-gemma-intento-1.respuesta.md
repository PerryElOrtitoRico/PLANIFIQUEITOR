```java
// src/main/java/com/planificador/service/CarpetaDatos.java
    public boolean vacia() throws IOException {
        for (String sub : List.of(SESIONES, MATERIAS, GUIA, FECHAS)) {
            if (!archivos(sub).isEmpty()) return false;
        }
        return !Files.exists(raiz.resolve(POMODORO));
    }

    static String nombreArchivo(String id) {
        return id.replaceAll("[^A-Za-z0-9._-]", "_") + ".json";
    }

    static boolean ignorado(String nombre) {
        return nombre.startsWith(".") || !nombre.endsWith(".json") || nombre.contains(".sync-conflict-");
    }

    Map<Path, String> deseado(Datos datos) {
        Map<Path, String> m = mapa();
        for (Subject s : datos.subjects()) m.put(Path.of(MATERIAS, nombreArchivo(s.getId())), json(s));
        for (StudySession s : datos.studySessions()) m.put(Path.of(GUIA, nombreArchivo(s.getId())), json(s));
        for (ImportantDate f : datos.importantDates()) m.put(Path.of(FECHAS, nombreArchivo(f.getId())), json(f));
        for (StudyLog l : datos.studyLogs()) m.put(Path.of(SESIONES, nombreArchivo(l.getId())), json(l));
        m.put(POMODORO, json(datos.pomodoroSettings()));
        return m;
    }
```

<!-- 22 s · entrada 3891 · salida 368 · finish_reason stop -->
