<!-- contrato: 1,5 | archivos: src/main/java/com/planificador/ui/CoursePanel.java, src/test/java/com/planificador/ui/CoursePanelTest.java -->
# T09b — Cursada: textos y tablas del panel

## Objetivo
Completar **solo** `statusLines`, `evaluationsModel`, `attendanceModel` y `attendanceFrom` de `CoursePanel` y los tests `textosYTablas` y `asistenciaConFechaMala`. El resto del panel ya está hecho.

## Código que ya existe
- `Subject`: `s.getCourse()` (`CourseTracking` o null). `CourseTracking`: `t.getRules()`, `t.getEvaluations()` (`List<Evaluation>`), `t.getAttendance()` (`List<AttendanceRecord>`); `new CourseTracking(rules, evaluations, attendance)`.
- `Evaluation`: `e.getKind()` (`Evaluation.PARCIAL` o `Evaluation.TP`), `e.getNumber()`, `e.isRetake()`, `e.getGrade()`, `e.getDate()` (puede ser null). `AttendanceRecord`: `r.getDate()`, `r.isPresent()`; `new AttendanceRecord(date, present)`.
- `CourseStatusCalculator.evaluate(tracking)` (`CourseStatus`), `CourseStatusCalculator.describe(status, rules)` (`List<String>`), `CourseStatusCalculator.formatGrade(nota)` ("7", "6,5").
- `StudyTimePanel.readOnlyModel(Object[] columnas)` devuelve un `DefaultTableModel` vacío que no se edita.

## Métodos (pasos en orden)
- `statusLines(s)`:
  1. `if (s.getCourse() == null) return List.of("Todavía no cargaste las reglas de cursada: tocá «Reglas de cursada».");`
  2. `CourseStatus st = CourseStatusCalculator.evaluate(s.getCourse());`
  3. `return CourseStatusCalculator.describe(st, s.getCourse().getRules());`
- `evaluationsModel(t)`:
  1. `DefaultTableModel m = StudyTimePanel.readOnlyModel(new Object[]{"Tipo", "N.º", "Recuperatorio", "Nota", "Fecha"});`
  2. `for (Evaluation e : t.getEvaluations()) {`
     - `boolean parcial = Evaluation.PARCIAL.equals(e.getKind());`
     - `m.addRow(new Object[]{parcial ? "Parcial" : "TP", e.getNumber(), parcial ? (e.isRetake() ? "Sí" : "No") : "—", CourseStatusCalculator.formatGrade(e.getGrade()), e.getDate() == null ? "" : e.getDate()});`
     - `}`
  3. `return m;`
- `attendanceModel(t)`:
  1. `DefaultTableModel m = StudyTimePanel.readOnlyModel(new Object[]{"Fecha", "Asistencia"});`
  2. `for (AttendanceRecord r : t.getAttendance()) m.addRow(new Object[]{r.getDate(), r.isPresent() ? "Presente" : "Ausente"});`
  3. `return m;`
- `attendanceFrom(dateText, present)`:
  1. `try { return new AttendanceRecord(LocalDate.parse(dateText.trim()).toString(), present); } catch (DateTimeParseException e) { throw new IllegalArgumentException("Fecha inválida: usá AAAA-MM-DD"); }`

## Tests (no necesitan `UiDatos`: arman los objetos a mano)
- `textosYTablas`:
  1. `Subject s = new Subject("s1", "Física II", "#000000", "", "");`
  2. `assertEquals(List.of("Todavía no cargaste las reglas de cursada: tocá «Reglas de cursada»."), CoursePanel.statusLines(s));`
  3. `CourseTracking t = s.getOrCreateCourse(); t.getEvaluations().add(new Evaluation("e1", Evaluation.PARCIAL, 1, false, 7, "2026-09-01")); t.getEvaluations().add(new Evaluation("e2", Evaluation.TP, 1, false, 8.5, null)); t.getAttendance().add(new AttendanceRecord("2026-08-10", false));`
  4. `assertEquals("Condición: En curso", CoursePanel.statusLines(s).get(0));`
  5. `var m = CoursePanel.evaluationsModel(t);` → `assertEquals("Parcial", m.getValueAt(0, 0));`, `assertEquals("No", m.getValueAt(0, 2));`, `assertEquals("8,5", m.getValueAt(1, 3));`, `assertEquals("", m.getValueAt(1, 4));`
  6. `assertEquals("Ausente", CoursePanel.attendanceModel(t).getValueAt(0, 1));`
- `asistenciaConFechaMala`: `assertEquals("2026-10-25", CoursePanel.attendanceFrom(" 2026-10-25 ", true).getDate());` y `assertThrows(IllegalArgumentException.class, () -> CoursePanel.attendanceFrom("25/10", true));`
