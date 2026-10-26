<!-- contrato: 1,7 | archivos: src/main/java/com/planificador/service/DataManager.java, src/test/java/com/planificador/service/DataManagerTest.java -->
# T05 — DataManager: guardar y cargar los datos nuevos

## Objetivo
Completar los `// TODO T05` de `DataManager` (`getStudyLogs`, `addStudyLog`, `getPomodoroSettings`, `setPomodoroSettings`, `applyLoaded`, y una línea en `removeSubject` y en `loadInitialDemoData`) y los tests `cargaUnJsonViejoSinLosCamposNuevos` y `guardaYRecargaLoNuevo`. Todo lo demás de `DataManager` ya funciona: no lo cambies.

## Código que ya existe
- Campos `studyLogs` (`List<StudyLog>`) y `pomodoroSettings` (`PomodoroSettings`) en `DataManager` y en `AppData`; `saveData()` ya los guarda.
- `AppData` tiene `data.subjects`, `data.studySessions`, `data.importantDates`, `data.studyLogs`, `data.pomodoroSettings` (campos, sin getters; pueden ser null).
- En el test ya están `DATOS_EJEMPLO` (un JSON viejo, sin los campos nuevos) y `@TempDir Path tmp`. El constructor es `new DataManager(archivo, List.of())`.

## Métodos (pasos en orden)
- `getStudyLogs()`: `return studyLogs;`
- `addStudyLog(log)`: `studyLogs.add(log);` y `saveData();`
- `getPomodoroSettings()`: `return pomodoroSettings;`
- `setPomodoroSettings(settings)`: `this.pomodoroSettings = settings;` y `saveData();`
- `applyLoaded(data)`:
  1. `this.subjects = data.subjects != null ? data.subjects : new ArrayList<>();`
  2. `this.studySessions = data.studySessions != null ? data.studySessions : new ArrayList<>();`
  3. `this.importantDates = data.importantDates != null ? data.importantDates : new ArrayList<>();`
  4. `this.studyLogs = data.studyLogs != null ? data.studyLogs : new ArrayList<>();`
  5. `this.pomodoroSettings = data.pomodoroSettings != null ? data.pomodoroSettings : new PomodoroSettings();`
- En `removeSubject`, en el lugar del `// TODO T05`: `studyLogs.removeIf(l -> subjectId.equals(l.getSubjectId()));`
- En `loadInitialDemoData`, en el lugar del `// TODO T05`: `studyLogs.clear();` y `pomodoroSettings = new PomodoroSettings();`

## Tests
- `cargaUnJsonViejoSinLosCamposNuevos`:
  1. `Path archivo = tmp.resolve("viejo/planificador_datos.json"); Files.createDirectories(archivo.getParent()); Files.writeString(archivo, DATOS_EJEMPLO, StandardCharsets.UTF_8);`
  2. `DataManager dm = new DataManager(archivo, List.of());`
  3. `assertTrue(dm.getStudyLogs().isEmpty());` · `assertEquals(25, dm.getPomodoroSettings().getWorkMinutes());` · `assertNull(dm.getSubjects().get(0).getCourse());`
- `guardaYRecargaLoNuevo`:
  1. `Path archivo = tmp.resolve("nuevo/planificador_datos.json"); DataManager dm = new DataManager(archivo, List.of());`
  2. `dm.addStudyLog(new StudyLog("l1", "s1", null, null, "2026-10-25", "10:00", 25));`
  3. `dm.setPomodoroSettings(new PomodoroSettings(50, 10, 20, 3));`
  4. `Subject s = dm.getSubjects().get(0); s.getOrCreateCourse().getEvaluations().add(new Evaluation("e1", Evaluation.PARCIAL, 1, false, 7.5, null)); dm.saveData();`
  5. `DataManager otra = new DataManager(archivo, List.of());`
  6. `assertEquals(1, otra.getStudyLogs().size());` · `assertEquals(50, otra.getPomodoroSettings().getWorkMinutes());` · `assertEquals(7.5, otra.getSubjects().get(0).getCourse().getEvaluations().get(0).getGrade(), 1e-9);`
