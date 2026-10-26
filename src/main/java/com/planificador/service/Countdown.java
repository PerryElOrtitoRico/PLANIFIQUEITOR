package com.planificador.service;

import com.planificador.model.ImportantDate;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Cuenta regresiva de las fechas importantes para la pantalla de Hoy. La fecha de hoy se pasa por parámetro. */
public final class Countdown {

    private Countdown() { }

    /** Días desde today hasta la fecha ISO (YYYY-MM-DD); negativo si ya pasó; null si la fecha es null o no se puede leer. */
    public static Long daysUntil(String isoDate, LocalDate today) {
        // TODO T01
        throw new UnsupportedOperationException();
    }

    /** Fechas con 0 <= días <= maxDays, ordenadas por días y, a igual días, por título. Las fechas ilegibles se saltean. */
    public static List<ImportantDate> upcoming(List<ImportantDate> dates, LocalDate today, int maxDays) {
        // TODO T01
        throw new UnsupportedOperationException();
    }

    /** Texto para mostrar: "Primer parcial (Mecánica de Fluidos) en 6 días", "… es mañana", "… es hoy". */
    public static String text(ImportantDate date, String subjectName, LocalDate today) {
        // TODO T01
        throw new UnsupportedOperationException();
    }
}
