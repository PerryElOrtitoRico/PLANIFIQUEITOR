<!-- contrato: 1,5 | archivos: src/main/java/com/planificador/service/DataManager.java -->
# T11a — DataManager: carpeta por defecto y migración

## Objetivo
Completar los `// TODO T11a` de `DataManager`: `resolveDataFolder`, `datos` y `migrarACarpeta`. No cambies nada más.

## Métodos (pasos en orden)
- `resolveDataFolder(Path home)`:
  1. `return home.resolve("Documentos").resolve("Carpeta personal").resolve("Estudieitor").resolve("Datos");`
- `datos()`:
  1. `return new CarpetaDatos.Datos(subjects, studySessions, importantDates, studyLogs, pomodoroSettings);`
- `migrarACarpeta()`:
```java
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
```
