```java
// src/main/java/com/planificador/service/DataManager.java
    /** Texto de una nota: recortado; vacío o null → null. */
    public static String limpiarNota(String texto) {
        if (texto == null) return null;
        String t = texto.strip();
        return t.isEmpty() ? null : t;
    }

    /** Cambia la nota de la sesión con ese id (limpiada con limpiarNota) y guarda. Si no existe, no hace nada. */
    public void setStudyLogNote(String logId, String note) {
        for (StudyLog l : studyLogs) {
            if (l.getId().equals(logId)) {
                l.setNote(limpiarNota(note));
                saveData();
                return;
            }
        }
    }
```

<!-- 14 s · entrada 5099 · salida 195 · finish_reason stop -->
