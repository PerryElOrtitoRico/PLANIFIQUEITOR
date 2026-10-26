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
