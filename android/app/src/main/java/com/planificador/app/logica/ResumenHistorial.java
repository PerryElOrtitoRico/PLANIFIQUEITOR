package com.planificador.app.logica;

import com.planificador.model.StudyLog;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Cuentas del Historial (calendario del mes y sesiones de un día). */
public final class ResumenHistorial {

    private ResumenHistorial() { }

    /** Minutos de cada día del mes que tuvo estudio. */
    public static Map<LocalDate, Integer> minutosPorDia(List<StudyLog> logs, YearMonth mes) {
        Map<LocalDate, Integer> r = new TreeMap<>();
        for (StudyLog l : logs) {
            LocalDate d = fecha(l);
            if (d != null && YearMonth.from(d).equals(mes)) r.merge(d, l.getMinutes(), Integer::sum);
        }
        return r;
    }

    /** Sesiones de ese día, por hora de inicio; las que no tienen hora (guía) al final. */
    public static List<StudyLog> delDia(List<StudyLog> logs, LocalDate dia) {
        List<StudyLog> r = new ArrayList<>();
        for (StudyLog l : logs) if (dia.equals(fecha(l))) r.add(l);
        r.sort(Comparator.comparing(StudyLog::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())));
        return r;
    }

    /** Igual que HistorialPanel.formatear de la PC: "1h05", "45m", "2h". */
    public static String formatear(int minutos) {
        return minutos >= 60 ? (minutos / 60) + "h" + (minutos % 60 > 0 ? String.format("%02d", minutos % 60) : "")
                : minutos + "m";
    }

    /** null si la fecha no se puede leer (un archivo escrito a mano): esa sesión no se muestra en el calendario. */
    private static LocalDate fecha(StudyLog l) {
        try {
            return l.getDate() == null ? null : LocalDate.parse(l.getDate());
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }
}
