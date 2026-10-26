<!-- contrato: 1,5 | archivos: src/main/java/com/planificador/service/CourseStatusCalculator.java, src/test/java/com/planificador/service/CourseStatusCalculatorTest.java -->
# T04d — Cursada: textos para mostrar

## Objetivo
Completar **solo** `describe` y `formatGrade` de `CourseStatusCalculator` y el test `textos`. Los demás métodos ya están hechos.

## Código que ya existe
- Records: `CourseStatus` (`s.condition()`, `s.attendance()`, `s.exams()`, `s.canPromote()`), `AttendanceSummary` (`a.remainingKnown()`, `a.remainingAbsences()`, `a.percent()`), `ExamResult` (`e.number()`, `e.state()`, `e.effective()` es `Double`).
- `CourseRules`: `rules.getPassGrade()`, `rules.getPromotionGrade()`, `rules.getMinAttendancePercent()`.
- En la clase ya está `AR = Locale.forLanguageTag("es-AR")` (pone coma decimal).

## Métodos
- `formatGrade(grade)`:
  1. `if (grade == Math.rint(grade)) return String.valueOf((long) grade);`
  2. `return String.format(AR, "%.1f", grade);`
- `describe(status, rules)`, pasos en orden:
  1. `List<String> out = new ArrayList<>();`
  2. El nombre de la condición, exactamente así:
```java
String nombre = switch (status.condition()) {
    case PROMOCION -> "Promoción";
    case REGULAR -> "Regular";
    case LIBRE -> "Libre";
    case EN_CURSO -> "En curso";
};
out.add("Condición: " + nombre);
```
  3. `AttendanceSummary a = status.attendance();`
  4. Exactamente así (`remainingKnown()` es boolean; el número es `remainingAbsences()`):
```java
if (a.remainingKnown()) {
    int quedan = a.remainingAbsences();
    if (quedan > 1) out.add("Podés faltar " + quedan + " clases más");
    else if (quedan == 1) out.add("Podés faltar 1 clase más");
    else if (quedan == 0) out.add("No podés faltar más");
    else out.add("Superaste el límite de faltas por " + (-quedan));
} else {
    out.add("Asistencia: " + String.format(AR, "%.1f", a.percent()) + " % (mínimo " + formatGrade(rules.getMinAttendancePercent()) + " %)");
}
```
  5. `String pass = formatGrade(rules.getPassGrade()); String promo = formatGrade(rules.getPromotionGrade());`
  6. Por cada `ExamResult e : status.exams()`, con `String p = "Parcial " + e.number() + ": ";`
     - `PENDIENTE` → `p + "falta rendir (necesitás " + pass + " para regularizar" + (rules.getPromotionGrade() > 0 ? " y " + promo + " para promocionar" : "") + ")"`
     - `A_RECUPERAR` → `p + formatGrade(e.effective()) + ", tenés que recuperarlo (necesitás " + pass + ")"`
     - `APROBADO` → `p + formatGrade(e.effective()) + " (aprobado)"`
     - `DESAPROBADO` → `p + formatGrade(e.effective()) + " (desaprobado)"`
  7. `if (status.condition() == CourseCondition.EN_CURSO && rules.getPromotionGrade() > 0) out.add(status.canPromote() ? "Todavía podés promocionar" : "Ya no podés promocionar; todavía podés regularizar");`
  8. `return out;`

## Test `textos` (usa `reglas()` del test: 75 %, 6 regulariza, 8 promociona)
```java
CourseStatus s = new CourseStatus(CourseCondition.EN_CURSO, new AttendanceSummary(10, 8, 2, 80.0, false, true, 2),
        List.of(new ExamResult(1, 6.5, null, 6.5, ExamState.APROBADO)), false, true);
```
→ `assertEquals(List.of("Condición: En curso", "Podés faltar 2 clases más", "Parcial 1: 6,5 (aprobado)", "Ya no podés promocionar; todavía podés regularizar"), CourseStatusCalculator.describe(s, reglas()));` y `assertEquals("7", CourseStatusCalculator.formatGrade(7));`
