```java
// src/main/java/com/planificador/service/DataManager.java
    static Path resolveDataFolder(Path home) {
        return home.resolve("Documentos").resolve("Carpeta personal").resolve("Estudieitor").resolve("Datos");
    }

    CarpetaDatos.Datos datos() {
        return new CarpetaDatos.Datos(subjects, studySessions, importantDates, studyLogs, pomodoroSettings);
    }

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
```

<!-- 31 s · entrada 4542 · salida 294 · finish_reason stop -->
