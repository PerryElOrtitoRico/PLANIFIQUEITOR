<!-- contrato: 1,2,3 | archivos: src/main/java/com/planificador/service/CarpetaDatos.java -->
# T10a — CarpetaDatos: nombres de archivo y mapa deseado

## Objetivo
Completar los `// TODO T10a` de `CarpetaDatos`: `nombreArchivo`, `ignorado`, `deseado` y `vacia`. No cambies nada más.

## Métodos (pasos en orden)
- `nombreArchivo(String id)`:
  1. `return id.replaceAll("[^A-Za-z0-9._-]", "_") + ".json";`
- `ignorado(String nombre)`:
  1. `return nombre.startsWith(".") || !nombre.endsWith(".json") || nombre.contains(".sync-conflict-");`
- `deseado(Datos datos)`:
  1. `Map<Path, String> m = mapa();`
  2. `for (Subject s : datos.subjects()) m.put(Path.of(MATERIAS, nombreArchivo(s.getId())), json(s));`
  3. `for (StudySession s : datos.studySessions()) m.put(Path.of(GUIA, nombreArchivo(s.getId())), json(s));`
  4. `for (ImportantDate f : datos.importantDates()) m.put(Path.of(FECHAS, nombreArchivo(f.getId())), json(f));`
  5. `for (StudyLog l : datos.studyLogs()) m.put(Path.of(SESIONES, nombreArchivo(l.getId())), json(l));`
  6. `m.put(POMODORO, json(datos.pomodoroSettings()));`
  7. `return m;`
- `vacia()`:
  1. `for (String sub : List.of(SESIONES, MATERIAS, GUIA, FECHAS)) { if (!archivos(sub).isEmpty()) return false; }`
  2. `return !Files.exists(raiz.resolve(POMODORO));`
