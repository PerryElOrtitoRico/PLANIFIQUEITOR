package com.planificador.app.logica;

import com.planificador.model.AttendanceRecord;
import com.planificador.model.CourseRules;
import com.planificador.model.Evaluation;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee los campos de los diálogos de Cursada con los mismos rangos que CourseRulesForm y EvaluationForm de la PC.
 * Los errores son IllegalArgumentException con un mensaje para mostrar.
 */
public final class FormularioCursada {

    private FormularioCursada() { }

    public static CourseRules reglas(String asistencia, String regular, String promocion, String parciales,
                                     List<Boolean> recuperatorios, String clases) {
        double a = decimal(asistencia, 0, 100, "La asistencia mínima");
        double r = decimal(regular, 0, 10, "La nota para regularizar");
        double p = decimal(promocion, 0, 10, "La nota para promocionar");
        int n = (int) entero(parciales, 0, 10, "La cantidad de parciales");
        int c = (int) entero(clases, 0, 200, "Las clases del cuatrimestre");
        return new CourseRules(a, r, p, n, recuperatorios(n, recuperatorios), c);
    }

    /** Una casilla por parcial: conserva las que había y las nuevas arrancan marcadas (como la PC). */
    public static List<Boolean> recuperatorios(int n, List<Boolean> previos) {
        List<Boolean> out = new ArrayList<>();
        for (int i = 0; i < n; i++) out.add(i >= previos.size() || Boolean.TRUE.equals(previos.get(i)));
        return out;
    }

    public static Evaluation nota(String id, boolean parcial, String numero, boolean recuperatorio, String nota, String fecha) {
        int num = (int) entero(numero, 1, 20, "El número");
        double g = decimal(nota, 0, 10, "La nota");
        String f = fecha == null || fecha.trim().isEmpty() ? null : fecha(fecha);
        return new Evaluation(id, parcial ? Evaluation.PARCIAL : Evaluation.TP, num, parcial && recuperatorio, g, f);
    }

    public static AttendanceRecord asistencia(String fecha, boolean presente) {
        return new AttendanceRecord(fecha(fecha), presente);
    }

    private static String fecha(String texto) {
        try {
            return LocalDate.parse(texto.trim()).toString();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Fecha inválida: usá AAAA-MM-DD");
        }
    }

    private static double decimal(String texto, double min, double max, String que) {
        double v;
        try {
            v = Double.parseDouble(texto.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(que + " tiene que ser un número.");
        }
        if (v < min || v > max) throw new IllegalArgumentException(que + " tiene que estar entre " + fmt(min) + " y " + fmt(max) + ".");
        return v;
    }

    private static double entero(String texto, int min, int max, String que) {
        double v = decimal(texto, min, max, que);
        if (v != Math.floor(v)) throw new IllegalArgumentException(que + " tiene que ser un número entero.");
        return v;
    }

    private static String fmt(double d) {
        return d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
    }
}
