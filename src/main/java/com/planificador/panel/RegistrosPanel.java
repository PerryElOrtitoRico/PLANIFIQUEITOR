package com.planificador.panel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.planificador.model.CourseRules;
import com.planificador.model.CourseTracking;
import com.planificador.model.Evaluation;
import com.planificador.model.ImportantDate;
import com.planificador.model.StudyLog;
import com.planificador.model.Subject;
import panel.cliente.Clave;
import panel.core.Registro;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Convierte los datos del Planificador (estudio hecho, fechas importantes y notas de parciales)
 * en registros del panel. Función pura: no toca disco ni red y nunca lanza; lo que no se puede
 * convertir se omite en silencio. {@code actualizadoEn} sale en 0: lo asigna el sincronizador.
 */
public final class RegistrosPanel {

    private static final String FUENTE = "planificador";
    private static final String AREA = "estudio";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final Map<String, String> TIPOS_DE_FECHA = Map.of(
            "parcial", "Parcial", "final", "Final", "tp", "TP", "entrega", "Entrega");

    private RegistrosPanel() {
    }

    /**
     * Registros por {@code idEnFuente}, en este orden: estudio (por fecha, materia e id), fechas
     * importantes (en el orden dado) y notas (por materia y número de parcial).
     */
    public static Map<String, Registro> construir(List<Subject> materias, List<StudyLog> estudio,
                                                  List<ImportantDate> fechas, LocalDate hoy, String dispositivo,
                                                  Function<String, String> fechaDeNotaSinFecha) {
        Map<String, Registro> salida = new LinkedHashMap<>();
        try {
            Map<String, Subject> porId = materiasPorId(materias);
            List<CicloValido> ciclos = ciclosValidos(estudio, porId);
            agregarEstudio(salida, ciclos, porId, dispositivo);
            agregarEventos(salida, fechas, porId, ciclos, hoy, dispositivo);
            agregarNotas(salida, materias, dispositivo, fechaDeNotaSinFecha);
        } catch (RuntimeException e) {
            // Nunca se propaga: se devuelve lo que se alcanzó a armar.
        }
        return salida;
    }

    // ---- Estudio ----

    private record CicloValido(StudyLog log, LocalDate fecha) {
    }

    private static Map<String, Subject> materiasPorId(List<Subject> materias) {
        Map<String, Subject> porId = new LinkedHashMap<>();
        if (materias == null) return porId;
        for (Subject s : materias) {
            try {
                if (s != null && s.getId() != null && s.getName() != null) porId.putIfAbsent(s.getId(), s);
            } catch (RuntimeException e) {
                // se omite esa materia
            }
        }
        return porId;
    }

    /** Ciclos con materia existente, fecha ISO válida y minutos positivos. */
    private static List<CicloValido> ciclosValidos(List<StudyLog> estudio, Map<String, Subject> porId) {
        List<CicloValido> validos = new ArrayList<>();
        if (estudio == null) return validos;
        for (StudyLog log : estudio) {
            try {
                if (log == null || log.getMinutes() <= 0 || !porId.containsKey(log.getSubjectId())) continue;
                LocalDate fecha = fechaIso(log.getDate());
                if (fecha != null) validos.add(new CicloValido(log, fecha));
            } catch (RuntimeException e) {
                // se omite ese ciclo
            }
        }
        return validos;
    }

    private static void agregarEstudio(Map<String, Registro> salida, List<CicloValido> ciclos,
                                       Map<String, Subject> porId, String dispositivo) {
        // Clave de grupo: fecha, nombre de la materia, id de la materia.
        Map<List<String>, List<CicloValido>> grupos = new TreeMap<>((a, b) -> {
            for (int i = 0; i < a.size(); i++) {
                int c = a.get(i).compareTo(b.get(i));
                if (c != 0) return c;
            }
            return 0;
        });
        for (CicloValido c : ciclos) {
            Subject s = porId.get(c.log().getSubjectId());
            grupos.computeIfAbsent(List.of(c.fecha().toString(), s.getName(), s.getId()), k -> new ArrayList<>()).add(c);
        }
        Comparator<StudyLog> porHora = Comparator
                .comparing(StudyLog::getStartTime, Comparator.nullsLast(Comparator.<String>naturalOrder()))
                .thenComparing(StudyLog::getId, Comparator.nullsLast(Comparator.<String>naturalOrder()));
        for (Map.Entry<List<String>, List<CicloValido>> g : grupos.entrySet()) {
            try {
                Subject s = porId.get(g.getKey().get(2));
                String fecha = g.getKey().get(0);
                List<StudyLog> logs = new ArrayList<>();
                int minutos = 0;
                for (CicloValido c : g.getValue()) {
                    logs.add(c.log());
                    minutos += c.log().getMinutes();
                }
                if (minutos < 1) continue;
                logs.sort(porHora);
                Set<String> temas = new LinkedHashSet<>();
                for (StudyLog l : logs) {
                    String t = l.getTopicTitle();
                    if (t != null && !t.isBlank()) temas.add(t.trim());
                }
                Map<String, Object> detalle = new LinkedHashMap<>();
                detalle.put("materia", s.getName());
                detalle.put("temas", new ArrayList<>(temas));
                detalle.put("ciclos", logs.size());
                String id = "est:" + s.getId() + ":" + fecha;
                salida.put(id, new Registro("esfuerzo", FUENTE, id, AREA, fecha, minutos,
                        resumenDeEstudio(s.getName(), new ArrayList<>(temas)), GSON.toJson(detalle),
                        null, null, null, dispositivo, 0L, false));
            } catch (RuntimeException e) {
                // se omite ese grupo
            }
        }
    }

    private static String resumenDeEstudio(String materia, List<String> temas) {
        if (temas.isEmpty()) return materia;
        String resumen = materia + " · " + String.join(", ", temas.subList(0, Math.min(3, temas.size())));
        if (temas.size() > 3) resumen += " y " + (temas.size() - 3) + " más";
        return resumen;
    }

    // ---- Fechas importantes ----

    private static void agregarEventos(Map<String, Registro> salida, List<ImportantDate> fechas,
                                       Map<String, Subject> porId, List<CicloValido> ciclos, LocalDate hoy,
                                       String dispositivo) {
        if (fechas == null) return;
        for (ImportantDate d : fechas) {
            try {
                if (d == null || d.getId() == null) continue;
                LocalDate fecha = fechaIso(d.getDate());
                if (fecha == null) continue;
                String tipo = d.getType() == null ? "" : d.getType().trim().toLowerCase(Locale.ROOT);
                if (!TIPOS_DE_FECHA.containsKey(tipo)) tipo = "fecha";
                String nombreTipo = TIPOS_DE_FECHA.getOrDefault(tipo, "Fecha");
                Subject s = d.getSubjectId() == null ? null : porId.get(d.getSubjectId());
                String titulo = d.getTitle() == null ? "" : d.getTitle().trim();

                StringBuilder resumen = new StringBuilder(nombreTipo);
                if (s != null) resumen.append(" · ").append(s.getName());
                if (!titulo.isEmpty()) resumen.append(" · ").append(titulo);

                Map<String, Object> detalle = new LinkedHashMap<>();
                if (s != null) detalle.put("materia", s.getName());
                detalle.put("tipo", tipo);
                if (!titulo.isEmpty()) detalle.put("titulo", titulo);
                if (s != null) detalle.put("minutosEstudiados", minutosHasta(ciclos, s.getId(), fecha, hoy));

                String id = "fec:" + d.getId();
                salida.put(id, new Registro("evento", FUENTE, id, AREA, fecha.toString(), null,
                        resumen.toString(), GSON.toJson(detalle), null, tipo, null, dispositivo, 0L, false));
            } catch (RuntimeException e) {
                // se omite esa fecha
            }
        }
    }

    /** Minutos de estudio de la materia con fecha menor o igual a hoy y a la fecha del evento. */
    private static int minutosHasta(List<CicloValido> ciclos, String materiaId, LocalDate fechaEvento, LocalDate hoy) {
        LocalDate limite = hoy != null && hoy.isBefore(fechaEvento) ? hoy : fechaEvento;
        int total = 0;
        for (CicloValido c : ciclos) {
            if (materiaId.equals(c.log().getSubjectId()) && !c.fecha().isAfter(limite)) total += c.log().getMinutes();
        }
        return total;
    }

    // ---- Notas ----

    private static void agregarNotas(Map<String, Registro> salida, List<Subject> materias, String dispositivo,
                                     Function<String, String> fechaDeNotaSinFecha) {
        if (materias == null) return;
        Set<String> vistas = new LinkedHashSet<>();
        for (Subject s : materias) {
            try {
                if (s == null || s.getId() == null || s.getName() == null || !vistas.add(s.getId())) continue;
                CourseTracking curso = s.getCourse();
                if (curso == null) continue;
                String clave = Clave.de(s.getName());
                if (clave.isEmpty()) continue;
                // Por número de parcial: el último recuperatorio, o si no hay, la última nota normal.
                Map<Integer, Evaluation> efectivas = new TreeMap<>();
                Map<Integer, Evaluation> normales = new TreeMap<>();
                for (Evaluation e : curso.getEvaluations()) {
                    if (e == null || !Evaluation.PARCIAL.equals(e.getKind())) continue;
                    (e.isRetake() ? efectivas : normales).put(e.getNumber(), e);
                }
                normales.forEach(efectivas::putIfAbsent);
                for (Evaluation e : efectivas.values()) {
                    try {
                        agregarNota(salida, s, curso, e, clave, dispositivo, fechaDeNotaSinFecha);
                    } catch (RuntimeException ex) {
                        // se omite esa nota
                    }
                }
            } catch (RuntimeException e) {
                // se omite esa materia
            }
        }
    }

    private static void agregarNota(Map<String, Registro> salida, Subject s, CourseTracking curso, Evaluation e,
                                    String clave, String dispositivo, Function<String, String> fechaDeNotaSinFecha) {
        double nota = e.getGrade();
        if (!Double.isFinite(nota)) return;
        String id = "nota:" + s.getId() + ":parcial" + e.getNumber();
        LocalDate fecha = fechaIso(e.getDate());
        if (fecha == null && fechaDeNotaSinFecha != null) fecha = fechaIso(fechaDeNotaSinFecha.apply(id));
        if (fecha == null) return;

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("materia", s.getName());
        detalle.put("numero", e.getNumber());
        detalle.put("recuperatorio", e.isRetake());
        CourseRules reglas = curso.getRules();
        if (reglas != null && Double.isFinite(reglas.getPassGrade())) detalle.put("notaAprobacion", comoNumero(reglas.getPassGrade()));

        String resumen = "Parcial " + e.getNumber() + (e.isRetake() ? " (recuperatorio)" : "")
                + " · " + s.getName() + ": " + comoTexto(nota);
        salida.put(id, new Registro("resultado", FUENTE, id, AREA, fecha.toString(), null, resumen,
                GSON.toJson(detalle), null, "nota:" + clave, nota, dispositivo, 0L, false));
    }

    /** Entero si no tiene decimales (7), si no el mismo valor (6.5), para usar como número JSON. */
    private static Number comoNumero(double v) {
        return v == Math.rint(v) && Math.abs(v) < 1e15 ? (Number) (long) v : (Number) v;
    }

    /** Para el texto: sin decimales si es entera (7) y con coma si no (6,5). */
    private static String comoTexto(double v) {
        return comoNumero(v).toString().replace('.', ',');
    }

    private static LocalDate fechaIso(String texto) {
        if (texto == null) return null;
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
