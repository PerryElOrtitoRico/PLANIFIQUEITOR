package com.planificador.panel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.planificador.model.CourseRules;
import com.planificador.model.CourseTracking;
import com.planificador.model.Evaluation;
import com.planificador.model.ImportantDate;
import com.planificador.model.StudyLog;
import com.planificador.model.StudySession;
import com.planificador.model.Subject;
import com.planificador.panel.PanelSincronizador.Resumen;
import com.planificador.service.DataManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import panel.cliente.ConexionPanel;
import panel.cliente.PanelCompartido;
import panel.core.ColaLocal;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de extremo a extremo contra un panel de verdad (por ejemplo el de {@code scripts/demo.sh} del repo Panel).
 * Solo corre si están definidas PANEL_E2E_URL y PANEL_E2E_TOKEN; con {@code mvn test} común se omite en silencio.
 * Usa datos de ejemplo armados acá y carpetas temporales: nunca toca los datos reales.
 */
@EnabledIfEnvironmentVariable(named = "PANEL_E2E_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "PANEL_E2E_TOKEN", matches = ".+")
class PanelExtremoAExtremoManualTest {

    private static final String FQ = "s-fq";
    private static final String ALG = "s-alg";
    private static final String NOMBRE_FQ = "Física & Química";
    private static final String NOMBRE_ALG = "Álgebra";
    private static final String CLAVE_FQ = "fisica-quimica";

    @TempDir
    Path carpeta;

    private final HttpClient http = HttpClient.newHttpClient();
    private final String url = quitarBarra(System.getenv("PANEL_E2E_URL"));
    private final String token = System.getenv("PANEL_E2E_TOKEN");

    private static String quitarBarra(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static void esperar(BooleanSupplier cond) throws InterruptedException {
        long limite = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < limite) {
            if (cond.getAsBoolean()) return;
            Thread.sleep(20);
        }
    }

    private HttpResponse<String> enviar(HttpRequest.Builder b) {
        try {
            return http.send(b.header("Authorization", "Bearer " + token).build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private JsonElement get(String ruta) {
        var resp = enviar(HttpRequest.newBuilder(URI.create(url + ruta)).GET());
        assertEquals(200, resp.statusCode(), ruta + " -> " + resp.body());
        return JsonParser.parseString(resp.body());
    }

    /** Registros del Planificador de un día, tal cual los devuelve el panel (los borrados no aparecen). */
    private List<JsonObject> delDia(LocalDate fecha) {
        List<JsonObject> res = new ArrayList<>();
        for (JsonElement e : get("/api/dia/" + fecha).getAsJsonArray()) {
            JsonObject o = e.getAsJsonObject();
            if ("planificador".equals(o.get("fuente").getAsString())) res.add(o);
        }
        return res;
    }

    private JsonObject registro(LocalDate fecha, String idEnFuente) {
        return delDia(fecha).stream().filter(o -> idEnFuente.equals(o.get("idEnFuente").getAsString()))
                .findFirst().orElse(null);
    }

    private static JsonObject detalle(JsonObject registro) {
        return JsonParser.parseString(registro.get("detalleJson").getAsString()).getAsJsonObject();
    }

    private static List<String> textos(JsonArray a) {
        List<String> r = new ArrayList<>();
        a.forEach(e -> r.add(e.getAsString()));
        return r;
    }

    /** Ids (con repetidos, si los hubiera) de todo lo del Planificador en el rango, ordenados. */
    private List<String> idsEnRango(LocalDate desde, LocalDate hasta) {
        List<String> ids = new ArrayList<>();
        for (LocalDate f = desde; !f.isAfter(hasta); f = f.plusDays(1)) {
            delDia(f).forEach(o -> ids.add(o.get("idEnFuente").getAsString()));
        }
        return ids.stream().sorted().toList();
    }

    /** Los datos de ejemplo, cargados con la API real de DataManager; todas las fechas dentro de los últimos 5 días. */
    private StudySession armarDatos(LocalDate hoy) {
        DataManager.initForTests(carpeta.resolve("datos").resolve("planificador_datos.json"));
        DataManager dm = DataManager.getInstance();

        Subject fq = new Subject(FQ, NOMBRE_FQ, "#336699", "Prof. A", "lunes");
        CourseTracking curso = fq.getOrCreateCourse();
        curso.setRules(new CourseRules(75, 7, 8, 2, List.of(true, false), 30));
        curso.getEvaluations().add(new Evaluation("ev1", Evaluation.PARCIAL, 1, false, 8, hoy.minusDays(4).toString()));
        curso.getEvaluations().add(new Evaluation("ev1r", Evaluation.PARCIAL, 1, true, 5, hoy.minusDays(1).toString()));
        curso.getEvaluations().add(new Evaluation("ev2", Evaluation.PARCIAL, 2, false, 6, hoy.minusDays(2).toString()));
        dm.addSubject(fq);
        dm.addSubject(new Subject(ALG, NOMBRE_ALG, "#993366", "Prof. B", "martes"));

        // hoy-4: tres ciclos de Física & Química (un tema repetido) y uno de Álgebra
        String d1 = hoy.minusDays(4).toString();
        dm.addStudyLog(new StudyLog("l1", FQ, "t1", "Cinética & equilibrio", d1, "08:00", 25));
        dm.addStudyLog(new StudyLog("l2", FQ, "t2", "Termodinámica", d1, "09:00", 25));
        dm.addStudyLog(new StudyLog("l3", FQ, "t1", "Cinética & equilibrio", d1, "10:00", 25));
        dm.addStudyLog(new StudyLog("l4", ALG, "t3", "Matrices", d1, "11:00", 40));
        // hoy-2: un ciclo y un cronómetro suelto del mismo tema
        String d2 = hoy.minusDays(2).toString();
        dm.addStudyLog(new StudyLog("l5", FQ, "t4", "Gases", d2, "08:00", 25));
        StudyLog crono = new StudyLog("l6", FQ, "t4", "Gases", d2, "09:00", 30);
        crono.setSource("cronometro");
        dm.addStudyLog(crono);
        // hoy: una sesión de la guía marcada como hecha
        StudySession guia = new StudySession("g1", "lunes", ALG, "Ejercicios de la guía 4", 50, false);
        dm.addStudySession(guia);
        dm.setSessionCompleted(guia, true);

        dm.addImportantDate(new ImportantDate("f1", "Primer parcial", FQ, hoy.minusDays(3).toString(), "parcial", null));
        dm.addImportantDate(new ImportantDate("f2", "Final integrador", FQ, hoy.plusDays(7).toString(), "final", null));
        dm.addImportantDate(new ImportantDate("f3", "TP 2", ALG, hoy.plusDays(7).toString(), "tp", null));
        return guia;
    }

    @Test
    void sincronizaConsultaYBorraEnElPanel() throws Exception {
        LocalDate hoy = LocalDate.now();
        LocalDate f4 = hoy.minusDays(4), f3 = hoy.minusDays(3), f2 = hoy.minusDays(2), f1 = hoy.minusDays(1),
                futuro = hoy.plusDays(7);
        StudySession guia = armarDatos(hoy);
        DataManager dm = DataManager.getInstance();
        Path archivoDatos = dm.getDataFile();
        long idMeta = -1;

        Path conexionDir = Files.createDirectory(carpeta.resolve("conexion"));
        ConexionPanel.guardarConfig(conexionDir, url, token, "pc-prueba", true);
        try (ConexionPanel c = ConexionPanel.abrir(conexionDir); PanelCompartido panel = new PanelCompartido(c);
             PanelSincronizador sinc = new PanelSincronizador(panel, EstadoPanel.junto(archivoDatos),
                     new DatosDeDataManager(), Clock.systemDefaultZone())) {
            assertTrue(c.activo());

            var meta = enviar(HttpRequest.newBuilder(URI.create(url + "/api/metas"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"area\":\"estudio\",\"nombre\":\"Aprobar "
                            + NOMBRE_FQ + "\",\"metrica\":\"nota:" + CLAVE_FQ
                            + "\",\"valorPartida\":0,\"valorObjetivo\":7}")));
            assertEquals(200, meta.statusCode(), meta.body());
            idMeta = JsonParser.parseString(meta.body()).getAsJsonObject().get("id").getAsLong();
            try {
                // ---- primera sincronización: 4 días de estudio + 3 fechas + 2 notas
                assertEquals(new Resumen(true, 9, 0, 0), sinc.sincronizar());
                c.vaciarAhora();
                final LocalDate fHoy = hoy;
                esperar(() -> registro(f4, "est:s-fq:" + f4) != null && registro(f4, "est:s-alg:" + f4) != null
                        && registro(f2, "est:s-fq:" + f2) != null && registro(fHoy, "est:s-alg:" + fHoy) != null
                        && registro(f3, "fec:f1") != null && registro(futuro, "fec:f2") != null
                        && registro(futuro, "fec:f3") != null && registro(f1, "nota:s-fq:parcial1") != null
                        && registro(f2, "nota:s-fq:parcial2") != null);
                assertEquals(0, c.pendientes(), "no tiene que quedar nada en la cola");

                // estudio: un registro por materia y día, minutos sumados y temas distintos en orden
                JsonObject fq4 = registro(f4, "est:s-fq:" + f4);
                assertNotNull(fq4);
                assertEquals("esfuerzo", fq4.get("tipo").getAsString());
                assertEquals("estudio", fq4.get("area").getAsString());
                assertEquals(75, fq4.get("duracionMin").getAsInt());
                assertEquals("Física & Química · Cinética & equilibrio, Termodinámica", fq4.get("resumen").getAsString());
                assertEquals(NOMBRE_FQ, detalle(fq4).get("materia").getAsString());
                assertEquals(List.of("Cinética & equilibrio", "Termodinámica"), textos(detalle(fq4).getAsJsonArray("temas")));
                assertEquals(3, detalle(fq4).get("ciclos").getAsInt());
                JsonObject alg4 = registro(f4, "est:s-alg:" + f4);
                assertNotNull(alg4);
                assertEquals(40, alg4.get("duracionMin").getAsInt());
                assertEquals("Álgebra · Matrices", alg4.get("resumen").getAsString());
                assertEquals(2, delDia(f4).stream().filter(o -> o.get("idEnFuente").getAsString().startsWith("est:")).count());

                JsonObject fq2 = registro(f2, "est:s-fq:" + f2);
                assertNotNull(fq2);
                assertEquals(55, fq2.get("duracionMin").getAsInt());
                assertEquals("Física & Química · Gases", fq2.get("resumen").getAsString());
                assertEquals(List.of("Gases"), textos(detalle(fq2).getAsJsonArray("temas")));
                assertEquals(2, detalle(fq2).get("ciclos").getAsInt());

                JsonObject algHoy = registro(hoy, "est:s-alg:" + hoy);
                assertNotNull(algHoy);
                assertEquals(50, algHoy.get("duracionMin").getAsInt());
                assertEquals("Álgebra · Ejercicios de la guía 4", algHoy.get("resumen").getAsString());
                assertEquals(List.of("Ejercicios de la guía 4"), textos(detalle(algHoy).getAsJsonArray("temas")));

                // fechas importantes: métrica, minutos estudiados, y los dos eventos del mismo día
                JsonObject parcial = registro(f3, "fec:f1");
                assertNotNull(parcial);
                assertEquals("evento", parcial.get("tipo").getAsString());
                assertEquals("parcial", parcial.get("metrica").getAsString());
                assertEquals("Parcial · Física & Química · Primer parcial", parcial.get("resumen").getAsString());
                assertEquals(75, detalle(parcial).get("minutosEstudiados").getAsInt());
                JsonObject fin = registro(futuro, "fec:f2");
                JsonObject tp = registro(futuro, "fec:f3");
                assertNotNull(fin);
                assertNotNull(tp);
                assertEquals("final", fin.get("metrica").getAsString());
                assertEquals("tp", tp.get("metrica").getAsString());
                assertEquals("Final · Física & Química · Final integrador", fin.get("resumen").getAsString());
                assertEquals("TP · Álgebra · TP 2", tp.get("resumen").getAsString());
                assertEquals(130, detalle(fin).get("minutosEstudiados").getAsInt());
                assertEquals(90, detalle(tp).get("minutosEstudiados").getAsInt());
                assertEquals(2, delDia(futuro).stream().filter(o -> o.get("idEnFuente").getAsString().startsWith("fec:")).count());

                // notas: la nota efectiva (el recuperatorio, aunque sea más baja que la original)
                JsonObject p1 = registro(f1, "nota:s-fq:parcial1");
                JsonObject p2 = registro(f2, "nota:s-fq:parcial2");
                assertNotNull(p1);
                assertNotNull(p2);
                assertEquals("resultado", p1.get("tipo").getAsString());
                assertEquals("nota:" + CLAVE_FQ, p1.get("metrica").getAsString());
                assertEquals(5.0, p1.get("valor").getAsDouble(), 1e-9);
                assertEquals("Parcial 1 (recuperatorio) · Física & Química: 5", p1.get("resumen").getAsString());
                assertTrue(detalle(p1).get("recuperatorio").getAsBoolean());
                assertEquals(7, detalle(p1).get("notaAprobacion").getAsInt());
                assertEquals("nota:" + CLAVE_FQ, p2.get("metrica").getAsString());
                assertEquals(6.0, p2.get("valor").getAsDouble(), 1e-9);
                assertEquals("Parcial 2 · Física & Química: 6", p2.get("resumen").getAsString());
                assertEquals(0, delDia(f4).stream().filter(o -> o.get("idEnFuente").getAsString().equals("nota:s-fq:parcial1")).count(),
                        "la nota original (8) no se manda");

                // progreso de la meta: la mejor entre las notas efectivas (6), no la original de 8
                JsonObject progreso = progresoDe(idMeta);
                assertEquals(6.0, progreso.getAsJsonObject("progreso").get("actual").getAsDouble(), 1e-9);
                assertEquals(1.0, progreso.getAsJsonObject("progreso").get("falta").getAsDouble(), 1e-9);
                assertFalse(progreso.getAsJsonObject("progreso").get("cumplida").getAsBoolean());
                List<Double> serie = new ArrayList<>();
                progreso.getAsJsonArray("serie").forEach(e -> serie.add(e.getAsJsonObject().get("valor").getAsDouble()));
                assertEquals(List.of(5.0, 6.0), serie.stream().sorted().toList());

                // calendario de cada mes involucrado: las celdas de estudio y los eventos
                for (String mes : List.of(f4.toString().substring(0, 7), futuro.toString().substring(0, 7))) {
                    JsonObject m = get("/api/mes/" + mes).getAsJsonObject();
                    for (LocalDate f : List.of(f4, f2, hoy)) {
                        if (f.toString().startsWith(mes)) assertTrue(hayCeldaDeEstudio(m, f), "falta la celda de " + f);
                    }
                    for (LocalDate f : List.of(f3, futuro)) {
                        if (f.toString().startsWith(mes)) assertTrue(hayEvento(m, f), "falta el evento de " + f + " en " + m.get("eventos"));
                    }
                }

                // ---- segunda sincronización sin cambios: no manda nada y no hay duplicados
                List<String> antes = idsEnRango(f4, futuro);
                assertEquals(9, antes.size());
                assertEquals(9, antes.stream().distinct().count());
                assertEquals(new Resumen(true, 0, 0, 0), sinc.sincronizar());
                assertEquals(0, c.pendientes());
                c.vaciarAhora();
                assertEquals(antes, idsEnRango(f4, futuro));

                // ---- quitar un ciclo del historial actualiza ese día (y lo que cuenta lo estudiado)
                assertTrue(dm.getStudyLogs().removeIf(l -> l.getId().equals("l2")));
                dm.saveData();
                assertEquals(new Resumen(true, 0, 3, 0), sinc.sincronizar());
                c.vaciarAhora();
                esperar(() -> {
                    JsonObject r = registro(f4, "est:s-fq:" + f4);
                    return r != null && r.get("duracionMin").getAsInt() == 50;
                });
                fq4 = registro(f4, "est:s-fq:" + f4);
                assertEquals(50, fq4.get("duracionMin").getAsInt());
                assertEquals("Física & Química · Cinética & equilibrio", fq4.get("resumen").getAsString());
                assertEquals(List.of("Cinética & equilibrio"), textos(detalle(fq4).getAsJsonArray("temas")));
                assertEquals(2, detalle(fq4).get("ciclos").getAsInt());
                assertEquals(50, detalle(registro(f3, "fec:f1")).get("minutosEstudiados").getAsInt());
                assertEquals(105, detalle(registro(futuro, "fec:f2")).get("minutosEstudiados").getAsInt());
                assertEquals(antes, idsEnRango(f4, futuro), "el mismo conjunto de ids, sin duplicados");

                // ---- quitar el último ciclo del día borra el registro del día en el panel
                dm.setSessionCompleted(guia, false);
                assertEquals(new Resumen(true, 0, 1, 1), sinc.sincronizar());
                c.vaciarAhora();
                esperar(() -> registro(hoy, "est:s-alg:" + hoy) == null);
                assertEquals(null, registro(hoy, "est:s-alg:" + hoy));
                assertEquals(40, detalle(registro(futuro, "fec:f3")).get("minutosEstudiados").getAsInt());
                assertEquals(0, c.pendientes());
            } finally {
                enviar(HttpRequest.newBuilder(URI.create(url + "/api/metas/" + idMeta)).DELETE());
            }
        }
        assertTrue(Files.exists(EstadoPanel.junto(archivoDatos).archivo()));
    }

    private JsonObject progresoDe(long idMeta) {
        for (JsonElement e : get("/api/progreso").getAsJsonArray()) {
            JsonObject o = e.getAsJsonObject();
            if (o.getAsJsonObject("meta").get("id").getAsLong() == idMeta) return o;
        }
        throw new AssertionError("la meta " + idMeta + " no está en /api/progreso");
    }

    private static boolean hayCeldaDeEstudio(JsonObject mes, LocalDate fecha) {
        for (JsonElement ce : mes.getAsJsonArray("celdas")) {
            JsonObject o = ce.getAsJsonObject();
            if (fecha.toString().equals(o.get("fecha").getAsString()) && "estudio".equals(o.get("area").getAsString())) return true;
        }
        return false;
    }

    private static boolean hayEvento(JsonObject mes, LocalDate fecha) {
        for (JsonElement ce : mes.getAsJsonArray("eventos")) {
            if (ce.toString().contains(fecha.toString())) return true;
        }
        return false;
    }

    @Test
    void conElPanelCaidoTodoQuedaEnLaCola() throws Exception {
        LocalDate hoy = LocalDate.now();
        armarDatos(hoy);
        int puertoCerrado;
        try (ServerSocket s = new ServerSocket(0)) {
            puertoCerrado = s.getLocalPort();
        }
        Path conexionDir = Files.createDirectory(carpeta.resolve("conexion"));
        ConexionPanel.guardarConfig(conexionDir, "http://127.0.0.1:" + puertoCerrado, token, "pc-prueba", true);
        try (ConexionPanel c = ConexionPanel.abrir(conexionDir); PanelCompartido panel = new PanelCompartido(c);
             PanelSincronizador sinc = new PanelSincronizador(panel,
                     EstadoPanel.junto(DataManager.getInstance().getDataFile()), new DatosDeDataManager(),
                     Clock.systemDefaultZone())) {
            Resumen resumen = assertDoesNotThrow(sinc::sincronizar);
            assertEquals(new Resumen(true, 9, 0, 0), resumen);
            assertDoesNotThrow(c::vaciarAhora);
            assertEquals(9, c.pendientes());
        }
        List<String> ids = new ColaLocal(conexionDir.resolve("panel-cola.jsonl")).pendientes().stream()
                .map(r -> r.idEnFuente()).sorted().toList();
        assertEquals(List.of("est:s-alg:" + hoy, "est:s-alg:" + hoy.minusDays(4), "est:s-fq:" + hoy.minusDays(2),
                "est:s-fq:" + hoy.minusDays(4), "fec:f1", "fec:f2", "fec:f3", "nota:s-fq:parcial1",
                "nota:s-fq:parcial2").stream().sorted().toList(), ids);
        assertEquals(9, Files.readAllLines(conexionDir.resolve("panel-cola.jsonl")).stream().filter(l -> !l.isBlank()).count());
    }
}
