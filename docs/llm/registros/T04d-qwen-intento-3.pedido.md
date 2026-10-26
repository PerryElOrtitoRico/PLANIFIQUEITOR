<!-- modelo qwen/qwen3.5-9b, temperatura 0.2, max_tokens 4310, razonamiento no -->

## system

Sos un programador Java 17 cuidadoso. Te paso archivos con métodos marcados `// TODO`: completá solo los cuerpos que nombra la tarea; los métodos marcados "lo completa otra subtarea" quedan exactamente como están.
Reglas:
- No cambies package, imports que ya están (aunque todavía no se usen), nombres de clases, firmas ni métodos que ya están hechos. Podés agregar imports y métodos private.
- Las clases de model se leen con getters: log.getMinutes(), rules.getPassGrade(). Los records de service se leen con x(): status.condition(), a.percent(). No existen getCondition() ni getPercent().
- En los tests, copiá las aserciones de la tarea tal cual (mismos valores y fechas) y no escribas comentarios.
- No crees ni modifiques otros archivos.
- Devolvé cada archivo completo (nada de '...') en su propio bloque de código, con la ruta en la primera línea: un bloque por archivo, nunca dos archivos en el mismo bloque. Ejemplo:
```java
// src/main/java/com/planificador/service/Ejemplo.java
package com.planificador.service;

public final class Ejemplo { }
```
- Sin explicaciones fuera de los bloques.

## user

# Contrato del proyecto (extracto)

# PLANIFICADOR, etapa Pomodoro y cursada — Especificación (contrato)

## 1. Reglas de código
- Java 17 (el `pom.xml` compila con `source/target` 17), Maven. Paquete base `com.planificador`: `model` (datos), `service` (lógica y persistencia), `ui` (Swing). No toques `pom.xml` ni agregues dependencias.
- Nombres de clases y métodos en inglés, como el código que ya existe; comentarios en español.
- Las clases de `model` tienen getters: `log.getMinutes()`, `rules.getPassGrade()`, `ev.isRetake()`. Los **records** de `service` se leen con `x()`: `status.condition()`, `a.percent()`. No existen `getCondition()` ni `getPercent()`.
- La lógica no usa `LocalDate.now()`: la fecha de hoy se pasa por parámetro (así se testea).
- Tests con JUnit 5 (`org.junit.jupiter.api.*`); archivos con `@TempDir`. Ningún test lee ni escribe `~/.local/share/planificador-estudio`.

## 5. Cursada (por materia)
- **Asistencia**: `porcentaje = presentes * 100 / registradas` (100 si no hay ninguna). Si se sabe la cantidad de clases (`plannedClasses > 0`): `faltasPermitidas = floor(plannedClasses * (100 - minAttendancePercent) / 100)`, `quedan = faltasPermitidas - ausentes`, y queda libre por faltas si `quedan < 0`. Si no se sabe: libre por faltas si hay clases registradas y `porcentaje < minAttendancePercent`.
- **Parciales** 1…`examCount`. Nota efectiva = la del recuperatorio si hay; si no, la del parcial. Estado: sin nota → `PENDIENTE`; efectiva ≥ `passGrade` → `APROBADO`; menor, sin recuperatorio rendido y con recuperatorio permitido → `A_RECUPERAR`; si no → `DESAPROBADO`. Si hay dos notas del mismo parcial (o del mismo recuperatorio), vale la última cargada.
- Los **TP** se cargan y se muestran, pero no cambian la condición.
- **Condición**, en este orden: `LIBRE` si quedó libre por faltas o hay un parcial `DESAPROBADO`; `EN_CURSO` si hay un parcial `PENDIENTE` o `A_RECUPERAR`, o si `plannedClasses > 0` y todavía hay menos clases registradas; `PROMOCION` si `promotionGrade > 0` y todas las notas efectivas ≥ `promotionGrade`; si no, `REGULAR`.
- Todavía puede promocionar si no está libre, `promotionGrade > 0` y cada parcial está sin nota, tiene efectiva ≥ `promotionGrade` o está `A_RECUPERAR`. Todavía puede regularizar si no está libre.

---

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

---

# Archivos actuales

```java
// src/main/java/com/planificador/service/CourseStatusCalculator.java
package com.planificador.service;

import com.planificador.model.AttendanceRecord;
import com.planificador.model.CourseRules;
import com.planificador.model.CourseTracking;
import com.planificador.model.Evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Calcula la condición de cursada (promoción, regular, libre, en curso) con las reglas de la materia. */
public final class CourseStatusCalculator {

    private static final Locale AR = Locale.forLanguageTag("es-AR");

    private CourseStatusCalculator() { }

    /** Asistencia: porcentaje, faltas y cuántas faltas quedan. */
    public static AttendanceSummary attendance(CourseRules rules, List<AttendanceRecord> records) {
        int registered = records.size();
        int present = 0;
        for (AttendanceRecord r : records) {
            if (r.isPresent()) {
                present++;
            }
        }
        int absent = registered - present;
        double percent = registered == 0 ? 100.0 : present * 100.0 / registered;
        int planned = rules.getPlannedClasses();

        if (planned > 0) {
            // allowed = floor(planned * (100.0 - minAttendancePercent) / 100.0)
            // Agregamos 1e-9 para manejar el redondeo de forma robusta al floor
            int allowed = (int) Math.floor(planned * (100.0 - rules.getMinAttendancePercent()) / 100.0 + 1e-9);
            int remaining = allowed - absent;
            return new AttendanceSummary(registered, present, absent, percent, remaining < 0, true, remaining);
        } else {
            boolean failed = registered > 0 && percent < rules.getMinAttendancePercent();
            return new AttendanceSummary(registered, present, absent, percent, failed, false, 0);
        }
    }

    /** Un ExamResult por parcial, del 1 a rules.getExamCount(). */
    public static List<ExamResult> exams(CourseRules rules, List<Evaluation> evaluations) {
        List<ExamResult> out = new ArrayList<>();
        for (int n = 1; n <= rules.getExamCount(); n++) {
            Double original = null;
            Double retake = null;
            for (Evaluation e : evaluations) {
                if (!Evaluation.PARCIAL.equals(e.getKind()) || e.getNumber() != n) continue;
                if (e.isRetake()) retake = e.getGrade();
                else original = e.getGrade();
            }
            Double effective = retake != null ? retake : original;
            ExamState state;
            if (effective == null) state = ExamState.PENDIENTE;
            else if (effective >= rules.getPassGrade()) state = ExamState.APROBADO;
            else if (retake == null && rules.hasRetake(n)) state = ExamState.A_RECUPERAR;
            else state = ExamState.DESAPROBADO;
            out.add(new ExamResult(n, original, retake, effective, state));
        }
        return out;
    }

    /** Condición de la materia. */
    public static CourseStatus evaluate(CourseTracking tracking) {
        // lo completa otra subtarea (T04c): no lo cambies
        throw new UnsupportedOperationException();
    }

    /** Líneas de texto para mostrar la condición y cuánto falta. */
    public static List<String> describe(CourseStatus status, CourseRules rules) {
        List<String> out = new ArrayList<>();

        String nombre = switch (status.condition()) {
            case PROMOCION -> "Promoción";
            case REGULAR -> "Regular";
            case LIBRE -> "Libre";
            case EN_CURSO -> "En curso";
        };
        out.add("Condición: " + nombre);

        AttendanceSummary a = status.attendance();
        if (a.remainingKnown() > 1) {
            out.add("Podés faltar " + a.remainingAbsences() + " clases más");
        } else if (a.remainingKnown() == 1) {
            out.add("Podés faltar 1 clase más");
        } else if (a.remainingKnown() == 0) {
            out.add("No podés faltar más");
        } else if (a.remainingKnown() < 0) {
            out.add("Superaste el límite de faltas por " + (-a.remainingAbsences()));
        } else {
            out.add("Asistencia: " + String.format(AR, "%.1f", a.percent()) + " % (mínimo " + formatGrade(rules.getMinAttendancePercent()) + " %)");
        }

        String pass = formatGrade(rules.getPassGrade());
        String promo = formatGrade(rules.getPromotionGrade());

        for (ExamResult e : status.exams()) {
            String p = "Parcial " + e.number() + ": ";
            if (e.state() == ExamState.PENDIENTE) {
                out.add(p + "falta rendir (necesitás " + pass + " para regularizar" + (rules.getPromotionGrade() > 0 ? " y " + promo + " para promocionar" : "") + ")");
            } else if (e.state() == ExamState.A_RECUPERAR) {
                out.add(p + formatGrade(e.effective()) + ", tenés que recuperarlo (necesitás " + pass + ")");
            } else if (e.state() == ExamState.APROBADO) {
                out.add(p + formatGrade(e.effective()) + " (aprobado)");
            } else if (e.state() == ExamState.DESAPROBADO) {
                out.add(p + formatGrade(e.effective()) + " (desaprobado)");
            }
        }

        if (status.condition() == CourseCondition.EN_CURSO && rules.getPromotionGrade() > 0) {
            out.add(status.canPromote() ? "Todavía podés promocionar" : "Ya no podés promocionar; todavía podés regularizar");
        }

        return out;
    }

    /** Nota sin decimales si es entera ("7"), si no con uno y coma ("6,5"). */
    public static String formatGrade(double grade) {
        if (grade == Math.rint(grade)) return String.valueOf((long) grade);
        return String.format(AR, "%.1f", grade);
    }
}
```

---

# Error al compilar o testear (corregilo)

```
src/main/java/com/planificador/service/CourseStatusCalculator.java:[85,32] bad operand types for binary operator '>' (first type: boolean, second type: int)
  ← línea 85: if (a.remainingKnown() > 1) {
src/main/java/com/planificador/service/CourseStatusCalculator.java:[87,39] incomparable types: boolean and int
  ← línea 87: } else if (a.remainingKnown() == 1) {
(también líneas 89 y 91: remainingKnown() es boolean)
```

Completá los `// TODO` que pide la tarea T04d. Devolvé estos archivos completos, cada uno en su bloque: src/main/java/com/planificador/service/CourseStatusCalculator.java.
