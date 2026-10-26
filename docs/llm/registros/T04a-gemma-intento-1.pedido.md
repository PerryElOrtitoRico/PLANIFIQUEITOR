<!-- modelo google/gemma-4-e4b-qat, temperatura 0.2, max_tokens 4416, razonamiento no -->

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
        // TODO T04a
        throw new UnsupportedOperationException();
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
        // lo completa otra subtarea (T04d): no lo cambies
        throw new UnsupportedOperationException();
    }

    /** Nota sin decimales si es entera ("7"), si no con uno y coma ("6,5"). */
    public static String formatGrade(double grade) {
        // lo completa otra subtarea (T04d): no lo cambies
        throw new UnsupportedOperationException();
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
        // TODO T04a
    }

    @Test
    void asistenciaSinClasesConocidas() {
        // TODO T04a
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
        // lo completa otra subtarea (T04c): no lo cambies
    }

    @Test
    void todaviaPuedePromocionar() {
        // lo completa otra subtarea (T04c): no lo cambies
    }

    @Test
    void textos() {
        // lo completa otra subtarea (T04d): no lo cambies
    }
}
```

Completá los `// TODO` que pide la tarea T04a. Devolvé estos archivos completos, cada uno en su bloque: src/main/java/com/planificador/service/CourseStatusCalculator.java, src/test/java/com/planificador/service/CourseStatusCalculatorTest.java.
