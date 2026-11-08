```java
// src/main/java/com/planificador/service/CarpetaDatos.java
    public Lectura leer() throws IOException {
        Map<Path, String> nuevo = mapa();
        List<String> ilegibles = new ArrayList<>();
        List<Subject> materias = new ArrayList<>();
        List<StudySession> guia = new ArrayList<>();
        List<ImportantDate> fechas = new ArrayList<>();
        List<StudyLog> sesiones = new ArrayList<>();
        int conflictos = 0;

        for (String sub : List.of(SESIONES, MATERIAS, GUIA, FECHAS)) {
            conflictos += conflictos(sub);
            for (Path rel : archivos(sub)) {
                try {
                    Object o = parsear(rel);
                    nuevo.put(rel, json(o));
                    if (o instanceof StudyLog l) sesiones.add(l);
                    else if (o instanceof Subject s) materias.add(s);
                    else if (o instanceof StudySession s) guia.add(s);
                    else if (o instanceof ImportantDate f) fechas.add(f);
                } catch (JsonParseException e) {
                    ilegibles.add(texto(rel));
                }
            }
        }

        PomodoroSettings ajustes = new PomodoroSettings();
        if (Files.exists(raiz.resolve(POMODORO))) {
            try {
                ajustes = (PomodoroSettings) parsear(POMODORO);
                nuevo.put(POMODORO, json(ajustes));
            } catch (JsonParseException e) {
                ilegibles.add(texto(POMODORO));
            }
        }

        boolean cambio = !nuevo.equals(conocido());
        setConocido(nuevo);
        return new Lectura(new Datos(materias, guia, fechas, sesiones, ajustes), conflictos, ilegibles, cambio);
    }
```

<!-- 38 s · entrada 3909 · salida 404 · finish_reason stop -->
