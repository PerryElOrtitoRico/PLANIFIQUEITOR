```java
// src/main/java/com/planificador/service/DataManager.java
    public String getLoadWarning() {
        if (avisoMigracion == null) return avisoCarpeta;
        if (avisoCarpeta == null) return avisoMigracion;
        return avisoMigracion + "\n\n" + avisoCarpeta;
    }

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

    public boolean recargar() {
        boolean cambio = cargarCarpeta();
        if (cambio) notificar();
        return cambio;
    }

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
```

<!-- 52 s · entrada 4953 · salida 538 · finish_reason stop -->
