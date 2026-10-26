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
        // Test 1: 3 presentes, 1 ausente. Total 4. Percent = 3/4 * 100 = 75.0

<!-- 407 s · entrada 5159 · salida 2208 · finish_reason length -->
