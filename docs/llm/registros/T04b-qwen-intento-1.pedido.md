<!-- modelo qwen/qwen3.5-9b, temperatura 0.2, max_tokens 4878, razonamiento no -->

## system

Sos un programador Java 17 cuidadoso. Te paso archivos con métodos marcados `// TODO`: completá solo los cuerpos que nombra la tarea; los métodos marcados "lo completa otra subtarea" quedan exactamente como están.
Reglas:
- No cambies package, imports que ya están (aunque todavía no se usen), nombres de clases, firmas ni métodos que ya están hechos. Podés agregar imports y métodos private.
- Las clases de model se leen con getters: log.getMinutes(), rules.getPassGrade(). Los records de service se leen con x(): status.condition(), a.percent(). No existen getCondition() ni getPercent().
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

# T04b — Cursada: estado de cada parcial

## Objetivo
Completar **solo** `exams` de `CourseStatusCalculator` y los tests `estadosDeLosParciales` y `elRecuperatorioReemplazaLaNota`. `attendance` ya está hecho.

## Código que ya existe
- `Evaluation` (clase con getters): `e.getKind()` (`Evaluation.PARCIAL` o `Evaluation.TP`), `e.getNumber()`, `e.isRetake()`, `e.getGrade()` (double).
- `CourseRules` (clase con getters): `rules.getExamCount()`, `rules.getPassGrade()`, `rules.hasRetake(n)`.
- `ExamResult` es un record: `new ExamResult(number, original, retake, effective, state)` con `Double` (pueden ser null); se lee con `r.state()`, `r.effective()`.

## Método `exams(rules, evaluations)` (pasos en orden)
1. `List<ExamResult> out = new ArrayList<>();`
2. `for (int n = 1; n <= rules.getExamCount(); n++) {`
3. `  Double original = null; Double retake = null;`
4. `  for (Evaluation e : evaluations) { if (!Evaluation.PARCIAL.equals(e.getKind()) || e.getNumber() != n) continue; if (e.isRetake()) retake = e.getGrade(); else original = e.getGrade(); }`
5. `  Double effective = retake != null ? retake : original;`
6. `  ExamState state; if (effective == null) state = ExamState.PENDIENTE; else if (effective >= rules.getPassGrade()) state = ExamState.APROBADO; else if (retake == null && rules.hasRetake(n)) state = ExamState.A_RECUPERAR; else state = ExamState.DESAPROBADO;`
7. `  out.add(new ExamResult(n, original, retake, effective, state));`
8. `}` y `return out;`

## Tests (usan `reglas()` = 6 regulariza, 2 parciales con recuperatorio, `parcial(n, nota)` y `recuperatorio(n, nota)` que ya están en el test)
- `estadosDeLosParciales`: `List<ExamResult> r = CourseStatusCalculator.exams(reglas(), List.of(parcial(1, 7), parcial(2, 4)));` → `assertEquals(ExamState.APROBADO, r.get(0).state())` y `assertEquals(ExamState.A_RECUPERAR, r.get(1).state())`; y con `List.of()` → `assertEquals(ExamState.PENDIENTE, ...get(0).state())`.
- `elRecuperatorioReemplazaLaNota`: con `List.of(parcial(2, 4), recuperatorio(2, 7))` → `get(1).effective()` es `7.0` y el estado `APROBADO`; con `List.of(parcial(2, 4), recuperatorio(2, 3))` → estado `DESAPROBADO`.

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
        // lo completa otra subtarea (T04a): no lo cambies
        throw new UnsupportedOperationException();
    }

    /** Un ExamResult por parcial, del 1 a rules.getExamCount(). */
    public static List<ExamResult> exams(CourseRules rules, List<Evaluation> evaluations) {
        // TODO T04b
        throw new UnsupportedOperationException();
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
        // lo completa otra subtarea (T04a): no lo cambies
    }

    @Test
    void asistenciaSinClasesConocidas() {
        // lo completa otra subtarea (T04a): no lo cambies
    }

    @Test
    void estadosDeLosParciales() {
        // TODO T04b
    }

    @Test
    void elRecuperatorioReemplazaLaNota() {
        // TODO T04b
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

Completá los `// TODO` que pide la tarea T04b. Devolvé estos archivos completos, cada uno en su bloque: src/main/java/com/planificador/service/CourseStatusCalculator.java, src/test/java/com/planificador/service/CourseStatusCalculatorTest.java.
