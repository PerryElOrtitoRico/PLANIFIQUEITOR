<!-- contrato: 1,5 | archivos: src/main/java/com/planificador/service/CourseStatusCalculator.java, src/test/java/com/planificador/service/CourseStatusCalculatorTest.java -->
# T04a — Cursada: asistencia

## Objetivo
Completar **solo** `attendance` de `CourseStatusCalculator` y los tests `asistenciaConClasesConocidas` y `asistenciaSinClasesConocidas`.

## Código que ya existe
- `AttendanceRecord` (clase con getters): `r.isPresent()`.
- `CourseRules` (clase con getters): `rules.getMinAttendancePercent()`, `rules.getPlannedClasses()`.
- `AttendanceSummary` es un record: `new AttendanceSummary(registered, present, absent, percent, failed, remainingKnown, remainingAbsences)`; se lee con `a.percent()`, `a.failed()`, `a.remainingAbsences()`.

## Método `attendance(rules, records)` (pasos en orden)
1. `int registered = records.size();`
2. `int present = 0; for (AttendanceRecord r : records) if (r.isPresent()) present++;`
3. `int absent = registered - present;`
4. `double percent = registered == 0 ? 100.0 : present * 100.0 / registered;`
5. `int planned = rules.getPlannedClasses();`
6. `if (planned > 0) { int allowed = (int) Math.floor(planned * (100.0 - rules.getMinAttendancePercent()) / 100.0 + 1e-9); int remaining = allowed - absent; return new AttendanceSummary(registered, present, absent, percent, remaining < 0, true, remaining); }`
7. `boolean failed = registered > 0 && percent < rules.getMinAttendancePercent();`
8. `return new AttendanceSummary(registered, present, absent, percent, failed, false, 0);`

## Tests (usan `reglas()` = 75 %, 16 clases, y `asistencias(presentes, ausentes)` que ya están en el test)
- `asistenciaConClasesConocidas`: `AttendanceSummary a = CourseStatusCalculator.attendance(reglas(), asistencias(8, 2));` → `assertEquals(80.0, a.percent(), 1e-9)`, `assertEquals(2, a.remainingAbsences())`, `assertFalse(a.failed())`; y con `asistencias(4, 5)`: `assertTrue(...failed())`.
- `asistenciaSinClasesConocidas`: `CourseRules r = new CourseRules(75, 6, 8, 2, List.of(true, true), 0);` → con `asistencias(3, 1)`: `assertFalse(a.failed())` y `assertFalse(a.remainingKnown())`; con `asistencias(2, 1)`: `assertTrue(...failed())`.
