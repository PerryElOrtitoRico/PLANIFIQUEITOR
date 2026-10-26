<!-- modelo qwen/qwen3.5-9b, temperatura 0.2, max_tokens 3178, razonamiento no -->

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
        // TODO T04c
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
        if (a.remainingKnown()) {
            int quedan = a.remainingAbsences();
            if (quedan > 1) out.add("Podés faltar " + quedan + " clases más");
            else if (quedan == 1) out.add("Podés faltar 1 clase más");
            else if (quedan == 0) out.add("No podés faltar más");
            else out.add("Superaste el límite de faltas por " + (-quedan));
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

```java
// src/test/java/com/planificador/service/CourseStatusCalculatorTest.java
package com.planificador.service;

import com.planificador.model.AttendanceRecord;
import com.planificador.model.CourseRules;
import com.planificador.model.CourseTracking;
import com.planificador.model.Evaluation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CourseStatusCalculatorTest {

    /** 75 % de asistencia, 6 regulariza, 8 promociona, 2 parciales con recuperatorio, 16 clases. */
    static CourseRules reglas() {
        return new CourseRules(75, 6, 8, 2, List.of(true, true), 16);
    }

    static Evaluation parcial(int numero, double nota) {
        return new Evaluation("e" + numero, Evaluation.PARCIAL, numero, false, nota, null);
    }

    static Evaluation recuperatorio(int numero, double nota) {
        return new Evaluation("r" + numero, Evaluation.PARCIAL, numero, true, nota, null);
    }

    /** presentes clases presentes y ausentes clases ausentes. */
    static List<AttendanceRecord> asistencias(int presentes, int ausentes) {
        List<AttendanceRecord> lista = new ArrayList<>();
        for (int i = 0; i < presentes; i++) lista.add(new AttendanceRecord("2026-08-" + (10 + i), true));
        for (int i = 0; i < ausentes; i++) lista.add(new AttendanceRecord("2026-09-" + (10 + i), false));
        return lista;
    }

    @Test
    void asistenciaConClasesConocidas() {
        // Test 1: 8 presentes, 2 ausentes. Total 10. 10 * 100 / 10 = 100%.
        // Planned = 16. MinAttendancePercent = 75.
        // Allowed = floor(16 * (100 - 75) / 100) = floor(16 * 0.25) = floor(4.0) = 4.
        // Remaining = 4 - 2 = 2.
        AttendanceSummary a1 = CourseStatusCalculator.attendance(reglas(), asistencias(8, 2));
        assertEquals(80.0, a1.percent(), 1e-9);
        assertEquals(2, a1.remainingAbsences());
        assertFalse(a1.failed());

        // Test 2: 4 presentes, 5 ausentes. Total 9. 9 * 100 / 9 = 100%.
        // Allowed = 4.
        // Remaining = 4 - 5 = -1. Failed = true.
        AttendanceSummary a2 = CourseStatusCalculator.attendance(reglas(), asistencias(4, 5));
        assertTrue(a2.failed());
    }

    @Test
    void asistenciaSinClasesConocidas() {
        // Reglas: MinAttendancePercent = 75.
        // Test 1: 3 presentes, 1 ausente. Total 4. Percent = 3/4 * 100 = 75.0%.
        // Planned = 0. Failed = (4 > 0 && 75.0 < 75) -> false.
        CourseRules r1 = new CourseRules(75, 6, 8, 2, List.of(true, true), 0);
        AttendanceSummary a1 = CourseStatusCalculator.attendance(r1, asistencias(3, 1));
        assertFalse(a1.failed());
        assertFalse(a1.remainingKnown());

        // Test 2: 2 presentes, 1 ausente. Total 3. Percent = 2/3 * 100 = 66.66...%.
        // Planned = 0. Failed = (3 > 0 && 66.66... < 75) -> true.
        CourseRules r2 = new CourseRules(75, 6, 8, 2, List.of(true, true), 0);
        AttendanceSummary a2 = CourseStatusCalculator.attendance(r2, asistencias(2, 1));
        assertTrue(a2.failed());
    }

    @Test
    void estadosDeLosParciales() {
        List<ExamResult> r = CourseStatusCalculator.exams(reglas(), List.of(parcial(1, 7), parcial(2, 4)));
        assertEquals(ExamState.APROBADO, r.get(0).state());
        assertEquals(ExamState.A_RECUPERAR, r.get(1).state());

        r = CourseStatusCalculator.exams(reglas(), List.of());
        assertEquals(ExamState.PENDIENTE, r.get(0).state());
    }

    @Test
    void elRecuperatorioReemplazaLaNota() {
        List<ExamResult> r = CourseStatusCalculator.exams(reglas(), List.of(parcial(2, 4), recuperatorio(2, 7)));
        assertEquals(ExamState.APROBADO, r.get(1).state());
        assertEquals(7.0, r.get(1).effective());

        r = CourseStatusCalculator.exams(reglas(), List.of(parcial(2, 4), recuperatorio(2, 3)));
        assertEquals(ExamState.DESAPROBADO, r.get(1).state());
    }

    @Test
    void condiciones() {
        // TODO T04c
    }

    @Test
    void todaviaPuedePromocionar() {
        // TODO T04c
    }

    @Test
    void textos() {
        CourseStatus s = new CourseStatus(CourseCondition.EN_CURSO, new AttendanceSummary(10, 8, 2, 80.0, false, true, 2),
                List.of(new ExamResult(1, 6.5, null, 6.5, ExamState.APROBADO)), false, true);
        assertEquals(List.of("Condición: En curso", "Podés faltar 2 clases más", "Parcial 1: 6,5 (aprobado)", "Ya no podés promocionar; todavía podés regularizar"), CourseStatusCalculator.describe(s, reglas()));
        assertEquals("7", CourseStatusCalculator.formatGrade(7));
    }
}
```

Completá los `// TODO` que pide la tarea T04c. Devolvé estos archivos completos, cada uno en su bloque: src/main/java/com/planificador/service/CourseStatusCalculator.java, src/test/java/com/planificador/service/CourseStatusCalculatorTest.java.
