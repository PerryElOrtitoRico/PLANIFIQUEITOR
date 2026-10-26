<!-- contrato: 1,4 | archivos: src/main/java/com/planificador/ui/StudyTimePanel.java, src/test/java/com/planificador/ui/StudyTimePanelTest.java -->
# T08 — Tiempo por materia: las dos tablas

## Objetivo
Completar **solo** `buildWeekModel` y `buildHistoryModel` de `StudyTimePanel` y los tests `tablaDeLaSemana` y `tablaDeLasUltimasSemanas`.

## Código que ya existe
- `DataManager.getInstance()`: `dm.getSubjects()` (`List<Subject>`), `dm.getStudyLogs()`, `dm.getStudySessions()`. `Subject`: `s.getId()`, `s.getName()`.
- `StudyTimeStats.compareWeek(subjectIds, logs, guide, weekStart)` (`List<SubjectWeekTime>`, en el orden de `subjectIds`), `StudyTimeStats.minutesBySubject(logs, weekStart)` (`Map<String, Integer>`), `StudyTimeStats.formatMinutes(min)` ("45 min", "2 h", "1 h 30 min").
- `SubjectWeekTime` es un record: `t.plannedMinutes()`, `t.actualMinutes()`.
- En la clase: `readOnlyModel(Object[] columnas)` devuelve un `DefaultTableModel` vacío con esas columnas, y `DIA` = formato `dd/MM`.

## Métodos (pasos en orden)
- `buildWeekModel(weekStart)`:
  1. `DataManager dm = DataManager.getInstance();`
  2. `DefaultTableModel m = readOnlyModel(new Object[]{"Materia", "Planeado", "Estudiado", "Avance"});`
  3. `List<String> ids = new ArrayList<>(); for (Subject s : dm.getSubjects()) ids.add(s.getId());`
  4. `List<SubjectWeekTime> times = StudyTimeStats.compareWeek(ids, dm.getStudyLogs(), dm.getStudySessions(), weekStart);`
  5. `for (int i = 0; i < times.size(); i++) {`
     - `SubjectWeekTime t = times.get(i);`
     - `String avance = t.plannedMinutes() > 0 ? (t.actualMinutes() * 100 / t.plannedMinutes()) + " %" : "—";`
     - `m.addRow(new Object[]{dm.getSubjects().get(i).getName(), StudyTimeStats.formatMinutes(t.plannedMinutes()), StudyTimeStats.formatMinutes(t.actualMinutes()), avance});`
     - `}`
  6. `return m;`
- `buildHistoryModel(lastWeekStart, weeks)`:
  1. `DataManager dm = DataManager.getInstance(); List<Subject> subjects = dm.getSubjects();`
  2. `Object[] cols = new Object[subjects.size() + 2]; cols[0] = "Semana"; for (int i = 0; i < subjects.size(); i++) cols[i + 1] = subjects.get(i).getName(); cols[cols.length - 1] = "Total";`
  3. `DefaultTableModel m = readOnlyModel(cols);`
  4. `for (int w = weeks - 1; w >= 0; w--) {`
     - `LocalDate ws = lastWeekStart.minusWeeks(w);`
     - `Map<String, Integer> mins = StudyTimeStats.minutesBySubject(dm.getStudyLogs(), ws);`
     - `Object[] row = new Object[cols.length]; row[0] = ws.format(DIA); int total = 0;`
     - `for (int i = 0; i < subjects.size(); i++) { int x = mins.getOrDefault(subjects.get(i).getId(), 0); total += x; row[i + 1] = StudyTimeStats.formatMinutes(x); }`
     - `row[cols.length - 1] = StudyTimeStats.formatMinutes(total);`
     - `m.addRow(row);`
     - `}`
  5. `return m;`

## Tests (cada uno empieza con `UiDatos.cargar(tmp);` y `StudyTimePanel p = new StudyTimePanel(null);`. En los datos: "Mecánica de Fluidos" tiene 120 min planeados y 50 estudiados la semana del 2026-10-19; "Física II" 90 planeados y 50 estudiados la semana del 2026-10-12)
- `tablaDeLaSemana`: `var m = p.buildWeekModel(LocalDate.of(2026, 10, 19));` → `assertEquals(2, m.getRowCount());`, `assertEquals("Mecánica de Fluidos", m.getValueAt(0, 0));`, `assertEquals("2 h", m.getValueAt(0, 1));`, `assertEquals("50 min", m.getValueAt(0, 2));`, `assertEquals("41 %", m.getValueAt(0, 3));`
- `tablaDeLasUltimasSemanas`: `var m = p.buildHistoryModel(LocalDate.of(2026, 10, 19), 2);` → `assertEquals(4, m.getColumnCount());`, `assertEquals("Total", m.getColumnName(3));`, `assertEquals("12/10", m.getValueAt(0, 0));`, `assertEquals("50 min", m.getValueAt(0, 2));`, `assertEquals("50 min", m.getValueAt(1, 3));`
