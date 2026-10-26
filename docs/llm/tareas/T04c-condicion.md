<!-- contrato: 1,5 | archivos: src/main/java/com/planificador/service/CourseStatusCalculator.java, src/test/java/com/planificador/service/CourseStatusCalculatorTest.java -->
# T04c — Cursada: condición

## Objetivo
Completar **solo** `evaluate` de `CourseStatusCalculator` y los tests `condiciones` y `todaviaPuedePromocionar`. `attendance` y `exams` ya están hechos: usalos.

## Código que ya existe
- `CourseTracking` (clase con getters): `tracking.getRules()`, `tracking.getAttendance()`, `tracking.getEvaluations()`. Constructor `new CourseTracking(rules, evaluations, attendance)`.
- `CourseRules`: `rules.getPromotionGrade()`, `rules.getPlannedClasses()`.
- Records: `AttendanceSummary` (`a.failed()`, `a.registered()`), `ExamResult` (`e.state()`, `e.effective()` es `Double`, puede ser null), `CourseStatus(condition, attendance, exams, canPromote, canRegularize)`.

## Método `evaluate(tracking)` (pasos en orden)
1. `CourseRules rules = tracking.getRules();`
2. `AttendanceSummary a = attendance(rules, tracking.getAttendance());`
3. `List<ExamResult> exams = exams(rules, tracking.getEvaluations());`
4. `boolean anyFailed = false; boolean anyOpen = false; boolean allPromo = true; boolean promoPossible = true;`
5. `for (ExamResult e : exams) {`
   - `if (e.state() == ExamState.DESAPROBADO) anyFailed = true;`
   - `if (e.state() == ExamState.PENDIENTE || e.state() == ExamState.A_RECUPERAR) anyOpen = true;`
   - `if (e.effective() == null || e.effective() < rules.getPromotionGrade()) allPromo = false;`
   - `boolean ok = e.effective() == null || e.effective() >= rules.getPromotionGrade() || e.state() == ExamState.A_RECUPERAR;`
   - `if (!ok) promoPossible = false;`
   - `}`
6. `boolean attendanceOpen = rules.getPlannedClasses() > 0 && a.registered() < rules.getPlannedClasses();`
7. `CourseCondition c; if (a.failed() || anyFailed) c = CourseCondition.LIBRE; else if (anyOpen || attendanceOpen) c = CourseCondition.EN_CURSO; else if (rules.getPromotionGrade() > 0 && allPromo) c = CourseCondition.PROMOCION; else c = CourseCondition.REGULAR;`
8. `boolean canPromote = c != CourseCondition.LIBRE && rules.getPromotionGrade() > 0 && promoPossible;`
9. `return new CourseStatus(c, a, exams, canPromote, c != CourseCondition.LIBRE);`

## Tests (usan `reglas()` = 75 %, 6 regulariza, 8 promociona, 2 parciales, 16 clases, y `parcial`, `recuperatorio`, `asistencias` del test)
- `condiciones`, cada uno con `CourseStatusCalculator.evaluate(new CourseTracking(reglas(), notas, asistencias(p, a))).condition()`:
  - notas `List.of(parcial(1, 8), parcial(2, 9))`, `asistencias(14, 2)` → `PROMOCION`
  - notas `List.of(parcial(1, 7), parcial(2, 9))`, `asistencias(16, 0)` → `REGULAR`
  - notas `List.of(parcial(1, 8), parcial(2, 9))`, `asistencias(11, 5)` → `LIBRE`
  - notas `List.of(parcial(1, 8), parcial(2, 9))`, `asistencias(10, 0)` → `EN_CURSO`
- `todaviaPuedePromocionar`: notas `List.of(parcial(1, 8), parcial(2, 4))`, `asistencias(16, 0)` → `canPromote()` es true; notas `List.of(parcial(1, 6))`, `asistencias(5, 0)` → `canPromote()` es false y `canRegularize()` es true.
