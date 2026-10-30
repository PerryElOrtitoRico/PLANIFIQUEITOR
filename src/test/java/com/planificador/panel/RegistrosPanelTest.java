package com.planificador.panel;

import com.planificador.model.CourseRules;
import com.planificador.model.Evaluation;
import com.planificador.model.ImportantDate;
import com.planificador.model.StudyLog;
import com.planificador.model.Subject;
import org.junit.jupiter.api.Test;
import panel.core.Registro;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrosPanelTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 30);
    private static final Function<String, String> SIN_RECORDATORIO = id -> null;

    private static Subject materia(String id, String nombre) {
        return new Subject(id, nombre, "#000000", "", "");
    }

    private static StudyLog ciclo(String id, String materiaId, String tema, String fecha, String hora, int minutos) {
        return new StudyLog(id, materiaId, null, tema, fecha, hora, minutos);
    }

    private static Map<String, Registro> construir(List<Subject> materias, List<StudyLog> estudio,
                                                   List<ImportantDate> fechas) {
        return RegistrosPanel.construir(materias, estudio, fechas, HOY, "pc-casa", SIN_RECORDATORIO);
    }

    private static Map<String, Registro> soloEstudio(List<Subject> materias, List<StudyLog> estudio) {
        return construir(materias, estudio, List.of());
    }

    private static Subject conNotas(String id, String nombre, double aprobacion, Evaluation... notas) {
        Subject s = materia(id, nombre);
        s.getOrCreateCourse().setRules(new CourseRules(75, aprobacion, 8, 2, List.of(true, true), 0));
        for (Evaluation e : notas) s.getOrCreateCourse().getEvaluations().add(e);
        return s;
    }

    private static Map<String, Registro> soloNotas(Subject s) {
        return construir(List.of(s), List.of(), List.of());
    }

    // ---- Estudio ----

    @Test
    void agrupaCiclosPorMateriaYDia() {
        List<Subject> materias = List.of(materia("m1", "Análisis"), materia("m2", "Física"));
        List<StudyLog> estudio = List.of(
                ciclo("l1", "m1", "Límites", "2026-10-28", "09:00", 25),
                ciclo("l2", "m1", "Derivadas", "2026-10-28", "10:00", 25),
                ciclo("l3", "m1", "Límites", "2026-10-28", "11:00", 30),
                ciclo("l4", "m1", "Integrales", "2026-10-29", "09:00", 40),
                ciclo("l5", "m2", "Cinemática", "2026-10-28", "09:00", 50));

        Map<String, Registro> r = soloEstudio(materias, estudio);

        assertEquals(List.of("est:m1:2026-10-28", "est:m2:2026-10-28", "est:m1:2026-10-29"),
                List.copyOf(r.keySet()));
        Registro a = r.get("est:m1:2026-10-28");
        assertEquals("esfuerzo", a.tipo());
        assertEquals("planificador", a.fuente());
        assertEquals("estudio", a.area());
        assertEquals("2026-10-28", a.fecha());
        assertEquals(80, a.duracionMin());
        assertNull(a.esfuerzo());
        assertNull(a.metrica());
        assertNull(a.valor());
        assertEquals("pc-casa", a.dispositivo());
        assertEquals(0L, a.actualizadoEn());
        assertFalse(a.borrado());
        assertEquals("Análisis · Límites, Derivadas", a.resumen());
        assertEquals("{\"materia\":\"Análisis\",\"temas\":[\"Límites\",\"Derivadas\"],\"ciclos\":3}", a.detalleJson());
        assertEquals(50, r.get("est:m2:2026-10-28").duracionMin());
    }

    @Test
    void resumenConTresTemasYMas() {
        List<Subject> materias = List.of(materia("m1", "Análisis"));
        List<StudyLog> tres = List.of(
                ciclo("l1", "m1", "A", "2026-10-28", "09:00", 25),
                ciclo("l2", "m1", "B", "2026-10-28", "10:00", 25),
                ciclo("l3", "m1", "C", "2026-10-28", "11:00", 25));
        assertEquals("Análisis · A, B, C", soloEstudio(materias, tres).get("est:m1:2026-10-28").resumen());

        List<StudyLog> cinco = new ArrayList<>(tres);
        cinco.add(ciclo("l4", "m1", "D", "2026-10-28", "12:00", 25));
        cinco.add(ciclo("l5", "m1", "E", "2026-10-28", "13:00", 25));
        Registro r = soloEstudio(materias, cinco).get("est:m1:2026-10-28");
        assertEquals("Análisis · A, B, C y 2 más", r.resumen());
        assertEquals("{\"materia\":\"Análisis\",\"temas\":[\"A\",\"B\",\"C\",\"D\",\"E\"],\"ciclos\":5}", r.detalleJson());

        List<StudyLog> sinTemas = List.of(ciclo("l1", "m1", null, "2026-10-28", "09:00", 25));
        assertEquals("Análisis", soloEstudio(materias, sinTemas).get("est:m1:2026-10-28").resumen());
    }

    @Test
    void temasSeOrdenanPorHoraYNoSeRepiten() {
        List<Subject> materias = List.of(materia("m1", "Análisis"));
        // En la lista vienen desordenados; sin hora va al final; mismo horario desempata por id.
        List<StudyLog> estudio = List.of(
                ciclo("l9", "m1", "Sin hora", "2026-10-28", null, 10),
                ciclo("l3", "m1", "Tarde", "2026-10-28", "15:00", 10),
                ciclo("l2", "m1", "Mañana b", "2026-10-28", "09:00", 10),
                ciclo("l1", "m1", "Mañana a", "2026-10-28", "09:00", 10),
                ciclo("l4", "m1", " Tarde ", "2026-10-28", "16:00", 10));

        Registro r = soloEstudio(materias, estudio).get("est:m1:2026-10-28");

        assertEquals("{\"materia\":\"Análisis\",\"temas\":[\"Mañana a\",\"Mañana b\",\"Tarde\",\"Sin hora\"],\"ciclos\":5}",
                r.detalleJson());
        assertEquals("Análisis · Mañana a, Mañana b, Tarde y 1 más", r.resumen());
    }

    @Test
    void cicloQueTerminaDespuesDeMedianocheCuentaEnElDiaDeFin() {
        List<Subject> materias = List.of(materia("m1", "Análisis"));
        // Empezó 23:40 del 30 y terminó el 31: el ciclo guarda el día en que terminó.
        List<StudyLog> estudio = List.of(
                ciclo("l1", "m1", "Límites", "2026-10-30", "20:00", 25),
                ciclo("l2", "m1", "Límites", "2026-10-31", "23:40", 25));

        Map<String, Registro> r = soloEstudio(materias, estudio);

        assertEquals(List.of("est:m1:2026-10-30", "est:m1:2026-10-31"), List.copyOf(r.keySet()));
        assertEquals(25, r.get("est:m1:2026-10-30").duracionMin());
        assertEquals(25, r.get("est:m1:2026-10-31").duracionMin());
    }

    @Test
    void temasNulosOEnBlancoSeOmiten() {
        List<Subject> materias = List.of(materia("m1", "Análisis"));
        List<StudyLog> estudio = List.of(
                ciclo("l1", "m1", null, "2026-10-28", "09:00", 20),
                ciclo("l2", "m1", "   ", "2026-10-28", "10:00", 20),
                ciclo("l3", "m1", "", "2026-10-28", "11:00", 20),
                ciclo("l4", "m1", "Límites", "2026-10-28", "12:00", 20));

        Registro r = soloEstudio(materias, estudio).get("est:m1:2026-10-28");

        assertEquals(80, r.duracionMin());
        assertEquals("Análisis · Límites", r.resumen());
        assertEquals("{\"materia\":\"Análisis\",\"temas\":[\"Límites\"],\"ciclos\":4}", r.detalleJson());
    }

    @Test
    void minutosCeroNoGeneranRegistro() {
        List<Subject> materias = List.of(materia("m1", "Análisis"));
        List<StudyLog> estudio = List.of(
                ciclo("l1", "m1", "A", "2026-10-28", "09:00", 0),
                ciclo("l2", "m1", "B", "2026-10-28", "10:00", -5),
                ciclo("l3", "m1", "C", "2026-10-29", "09:00", 0),
                ciclo("l4", "m1", "D", "2026-10-29", "10:00", 15));

        Map<String, Registro> r = soloEstudio(materias, estudio);

        assertEquals(List.of("est:m1:2026-10-29"), List.copyOf(r.keySet()));
        Registro d = r.get("est:m1:2026-10-29");
        assertEquals(15, d.duracionMin());
        assertEquals("{\"materia\":\"Análisis\",\"temas\":[\"D\"],\"ciclos\":1}", d.detalleJson());
    }

    @Test
    void estudioDeMateriaInexistenteSeIgnora() {
        List<StudyLog> estudio = List.of(ciclo("l1", "borrada", "A", "2026-10-28", "09:00", 25));
        assertTrue(soloEstudio(List.of(materia("m1", "Análisis")), estudio).isEmpty());
    }

    @Test
    void fechaInvalidaDeUnCicloSeOmite() {
        List<Subject> materias = List.of(materia("m1", "Análisis"));
        List<StudyLog> estudio = List.of(
                ciclo("l1", "m1", "A", "28/10/2026", "09:00", 25),
                ciclo("l2", "m1", "B", null, "09:00", 25),
                ciclo("l3", "m1", "C", "2026-02-30", "09:00", 25),
                ciclo("l4", "m1", "D", "2026-10-28", "09:00", 25));

        Map<String, Registro> r = soloEstudio(materias, estudio);

        assertEquals(List.of("est:m1:2026-10-28"), List.copyOf(r.keySet()));
        assertEquals("{\"materia\":\"Análisis\",\"temas\":[\"D\"],\"ciclos\":1}", r.get("est:m1:2026-10-28").detalleJson());
    }

    // ---- Fechas importantes ----

    @Test
    void eventoConMateria() {
        List<Subject> materias = List.of(materia("m1", "Análisis"), materia("m2", "Física"));
        List<StudyLog> estudio = List.of(
                ciclo("l1", "m1", "A", "2026-11-01", "09:00", 30),
                ciclo("l2", "m1", "B", "2026-11-05", "09:00", 45),   // día del evento, pero posterior a hoy: no cuenta
                ciclo("l3", "m1", "C", "2026-11-06", "09:00", 60),   // posterior a hoy y al evento: no cuenta
                ciclo("l4", "m2", "D", "2026-11-02", "09:00", 99));  // otra materia
        ImportantDate parcial = new ImportantDate("d1", "Parcial 1", "m1", "2026-11-05", "parcial", "");
        // La fecha ya pasó: cuenta hasta el día del evento (2026-10-20) y no hasta hoy.
        ImportantDate pasado = new ImportantDate("d2", "TP final", "m1", "2026-10-20", " TP ", "");
        List<StudyLog> todos = new ArrayList<>(estudio);
        todos.add(ciclo("l5", "m1", "E", "2026-10-10", "09:00", 20));
        todos.add(ciclo("l6", "m1", "F", "2026-10-25", "09:00", 20));

        Map<String, Registro> r = construir(materias, todos, List.of(parcial, pasado));

        Registro e = r.get("fec:d1");
        assertEquals("evento", e.tipo());
        assertEquals("planificador", e.fuente());
        assertEquals("estudio", e.area());
        assertEquals("2026-11-05", e.fecha());
        assertEquals("parcial", e.metrica());
        assertEquals("Parcial · Análisis · Parcial 1", e.resumen());
        assertNull(e.duracionMin());
        assertNull(e.valor());
        assertNull(e.esfuerzo());
        assertEquals("pc-casa", e.dispositivo());
        assertEquals(0L, e.actualizadoEn());
        assertFalse(e.borrado());
        // hoy = 2026-10-30: solo cuentan l5 (20) y l6 (20); lo posterior a hoy todavía no pasó.
        assertEquals("{\"materia\":\"Análisis\",\"tipo\":\"parcial\",\"titulo\":\"Parcial 1\",\"minutosEstudiados\":40}",
                e.detalleJson());

        Registro p = r.get("fec:d2");
        assertEquals("tp", p.metrica());
        assertEquals("TP · Análisis · TP final", p.resumen());
        assertEquals("{\"materia\":\"Análisis\",\"tipo\":\"tp\",\"titulo\":\"TP final\",\"minutosEstudiados\":20}",
                p.detalleJson());
    }

    @Test
    void eventoFuturoNoCuentaEstudioPosterior() {
        List<Subject> materias = List.of(materia("m1", "Análisis"));
        List<StudyLog> estudio = List.of(
                ciclo("l1", "m1", "A", "2026-10-30", "09:00", 30),   // hoy: cuenta
                ciclo("l2", "m1", "B", "2026-10-31", "09:00", 45),   // mañana: todavía no
                ciclo("l3", "m1", "C", "2026-10-29", "09:00", 10));
        ImportantDate final_ = new ImportantDate("d1", "Final", "m1", "2026-12-10", "final", "");

        Registro e = construir(materias, estudio, List.of(final_)).get("fec:d1");

        assertEquals("Final · Análisis · Final", e.resumen());
        assertEquals("{\"materia\":\"Análisis\",\"tipo\":\"final\",\"titulo\":\"Final\",\"minutosEstudiados\":40}",
                e.detalleJson());
    }

    @Test
    void eventoSinMateria() {
        ImportantDate d = new ImportantDate("d1", "Entrega de la maqueta", "inexistente", "2026-11-15", "entrega", "");
        ImportantDate sinTitulo = new ImportantDate("d2", "  ", null, "2026-11-16", "entrega", "");

        Map<String, Registro> r = construir(List.of(materia("m1", "Análisis")), List.of(), List.of(d, sinTitulo));

        Registro e = r.get("fec:d1");
        assertEquals("entrega", e.metrica());
        assertEquals("Entrega · Entrega de la maqueta", e.resumen());
        assertEquals("{\"tipo\":\"entrega\",\"titulo\":\"Entrega de la maqueta\"}", e.detalleJson());

        Registro s = r.get("fec:d2");
        assertEquals("Entrega", s.resumen());
        assertEquals("{\"tipo\":\"entrega\"}", s.detalleJson());
    }

    @Test
    void eventoDeTipoDesconocidoUsaMetricaFecha() {
        List<Subject> materias = List.of(materia("m1", "Análisis"));
        ImportantDate raro = new ImportantDate("d1", "Visita", "m1", "2026-11-15", "excursion", "");
        ImportantDate vacio = new ImportantDate("d2", "Algo", "m1", "2026-11-16", "", "");
        ImportantDate nulo = new ImportantDate("d3", "Otro", null, "2026-11-17", null, "");

        Map<String, Registro> r = construir(materias, List.of(), List.of(raro, vacio, nulo));

        assertEquals("fecha", r.get("fec:d1").metrica());
        assertEquals("Fecha · Análisis · Visita", r.get("fec:d1").resumen());
        assertEquals("{\"materia\":\"Análisis\",\"tipo\":\"fecha\",\"titulo\":\"Visita\",\"minutosEstudiados\":0}",
                r.get("fec:d1").detalleJson());
        assertEquals("fecha", r.get("fec:d2").metrica());
        assertEquals("fecha", r.get("fec:d3").metrica());
        assertEquals("Fecha · Otro", r.get("fec:d3").resumen());
    }

    @Test
    void eventoConFechaInvalidaSeOmite() {
        List<Subject> materias = List.of(materia("m1", "Análisis"));
        Map<String, Registro> r = construir(materias, List.of(), List.of(
                new ImportantDate("d1", "A", "m1", null, "parcial", ""),
                new ImportantDate("d2", "B", "m1", "15/11/2026", "parcial", ""),
                new ImportantDate("d3", "C", "m1", "", "parcial", ""),
                new ImportantDate("d4", "D", "m1", "2026-11-15", "parcial", "")));
        assertEquals(List.of("fec:d4"), List.copyOf(r.keySet()));
    }

    // ---- Notas ----

    @Test
    void notaUsaElRecuperatorioAunqueSeaMasBaja() {
        Subject s = conNotas("m1", "Análisis", 6,
                new Evaluation("e1", "parcial", 1, false, 7, "2026-09-10"),
                new Evaluation("e2", "parcial", 1, true, 4, "2026-09-24"),
                new Evaluation("e3", "parcial", 2, false, 8, "2026-10-15"));

        Map<String, Registro> r = soloNotas(s);

        assertEquals(List.of("nota:m1:parcial1", "nota:m1:parcial2"), List.copyOf(r.keySet()));
        Registro n = r.get("nota:m1:parcial1");
        assertEquals("resultado", n.tipo());
        assertEquals("planificador", n.fuente());
        assertEquals("estudio", n.area());
        assertEquals("2026-09-24", n.fecha());
        assertEquals("nota:analisis", n.metrica());
        assertEquals(4.0, n.valor());
        assertNull(n.duracionMin());
        assertNull(n.esfuerzo());
        assertEquals("Parcial 1 (recuperatorio) · Análisis: 4", n.resumen());
        assertEquals("{\"materia\":\"Análisis\",\"numero\":1,\"recuperatorio\":true,\"notaAprobacion\":6}", n.detalleJson());

        Registro n2 = r.get("nota:m1:parcial2");
        assertEquals("Parcial 2 · Análisis: 8", n2.resumen());
        assertEquals("{\"materia\":\"Análisis\",\"numero\":2,\"recuperatorio\":false,\"notaAprobacion\":6}", n2.detalleJson());
    }

    @Test
    void notaConComaDecimal() {
        Subject s = conNotas("m1", "Análisis", 5.5,
                new Evaluation("e1", "parcial", 1, false, 6.5, "2026-09-10"));

        Registro n = soloNotas(s).get("nota:m1:parcial1");

        assertEquals("Parcial 1 · Análisis: 6,5", n.resumen());
        assertEquals(6.5, n.valor());
        assertEquals("{\"materia\":\"Análisis\",\"numero\":1,\"recuperatorio\":false,\"notaAprobacion\":5.5}", n.detalleJson());
    }

    @Test
    void notaSinFechaUsaLaFechaRecordada() {
        Subject s = conNotas("m1", "Análisis", 6,
                new Evaluation("e1", "parcial", 1, false, 7, null),
                new Evaluation("e2", "parcial", 2, false, 9, "no es fecha"));
        Function<String, String> recordada = id -> "nota:m1:parcial1".equals(id) ? "2026-10-01" : "2026-10-02";

        Map<String, Registro> r = RegistrosPanel.construir(List.of(s), List.of(), List.of(), HOY, "pc-casa", recordada);

        assertEquals("2026-10-01", r.get("nota:m1:parcial1").fecha());
        assertEquals("2026-10-02", r.get("nota:m1:parcial2").fecha());
    }

    @Test
    void notaSinFechaYSinRecordatorioSeOmite() {
        Subject s = conNotas("m1", "Análisis", 6,
                new Evaluation("e1", "parcial", 1, false, 7, null),
                new Evaluation("e2", "parcial", 2, false, 9, "2026-10-15"));
        Function<String, String> invalida = id -> "ayer";

        assertEquals(List.of("nota:m1:parcial2"), List.copyOf(soloNotas(s).keySet()));
        assertEquals(List.of("nota:m1:parcial2"),
                List.copyOf(RegistrosPanel.construir(List.of(s), List.of(), List.of(), HOY, "pc", invalida).keySet()));
    }

    @Test
    void notaDeTPNoSeEnvia() {
        Subject s = conNotas("m1", "Análisis", 6,
                new Evaluation("e1", "tp", 1, false, 9, "2026-10-01"));
        assertTrue(soloNotas(s).isEmpty());
    }

    @Test
    void materiaConClaveVaciaNoMandaNota() {
        Subject s = conNotas("m1", "¿¡?!", 6,
                new Evaluation("e1", "parcial", 1, false, 7, "2026-10-01"));
        assertTrue(soloNotas(s).isEmpty());
    }

    // ---- Generales ----

    @Test
    void listasNulasDevuelvenVacio() {
        assertTrue(RegistrosPanel.construir(null, null, null, HOY, "pc", SIN_RECORDATORIO).isEmpty());
        assertTrue(RegistrosPanel.construir(List.of(), List.of(), List.of(), HOY, "pc", null).isEmpty());
    }

    @Test
    void todosLosRegistrosPasanErrores() {
        Subject s = conNotas("m1", "Análisis", 6,
                new Evaluation("e1", "parcial", 1, false, 7, "2026-10-01"));
        Map<String, Registro> r = construir(List.of(s),
                List.of(ciclo("l1", "m1", "Límites", "2026-10-28", "09:00", 25)),
                List.of(new ImportantDate("d1", "Parcial 1", "m1", "2026-11-05", "parcial", ""),
                        new ImportantDate("d2", "Visita", null, "2026-11-06", "otro", "")));

        assertEquals(4, r.size());
        for (Registro reg : r.values()) {
            Registro conReloj = new Registro(reg.tipo(), reg.fuente(), reg.idEnFuente(), reg.area(), reg.fecha(),
                    reg.duracionMin(), reg.resumen(), reg.detalleJson(), reg.esfuerzo(), reg.metrica(), reg.valor(),
                    reg.dispositivo(), 1L, reg.borrado());
            assertEquals(List.of(), conReloj.errores(), reg.idEnFuente());
        }
    }

    @Test
    void ordenDeLosRegistros() {
        Subject b = conNotas("mb", "Beta", 6, new Evaluation("e1", "parcial", 2, false, 7, "2026-10-01"),
                new Evaluation("e2", "parcial", 1, false, 5, "2026-10-01"));
        Subject a = conNotas("ma", "Alfa", 6, new Evaluation("e3", "parcial", 1, false, 8, "2026-10-01"));
        List<StudyLog> estudio = List.of(
                ciclo("l1", "mb", "x", "2026-10-29", "09:00", 10),
                ciclo("l2", "mb", "x", "2026-10-28", "09:00", 10),
                ciclo("l3", "ma", "x", "2026-10-29", "09:00", 10));
        List<ImportantDate> fechas = List.of(
                new ImportantDate("d2", "Z", "ma", "2026-12-01", "final", ""),
                new ImportantDate("d1", "Y", "ma", "2026-11-01", "parcial", ""));

        Map<String, Registro> r = construir(List.of(b, a), estudio, fechas);

        assertEquals(List.of(
                "est:mb:2026-10-28", "est:ma:2026-10-29", "est:mb:2026-10-29",
                "fec:d2", "fec:d1",
                "nota:mb:parcial1", "nota:mb:parcial2", "nota:ma:parcial1"),
                List.copyOf(r.keySet()));
    }

    // ---- Bordes ----

    @Test
    void minutosEstudiadosIncluyeElDiaDelEventoSiEsAnteriorAHoy() {
        List<Subject> materias = List.of(materia("m1", "Análisis"));
        List<StudyLog> estudio = List.of(
                ciclo("l1", "m1", "A", "2026-10-19", "09:00", 10),
                ciclo("l2", "m1", "B", "2026-10-20", "09:00", 20),
                ciclo("l3", "m1", "C", "2026-10-21", "09:00", 40));
        ImportantDate d = new ImportantDate("d1", "P", "m1", "2026-10-20", "parcial", "");

        Registro e = construir(materias, estudio, List.of(d)).get("fec:d1");

        assertEquals("{\"materia\":\"Análisis\",\"tipo\":\"parcial\",\"titulo\":\"P\",\"minutosEstudiados\":30}",
                e.detalleJson());
    }

    @Test
    void minutosEstudiadosIncluyeElDiaDeHoy() {
        List<Subject> materias = List.of(materia("m1", "Análisis"));
        List<StudyLog> estudio = List.of(
                ciclo("l1", "m1", "A", "2026-10-30", "09:00", 20),
                ciclo("l2", "m1", "B", "2026-10-31", "09:00", 40));
        ImportantDate d = new ImportantDate("d1", "P", "m1", "2026-12-01", "parcial", "");

        Registro e = construir(materias, estudio, List.of(d)).get("fec:d1");

        assertEquals("{\"materia\":\"Análisis\",\"tipo\":\"parcial\",\"titulo\":\"P\",\"minutosEstudiados\":20}",
                e.detalleJson());
    }

    @Test
    void fechasNoIsoSeOmiten() {
        Subject s = conNotas("m1", "Análisis", 6,
                new Evaluation("e1", "parcial", 1, false, 7, "2026-2-3"),
                new Evaluation("e2", "parcial", 2, false, 7, "2026-10-28T10:00:00"));
        Function<String, String> recordada = id -> "2026-10-28T10:00:00";
        List<StudyLog> estudio = List.of(
                ciclo("l1", "m1", "A", "2026-2-3", "09:00", 25),
                ciclo("l2", "m1", "B", "2026-10-28T10:00:00", "09:00", 25));
        List<ImportantDate> fechas = List.of(
                new ImportantDate("d1", "A", "m1", "2026-2-3", "parcial", ""),
                new ImportantDate("d2", "B", "m1", "2026-10-28T10:00:00", "parcial", ""));

        Map<String, Registro> r = RegistrosPanel.construir(List.of(s), estudio, fechas, HOY, "pc", recordada);

        assertTrue(r.isEmpty());
    }

    @Test
    void hoyNuloNoLanzaYElEventoCuentaHastaSuPropiaFecha() {
        List<Subject> materias = List.of(materia("m1", "Análisis"));
        List<StudyLog> estudio = List.of(
                ciclo("l1", "m1", "A", "2026-12-01", "09:00", 20),
                ciclo("l2", "m1", "B", "2026-12-02", "09:00", 40));
        ImportantDate d = new ImportantDate("d1", "P", "m1", "2026-12-01", "parcial", "");

        Map<String, Registro> r = RegistrosPanel.construir(materias, estudio, List.of(d), null, "pc", SIN_RECORDATORIO);

        // Sin "hoy" no hay tope por hoy: cuenta hasta la fecha del evento.
        assertEquals("{\"materia\":\"Análisis\",\"tipo\":\"parcial\",\"titulo\":\"P\",\"minutosEstudiados\":20}",
                r.get("fec:d1").detalleJson());
    }

    @Test
    void idDeMateriaConDosPuntosNoMezclaGrupos() {
        List<Subject> materias = List.of(materia("a", "Alfa"), materia("a:2026", "Beta"));
        List<StudyLog> estudio = List.of(
                ciclo("l1", "a", "x", "2026-10-28", "09:00", 10),
                ciclo("l2", "a:2026", "y", "2026-10-28", "09:00", 20),
                ciclo("l3", "a:2026", "z", "2026-10-29", "09:00", 30));

        Map<String, Registro> r = soloEstudio(materias, estudio);

        assertEquals(List.of("est:a:2026-10-28", "est:a:2026:2026-10-28", "est:a:2026:2026-10-29"),
                List.copyOf(r.keySet()));
        assertEquals(10, r.get("est:a:2026-10-28").duracionMin());
        assertEquals("Alfa · x", r.get("est:a:2026-10-28").resumen());
        assertEquals(20, r.get("est:a:2026:2026-10-28").duracionMin());
        assertEquals("Beta · y", r.get("est:a:2026:2026-10-28").resumen());
        assertEquals(30, r.get("est:a:2026:2026-10-29").duracionMin());
    }

    @Test
    void variosRecuperatoriosGanaElUltimo() {
        Subject s = conNotas("m1", "Análisis", 6,
                new Evaluation("e1", "parcial", 1, false, 9, "2026-09-10"),
                new Evaluation("e2", "parcial", 1, true, 3, "2026-09-20"),
                new Evaluation("e3", "parcial", 1, true, 5, "2026-09-30"));

        Registro n = soloNotas(s).get("nota:m1:parcial1");

        assertEquals(5.0, n.valor());
        assertEquals("2026-09-30", n.fecha());
        assertEquals("Parcial 1 (recuperatorio) · Análisis: 5", n.resumen());
    }

    @Test
    void variosNormalesSinRecuperatorioGanaElUltimo() {
        Subject s = conNotas("m1", "Análisis", 6,
                new Evaluation("e1", "parcial", 1, false, 9, "2026-09-10"),
                new Evaluation("e2", "parcial", 1, false, 4, "2026-09-20"));

        Registro n = soloNotas(s).get("nota:m1:parcial1");

        assertEquals(4.0, n.valor());
        assertEquals("2026-09-20", n.fecha());
        assertEquals("Parcial 1 · Análisis: 4", n.resumen());
    }

    @Test
    void formatoDeNotas() {
        Subject s = conNotas("m1", "Análisis", 6,
                new Evaluation("e0", "parcial", 1, false, 0, "2026-09-01"),
                new Evaluation("e1", "parcial", 2, false, 10, "2026-09-01"),
                new Evaluation("e2", "parcial", 3, false, 7, "2026-09-01"),
                new Evaluation("e3", "parcial", 4, false, 6.5, "2026-09-01"),
                new Evaluation("e4", "parcial", 5, false, 6.75, "2026-09-01"));

        Map<String, Registro> r = soloNotas(s);

        assertEquals("Parcial 1 · Análisis: 0", r.get("nota:m1:parcial1").resumen());
        assertEquals("Parcial 2 · Análisis: 10", r.get("nota:m1:parcial2").resumen());
        assertEquals("Parcial 3 · Análisis: 7", r.get("nota:m1:parcial3").resumen());
        assertEquals("Parcial 4 · Análisis: 6,5", r.get("nota:m1:parcial4").resumen());
        assertEquals("Parcial 5 · Análisis: 6,75", r.get("nota:m1:parcial5").resumen());
        assertEquals(0.0, r.get("nota:m1:parcial1").valor());
        assertEquals(10.0, r.get("nota:m1:parcial2").valor());
        assertEquals(7.0, r.get("nota:m1:parcial3").valor());
        assertEquals(6.5, r.get("nota:m1:parcial4").valor());
        assertEquals(6.75, r.get("nota:m1:parcial5").valor());
        assertEquals("{\"materia\":\"Análisis\",\"numero\":1,\"recuperatorio\":false,\"notaAprobacion\":6}",
                r.get("nota:m1:parcial1").detalleJson());

        Subject d = conNotas("m2", "Física", 5.5, new Evaluation("e5", "parcial", 1, false, 8, "2026-09-01"));
        assertEquals("{\"materia\":\"Física\",\"numero\":1,\"recuperatorio\":false,\"notaAprobacion\":5.5}",
                soloNotas(d).get("nota:m2:parcial1").detalleJson());
    }

    @Test
    void unaFuncionDeFechaQueLanzaSoloOmiteEsaNota() {
        Subject s = conNotas("m1", "Análisis", 6,
                new Evaluation("e1", "parcial", 1, false, 7, null),
                new Evaluation("e2", "parcial", 2, false, 8, "2026-10-15"));
        Function<String, String> lanza = id -> {
            throw new IllegalStateException("falla");
        };

        Map<String, Registro> r = RegistrosPanel.construir(List.of(s),
                List.of(ciclo("l1", "m1", "A", "2026-10-28", "09:00", 25)),
                List.of(new ImportantDate("d1", "P", "m1", "2026-11-05", "parcial", "")), HOY, "pc", lanza);

        assertEquals(List.of("est:m1:2026-10-28", "fec:d1", "nota:m1:parcial2"), List.copyOf(r.keySet()));
    }

    @Test
    void elJsonNoEscapaCaracteresHtml() {
        Subject s = conNotas("m1", "Mecánica & Fluidos <1>", 6,
                new Evaluation("e1", "parcial", 1, false, 7, "2026-10-15"));
        Map<String, Registro> r = construir(List.of(s),
                List.of(ciclo("l1", "m1", "A", "2026-10-28", "09:00", 25)),
                List.of(new ImportantDate("d1", "P", "m1", "2026-11-05", "parcial", "")));

        assertEquals("{\"materia\":\"Mecánica & Fluidos <1>\",\"temas\":[\"A\"],\"ciclos\":1}",
                r.get("est:m1:2026-10-28").detalleJson());
        assertTrue(r.get("fec:d1").detalleJson().startsWith("{\"materia\":\"Mecánica & Fluidos <1>\""));
        assertTrue(r.get("nota:m1:parcial1").detalleJson().startsWith("{\"materia\":\"Mecánica & Fluidos <1>\""));
    }

    // ---- completo ----

    @Test
    void completoEsVerdaderoConDatosNormalesEInvalidos() {
        List<StudyLog> estudio = new ArrayList<>();
        estudio.add(ciclo("l1", "m1", "A", "2026-10-28", "09:00", 25));
        estudio.add(ciclo("l2", "m1", "B", "no es fecha", "09:00", 25)); // inválido: se omite sin falla
        estudio.add(null);
        RegistrosPanel.Resultado r = RegistrosPanel.construirConEstado(List.of(materia("m1", "Análisis")), estudio,
                List.of(new ImportantDate("d1", "A", "m1", null, "parcial", "")), HOY, "pc", SIN_RECORDATORIO);
        assertTrue(r.completo());
        assertEquals(1, r.registros().size());
        assertEquals(RegistrosPanel.construir(List.of(materia("m1", "Análisis")), estudio, List.of(), HOY, "pc",
                SIN_RECORDATORIO), r.registros());
    }

    @Test
    void completoEsFalsoSiUnCicloLanzaUnaExcepcionInesperada() {
        StudyLog roto = new StudyLog("l9", "m1", null, "X", "2026-10-28", "09:00", 10) {
            @Override
            public int getMinutes() {
                throw new IllegalStateException("roto");
            }
        };
        RegistrosPanel.Resultado r = RegistrosPanel.construirConEstado(List.of(materia("m1", "Análisis")),
                List.of(ciclo("l1", "m1", "A", "2026-10-28", "09:00", 25), roto), List.of(), HOY, "pc",
                SIN_RECORDATORIO);
        assertFalse(r.completo());
        assertEquals(1, r.registros().size(), "lo demás se arma igual");
    }

    @Test
    void completoEsFalsoSiFallaLaIteracionDeUnaLista() {
        List<StudyLog> mala = new ArrayList<>() {
            @Override
            public java.util.Iterator<StudyLog> iterator() {
                throw new IllegalStateException("roto");
            }
        };
        RegistrosPanel.Resultado r = RegistrosPanel.construirConEstado(List.of(materia("m1", "Análisis")), mala,
                List.of(), HOY, "pc", SIN_RECORDATORIO);
        assertFalse(r.completo());
    }
}
