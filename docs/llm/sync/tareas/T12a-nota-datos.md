<!-- contrato: 1,6 | archivos: src/main/java/com/planificador/service/DataManager.java -->
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
