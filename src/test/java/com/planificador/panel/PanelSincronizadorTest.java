package com.planificador.panel;

import com.google.gson.Gson;
import com.planificador.model.CourseRules;
import com.planificador.model.Evaluation;
import com.planificador.model.ImportantDate;
import com.planificador.model.StudyLog;
import com.planificador.model.Subject;
import com.planificador.panel.PanelSincronizador.Resumen;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import panel.cliente.ConexionPanel;
import panel.cliente.PanelCompartido;
import panel.core.ColaLocal;
import panel.core.Registro;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PanelSincronizadorTest {

    private static final long AHORA = Instant.parse("2026-10-30T12:00:00Z").toEpochMilli();
    private static final long DIA = 86_400_000L;
    private static final Gson GSON = new Gson();

    /** Reloj mutable: se puede adelantar y también retroceder. */
    private static class Reloj extends Clock {
        long ms = AHORA;

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(ms);
        }
    }

    private static final class Datos implements DatosPlanificador {
        final List<Subject> materias = new ArrayList<>();
        final List<StudyLog> estudio = new ArrayList<>();
        final List<ImportantDate> fechas = new ArrayList<>();
        boolean confiable = true;

        public List<Subject> materias() {
            return new ArrayList<>(materias);
        }

        public List<StudyLog> estudio() {
            return new ArrayList<>(estudio);
        }

        public List<ImportantDate> fechas() {
            return new ArrayList<>(fechas);
        }

        public boolean confiable() {
            return confiable;
        }
    }

    @TempDir
    Path carpeta;

    private HttpServer servidor;
    private final List<String> cuerpos = new CopyOnWriteArrayList<>();
    private final List<AutoCloseable> porCerrar = new ArrayList<>();
    private final Reloj reloj = new Reloj();
    private final Datos datos = new Datos();
    private PanelCompartido panel;
    private ConexionPanel conexion;
    private EstadoPanel estado;
    private PanelSincronizador sincronizador;
    private int vistos;

    private static final Logger[] LOGGERS = {Logger.getLogger(PanelSincronizador.class.getName()),
            Logger.getLogger(EstadoPanel.class.getName())};

    @BeforeEach
    void preparar() throws Exception {
        for (Logger l : LOGGERS) l.setUseParentHandlers(false); // los avisos esperados no ensucian la salida
        servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        servidor.createContext("/", ex -> {
            cuerpos.add(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            ex.sendResponseHeaders(200, -1);
            ex.close();
        });
        servidor.start();
        ConexionPanel.guardarConfig(carpeta, "http://127.0.0.1:" + servidor.getAddress().getPort(),
                "token-de-prueba-123", "pc", true);
        conexion = ConexionPanel.abrir(carpeta);
        panel = new PanelCompartido(conexion);
        porCerrar.add(panel);
        estado = new EstadoPanel(carpeta.resolve("panel-estado.json"));
        sincronizador = new PanelSincronizador(panel, estado, datos, reloj);
        porCerrar.add(sincronizador::close);

        datos.materias.add(materia("m1", "Análisis"));
    }

    @AfterEach
    void limpiar() throws Exception {
        for (AutoCloseable c : porCerrar) c.close();
        servidor.stop(0);
        for (Logger l : LOGGERS) l.setUseParentHandlers(true);
    }

    // ---- ayudas ----

    private static Subject materia(String id, String nombre) {
        return new Subject(id, nombre, "#000000", "", "");
    }

    private static StudyLog ciclo(String id, String tema, String fecha, int minutos) {
        return new StudyLog(id, "m1", null, tema, fecha, "10:00", minutos);
    }

    private static Subject conNota(String id, String nombre, String fecha) {
        Subject s = materia(id, nombre);
        s.getOrCreateCourse().setRules(new CourseRules(75, 6, 8, 2, List.of(true, true), 0));
        s.getOrCreateCourse().getEvaluations().add(new Evaluation("e1", "parcial", 1, false, 7, fecha));
        return s;
    }

    private static void esperar(BooleanSupplier cond) throws InterruptedException {
        long limite = System.nanoTime() + 5_000_000_000L;
        while (!cond.getAsBoolean() && System.nanoTime() < limite) Thread.sleep(20);
    }

    /** Sincroniza, deja que la cola se vacíe y devuelve lo que llegó al servidor en esta pasada, por id. */
    private Map<String, Registro> sync(Resumen esperado) throws Exception {
        Resumen r = sincronizador.sincronizar();
        assertEquals(esperado, r);
        return recibidos(r.nuevos() + r.cambiados() + r.borrados());
    }

    private Map<String, Registro> recibidos(int cantidad) throws Exception {
        conexion.vaciarAhora();
        esperar(() -> cuerpos.size() >= vistos + cantidad && conexion.pendientes() == 0);
        assertEquals(vistos + cantidad, cuerpos.size(), cuerpos.toString());
        Map<String, Registro> porId = new LinkedHashMap<>();
        for (String b : cuerpos.subList(vistos, cuerpos.size())) {
            Registro reg = GSON.fromJson(b, Registro.class);
            assertEquals(null, porId.put(reg.idEnFuente(), reg), "id repetido en una pasada");
        }
        vistos = cuerpos.size();
        return porId;
    }

    /** Una fecha que no cambia: evita que lo deseado quede vacío (la guarda no borra en ese caso). */
    private void ancla() {
        datos.fechas.add(new ImportantDate("ancla", "Final", null, "2026-12-01", "final", ""));
    }

    private void llenarBasico() {
        datos.estudio.add(ciclo("l1", "Límites", "2026-10-29", 30));
        datos.estudio.add(ciclo("l2", "Derivadas", "2026-10-29", 20));
    }

    // ---- pruebas ----

    @Test
    void primeraVezMandaTodo() throws Exception {
        datos.materias.set(0, conNota("m1", "Análisis", null));
        llenarBasico();
        datos.fechas.add(new ImportantDate("f1", "Primer parcial", "m1", "2026-11-05", "parcial", ""));

        Map<String, Registro> enviados = sync(new Resumen(true, 3, 0, 0));

        assertEquals(List.of("est:m1:2026-10-29", "fec:f1", "nota:m1:parcial1"), List.copyOf(enviados.keySet()));
        for (Registro r : enviados.values()) {
            assertEquals("planificador", r.fuente());
            assertEquals("pc", r.dispositivo());
            assertEquals(AHORA, r.actualizadoEn());
            assertFalse(r.borrado());
        }
        assertEquals(50, enviados.get("est:m1:2026-10-29").duracionMin());
        assertEquals("2026-10-30", enviados.get("nota:m1:parcial1").fecha());
    }

    @Test
    void segundaVezNoMandaNada() throws Exception {
        llenarBasico();
        datos.fechas.add(new ImportantDate("f1", "Primer parcial", "m1", "2026-11-05", "parcial", ""));
        sync(new Resumen(true, 2, 0, 0));
        reloj.ms += 60_000;

        Map<String, Registro> segunda = sync(new Resumen(true, 0, 0, 0));

        assertTrue(segunda.isEmpty());
        assertEquals(2, cuerpos.size());
    }

    @Test
    void cambioEnUnGrupoSeReenviaConActualizadoEnMayor() throws Exception {
        llenarBasico();
        Registro antes = sync(new Resumen(true, 1, 0, 0)).get("est:m1:2026-10-29");
        reloj.ms += 5_000;
        datos.estudio.set(1, ciclo("l2", "Integrales", "2026-10-29", 20));

        Registro despues = sync(new Resumen(true, 0, 1, 0)).get("est:m1:2026-10-29");

        assertEquals(AHORA + 5_000, despues.actualizadoEn());
        assertTrue(despues.actualizadoEn() > antes.actualizadoEn());
        assertTrue(despues.resumen().contains("Integrales"), despues.resumen());
        assertEquals(50, despues.duracionMin());
    }

    @Test
    void agregarUnCicloCambiaElGrupoDelDia() throws Exception {
        llenarBasico();
        sync(new Resumen(true, 1, 0, 0));
        reloj.ms += 1_000;
        datos.estudio.add(ciclo("l3", "Series", "2026-10-29", 15));

        Registro r = sync(new Resumen(true, 0, 1, 0)).get("est:m1:2026-10-29");

        assertEquals(65, r.duracionMin());
        assertEquals(AHORA + 1_000, r.actualizadoEn());
        assertFalse(r.borrado());
    }

    @Test
    void borrarUnCicloDejaElGrupoConMenosMinutos() throws Exception {
        llenarBasico();
        sync(new Resumen(true, 1, 0, 0));
        reloj.ms += 1_000;
        datos.estudio.remove(1);

        Registro r = sync(new Resumen(true, 0, 1, 0)).get("est:m1:2026-10-29");

        assertEquals(30, r.duracionMin());
        assertFalse(r.borrado());
        assertEquals(AHORA + 1_000, r.actualizadoEn());
    }

    @Test
    void borrarElUltimoCicloDelDiaMandaMarcaDeBorrado() throws Exception {
        datos.estudio.add(ciclo("l1", "Límites", "2026-10-29", 30));
        ancla();
        Registro antes = sync(new Resumen(true, 2, 0, 0)).get("est:m1:2026-10-29");
        reloj.ms += 1_000;
        datos.estudio.clear();

        Registro r = sync(new Resumen(true, 0, 0, 1)).get("est:m1:2026-10-29");

        assertTrue(r.borrado());
        assertEquals(AHORA + 1_000, r.actualizadoEn());
        assertEquals(antes.resumen(), r.resumen());
        assertEquals(antes.duracionMin(), r.duracionMin());
        assertEquals("2026-10-29", r.fecha());
        assertTrue(estado.cargar().registros().get("est:m1:2026-10-29").borrado());
    }

    @Test
    void estudioQueQuedaEnCeroSeBorra() throws Exception {
        datos.estudio.add(ciclo("l1", "Límites", "2026-10-29", 30));
        ancla();
        sync(new Resumen(true, 2, 0, 0));
        reloj.ms += 1_000;
        datos.estudio.set(0, ciclo("l1", "Límites", "2026-10-29", 0));

        Registro r = sync(new Resumen(true, 0, 0, 1)).get("est:m1:2026-10-29");

        assertTrue(r.borrado());
    }

    @Test
    void borrarUnaMateriaBorraSusRegistros() throws Exception {
        datos.materias.set(0, conNota("m1", "Análisis", "2026-09-10"));
        llenarBasico();
        datos.fechas.add(new ImportantDate("f1", "Primer parcial", "m1", "2026-11-05", "parcial", ""));
        ancla();
        sync(new Resumen(true, 4, 0, 0));
        reloj.ms += 1_000;
        datos.materias.clear();
        datos.estudio.clear();
        datos.fechas.removeIf(f -> f.getId().equals("f1"));

        Map<String, Registro> marcas = sync(new Resumen(true, 0, 0, 3));

        assertEquals(List.of("est:m1:2026-10-29", "fec:f1", "nota:m1:parcial1"), List.copyOf(marcas.keySet()));
        for (Registro r : marcas.values()) assertTrue(r.borrado());
        assertEquals(3, estado.cargar().registros().values().stream().filter(Registro::borrado).count());
    }

    @Test
    void borrarUnaFechaMandaMarca() throws Exception {
        datos.fechas.add(new ImportantDate("f1", "Primer parcial", "m1", "2026-11-05", "parcial", ""));
        datos.fechas.add(new ImportantDate("f2", "TP 1", "m1", "2026-11-12", "tp", ""));
        sync(new Resumen(true, 2, 0, 0));
        reloj.ms += 1_000;
        datos.fechas.remove(0);

        Map<String, Registro> r = sync(new Resumen(true, 0, 0, 1));

        assertEquals(List.of("fec:f1"), List.copyOf(r.keySet()));
        assertTrue(r.get("fec:f1").borrado());
        assertEquals(List.of("fec:f2"), estado.cargar().registros().values().stream()
                .filter(x -> !x.borrado()).map(Registro::idEnFuente).toList());
    }

    @Test
    void borrarUnaNotaMandaMarca() throws Exception {
        datos.materias.set(0, conNota("m1", "Análisis", "2026-09-10"));
        sync(new Resumen(true, 1, 0, 0));
        reloj.ms += 1_000;
        datos.materias.get(0).getCourse().getEvaluations().clear();
        datos.fechas.add(new ImportantDate("f1", "Primer parcial", "m1", "2026-11-05", "parcial", ""));

        Registro r = sync(new Resumen(true, 1, 0, 1)).get("nota:m1:parcial1");

        assertTrue(r.borrado());
        assertEquals(7.0, r.valor());
    }

    @Test
    void panelInactivoNoTocaNada() throws Exception {
        Path otra = carpeta.resolve("sin-config");
        ConexionPanel inactiva = ConexionPanel.abrir(otra);
        porCerrar.add(inactiva);
        Clock quePrueba = new Clock() {
            public ZoneId getZone() {
                throw new AssertionError("no se debía leer el reloj");
            }

            public Clock withZone(ZoneId z) {
                throw new AssertionError("no se debía leer el reloj");
            }

            public Instant instant() {
                throw new AssertionError("no se debía leer el reloj");
            }
        };
        DatosPlanificador quePrueba2 = new DatosPlanificador() {
            public List<Subject> materias() {
                throw new AssertionError("no se debían leer los datos");
            }

            public List<StudyLog> estudio() {
                throw new AssertionError("no se debían leer los datos");
            }

            public List<ImportantDate> fechas() {
                throw new AssertionError("no se debían leer los datos");
            }

            public boolean confiable() {
                throw new AssertionError("no se debían leer los datos");
            }
        };
        Path archivoEstado = otra.resolve("panel-estado.json");
        PanelSincronizador s = new PanelSincronizador(new PanelCompartido(inactiva), new EstadoPanel(archivoEstado),
                quePrueba2, quePrueba);

        assertEquals(new Resumen(false, 0, 0, 0), s.sincronizar());
        assertEquals(new Resumen(false, 0, 0, 0),
                new PanelSincronizador(new PanelCompartido(null), new EstadoPanel(archivoEstado), quePrueba2,
                        quePrueba).sincronizar());

        assertFalse(Files.exists(archivoEstado));
        assertFalse(Files.exists(otra.resolve("panel-cola.jsonl")));
        assertFalse(Files.exists(otra));
    }

    @Test
    void estadoDanadoSeTrataComoVacio() throws Exception {
        llenarBasico();
        sync(new Resumen(true, 1, 0, 0));
        Files.writeString(estado.archivo(), "{ esto no es json");

        Map<String, Registro> r = sync(new Resumen(true, 1, 0, 0));

        assertEquals(List.of("est:m1:2026-10-29"), List.copyOf(r.keySet()));
        assertEquals(1, estado.cargar().registros().size(), "el estado quedó reparado");
    }

    @Test
    void actualizadoEnNuncaRetrocede() throws Exception {
        datos.estudio.add(ciclo("l1", "Límites", "2026-10-29", 30));
        ancla();
        Registro a = sync(new Resumen(true, 2, 0, 0)).get("est:m1:2026-10-29");
        reloj.ms -= 10 * DIA;
        datos.estudio.add(ciclo("l2", "Derivadas", "2026-10-29", 20));

        Registro b = sync(new Resumen(true, 0, 1, 0)).get("est:m1:2026-10-29");
        reloj.ms -= DIA;
        datos.estudio.clear();
        Registro c = sync(new Resumen(true, 0, 0, 1)).get("est:m1:2026-10-29");

        assertEquals(a.actualizadoEn() + 1, b.actualizadoEn());
        assertEquals(b.actualizadoEn() + 1, c.actualizadoEn());
        assertTrue(c.borrado());
    }

    @Test
    void unaExcepcionNoSePropaga() throws Exception {
        List<String> mensajes = new ArrayList<>();
        Handler h = new Handler() {
            public void publish(LogRecord r) {
                mensajes.add(r.getMessage() + " " + java.util.Arrays.toString(r.getParameters()));
            }

            public void flush() {
            }

            public void close() {
            }
        };
        Logger log = Logger.getLogger(PanelSincronizador.class.getName());
        log.addHandler(h);
        try {
            DatosPlanificador roto = new DatosPlanificador() {
                public List<Subject> materias() {
                    throw new IllegalStateException("token-secreto-123");
                }

                public List<StudyLog> estudio() {
                    return List.of();
                }

                public List<ImportantDate> fechas() {
                    return List.of();
                }

                public boolean confiable() {
                    return true;
                }
            };
            PanelSincronizador s = new PanelSincronizador(panel, estado, roto, reloj);

            Resumen r = assertDoesNotThrow(s::sincronizar);

            assertEquals(new Resumen(true, 0, 0, 0), r);
            assertFalse(mensajes.isEmpty());
            for (String m : mensajes) assertFalse(m.contains("token-secreto-123"), m);
        } finally {
            log.removeHandler(h);
        }
    }

    @Test
    void notaSinFechaRecuerdaLaPrimeraFecha() throws Exception {
        datos.materias.set(0, conNota("m1", "Análisis", null));
        Registro uno = sync(new Resumen(true, 1, 0, 0)).get("nota:m1:parcial1");
        assertEquals("2026-10-30", uno.fecha());
        reloj.ms += 6 * DIA;

        sync(new Resumen(true, 0, 0, 0));
        datos.estudio.add(ciclo("l1", "Límites", "2026-11-03", 30));
        sync(new Resumen(true, 1, 0, 0));

        assertEquals(Map.of("nota:m1:parcial1", "2026-10-30"), estado.cargar().fechasNotas());
        assertEquals("2026-10-30", estado.cargar().registros().get("nota:m1:parcial1").fecha());
    }

    @Test
    void reenviarTodoTrasVaciar() throws Exception {
        datos.materias.set(0, conNota("m1", "Análisis", null));
        llenarBasico();
        sync(new Resumen(true, 2, 0, 0));
        reloj.ms += 3 * DIA;

        estado.vaciar();
        Map<String, Registro> r = sync(new Resumen(true, 2, 0, 0));

        assertEquals("2026-10-30", r.get("nota:m1:parcial1").fecha());
        assertEquals(AHORA + 3 * DIA, r.get("est:m1:2026-10-29").actualizadoEn());
    }

    @Test
    void elEstadoSeGuardaDespuesDePublicar() throws Exception {
        servidor.stop(0); // sin red: lo publicado se queda en la cola
        llenarBasico();
        datos.fechas.add(new ImportantDate("f1", "Primer parcial", "m1", "2026-11-05", "parcial", ""));

        assertEquals(new Resumen(true, 2, 0, 0), sincronizador.sincronizar());

        Map<String, Registro> guardado = estado.cargar().registros();
        List<Registro> cola = new ColaLocal(carpeta.resolve("panel-cola.jsonl")).pendientes();
        assertEquals(2, cola.size());
        assertEquals(2, guardado.size());
        for (Registro r : cola) {
            assertNotNull(guardado.get(r.idEnFuente()));
            assertEquals(r, guardado.get(r.idEnFuente()));
        }
    }

    // ---- guardas contra el borrado masivo ----

    private Map<String, Registro> conEstudioYFecha() throws Exception {
        llenarBasico();
        datos.fechas.add(new ImportantDate("f1", "Primer parcial", "m1", "2026-11-05", "parcial", ""));
        return sync(new Resumen(true, 2, 0, 0));
    }

    private List<String> idsVivosEnElEstado() {
        return estado.cargar().registros().values().stream().filter(x -> !x.borrado())
                .map(Registro::idEnFuente).sorted().toList();
    }

    @Test
    void conLaCargaDanadaNoSeBorraNada() throws Exception {
        conEstudioYFecha();
        reloj.ms += 1_000;
        datos.confiable = false;
        datos.estudio.clear();
        datos.fechas.clear();
        datos.fechas.add(new ImportantDate("f2", "TP 1", "m1", "2026-11-12", "tp", ""));

        Map<String, Registro> r = sync(new Resumen(true, 1, 0, 0));

        assertEquals(List.of("fec:f2"), List.copyOf(r.keySet()));
        assertFalse(r.get("fec:f2").borrado());
    }

    @Test
    void siConstruirFallaNoSeBorraNada() throws Exception {
        conEstudioYFecha();
        reloj.ms += 1_000;
        StudyLog roto = new StudyLog("l9", "m1", null, "X", "2026-10-29", "09:00", 10) {
            @Override
            public int getMinutes() {
                throw new IllegalStateException("roto");
            }
        };
        datos.estudio.add(roto);
        datos.fechas.clear();

        Map<String, Registro> r = sync(new Resumen(true, 0, 0, 0));

        assertTrue(r.isEmpty());
        assertEquals(List.of("est:m1:2026-10-29", "fec:f1"), idsVivosEnElEstado());
    }

    @Test
    void sinNadaDeseadoNoSeBorraLoYaEnviado() throws Exception {
        conEstudioYFecha();
        reloj.ms += 1_000;
        datos.estudio.clear();
        datos.fechas.clear();

        Map<String, Registro> r = sync(new Resumen(true, 0, 0, 0));

        assertTrue(r.isEmpty());
        assertEquals(2, cuerpos.size());
    }

    @Test
    void losIdsNoBorradosPorLaGuardiaSiguenEnElEstado() throws Exception {
        Map<String, Registro> primera = conEstudioYFecha();
        datos.confiable = false;
        datos.estudio.clear();
        datos.fechas.clear();
        sync(new Resumen(true, 0, 0, 0));

        assertEquals(primera, estado.cargar().registros());
    }

    @Test
    void trasRecuperarLaConfianzaSeBorraLoQueFalta() throws Exception {
        conEstudioYFecha();
        datos.confiable = false;
        datos.estudio.clear();
        datos.fechas.clear();
        datos.fechas.add(new ImportantDate("f2", "TP 1", "m1", "2026-11-12", "tp", ""));
        sync(new Resumen(true, 1, 0, 0));
        reloj.ms += 1_000;
        datos.confiable = true;

        Map<String, Registro> r = sync(new Resumen(true, 0, 0, 2));

        assertEquals(List.of("est:m1:2026-10-29", "fec:f1"), List.copyOf(r.keySet()).stream().sorted().toList());
        for (Registro x : r.values()) assertTrue(x.borrado());
        assertEquals(List.of("fec:f2"), idsVivosEnElEstado());
    }

    // ---- marcas de borrado recordadas ----

    @Test
    void reaparecerDespuesDeBorrarUsaUnaMarcaDeTiempoMayor() throws Exception {
        datos.estudio.add(ciclo("l1", "Límites", "2026-10-29", 30));
        ancla();
        sync(new Resumen(true, 2, 0, 0));
        reloj.ms += 1_000;
        datos.estudio.clear();
        Registro marca = sync(new Resumen(true, 0, 0, 1)).get("est:m1:2026-10-29");
        reloj.ms -= DIA; // el reloj retrocede entre el borrado y el regreso
        datos.estudio.add(ciclo("l1", "Límites", "2026-10-29", 30));

        Registro vuelta = sync(new Resumen(true, 1, 0, 0)).get("est:m1:2026-10-29");

        assertFalse(vuelta.borrado());
        assertEquals(marca.actualizadoEn() + 1, vuelta.actualizadoEn());
    }

    @Test
    void reaparecerCuentaComoNuevo() throws Exception {
        datos.fechas.add(new ImportantDate("f1", "Primer parcial", "m1", "2026-11-05", "parcial", ""));
        ancla();
        sync(new Resumen(true, 2, 0, 0));
        datos.fechas.remove(0);
        sync(new Resumen(true, 0, 0, 1));
        datos.fechas.add(0, new ImportantDate("f1", "Primer parcial", "m1", "2026-11-05", "parcial", ""));

        Resumen r = sincronizador.sincronizar();

        assertEquals(new Resumen(true, 1, 0, 0), r);
    }

    @Test
    void unaMarcaYaPublicadaNoSeRepite() throws Exception {
        datos.estudio.add(ciclo("l1", "Límites", "2026-10-29", 30));
        ancla();
        sync(new Resumen(true, 2, 0, 0));
        reloj.ms += 1_000;
        datos.estudio.clear();
        sync(new Resumen(true, 0, 0, 1));
        reloj.ms += 1_000;

        Map<String, Registro> otra = sync(new Resumen(true, 0, 0, 0));

        assertTrue(otra.isEmpty());
        assertEquals(3, cuerpos.size());
        assertTrue(estado.cargar().registros().get("est:m1:2026-10-29").borrado());
    }

    @Test
    void lasMarcasViejasSeDescartanDelEstado() throws Exception {
        datos.estudio.add(ciclo("l1", "Límites", "2026-10-29", 30));
        ancla();
        sync(new Resumen(true, 2, 0, 0));
        datos.estudio.clear();
        sync(new Resumen(true, 0, 0, 1)); // marca en AHORA
        reloj.ms += 29 * DIA;
        sync(new Resumen(true, 0, 0, 0));
        assertTrue(estado.cargar().registros().containsKey("est:m1:2026-10-29"), "a los 29 días sigue");

        reloj.ms += 2 * DIA; // 31 días desde la marca
        sync(new Resumen(true, 0, 0, 0));

        assertFalse(estado.cargar().registros().containsKey("est:m1:2026-10-29"), "a los 31 días se descarta");
        assertTrue(estado.cargar().registros().containsKey("fec:ancla"));
    }

    @Test
    void fechasDeNotasQueYaNoExistenSeDescartan() throws Exception {
        datos.materias.set(0, conNota("m1", "Análisis", null));
        ancla();
        sync(new Resumen(true, 2, 0, 0));
        assertEquals(Map.of("nota:m1:parcial1", "2026-10-30"), estado.cargar().fechasNotas());
        datos.materias.get(0).getCourse().getEvaluations().clear();

        sync(new Resumen(true, 0, 0, 1));

        assertTrue(estado.cargar().fechasNotas().isEmpty());
    }

    // ---- ejecución fuera del hilo del llamador ----

    private static long hilosDelSincronizador() {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(t -> t.isAlive() && t.getName().equals("panel-sync-planificador")).count();
    }

    @Test
    void sincronizarEsTomarMasAplicar() throws Exception {
        llenarBasico();
        datos.fechas.add(new ImportantDate("f1", "Primer parcial", "m1", "2026-11-05", "parcial", ""));

        PanelSincronizador.Instantanea foto = sincronizador.tomar();
        datos.estudio.clear(); // lo que cambie después de tomar no cuenta
        datos.fechas.clear();
        Resumen r = sincronizador.aplicar(foto);

        assertEquals(new Resumen(true, 2, 0, 0), r);
        assertEquals(2, recibidos(2).size());
        // y sincronizar() sigue siendo tomar + aplicar sobre los datos de ahora: ahora no hay nada y se borra todo
        datos.estudio.add(ciclo("l9", "Otro", "2026-10-28", 10));
        assertEquals(new Resumen(true, 1, 0, 2), sincronizador.sincronizar());
    }

    @Test
    void lasCorridasAsincronasSeSerializan() throws Exception {
        llenarBasico();
        List<String> orden = new CopyOnWriteArrayList<>();
        List<String> hilos = new CopyOnWriteArrayList<>();
        List<Boolean> demonios = new CopyOnWriteArrayList<>();
        CountDownLatch suelta = new CountDownLatch(1);
        CountDownLatch primeraEmpezo = new CountDownLatch(1);

        sincronizador.sincronizarAsync(res -> {
            primeraEmpezo.countDown();
            try {
                suelta.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            orden.add("1:" + res.nuevos());
            hilos.add(Thread.currentThread().getName());
            demonios.add(Thread.currentThread().isDaemon());
        });
        assertTrue(primeraEmpezo.await(5, TimeUnit.SECONDS));
        for (int i = 2; i <= 3; i++) {
            int n = i;
            sincronizador.sincronizarAsync(res -> {
                orden.add(n + ":" + res.nuevos());
                hilos.add(Thread.currentThread().getName());
            });
        }
        Thread.sleep(100);
        assertTrue(orden.isEmpty(), "la segunda no corre mientras la primera no termina");
        suelta.countDown();
        esperar(() -> orden.size() == 3);

        assertEquals(List.of("1:1", "2:0", "3:0"), orden);
        assertEquals(List.of("panel-sync-planificador", "panel-sync-planificador", "panel-sync-planificador"), hilos);
        assertEquals(List.of(true), demonios);
        assertEquals(1, hilosDelSincronizador());
    }

    @Test
    void unaCorridaAsincronaQueLanzaNoMataElEjecutor() throws Exception {
        llenarBasico();
        List<String> orden = new CopyOnWriteArrayList<>();
        sincronizador.sincronizarAsync(res -> {
            orden.add("lanza");
            throw new IllegalStateException("falla del oyente");
        });
        sincronizador.sincronizarAsync(res -> orden.add("sigue"));
        esperar(() -> orden.size() == 2);

        assertEquals(List.of("lanza", "sigue"), orden);
        assertEquals(1, hilosDelSincronizador());
    }

    @Test
    void inactivoNoCreaElHiloNiTomaLaInstantanea() throws Exception {
        sincronizador.close();
        esperar(() -> hilosDelSincronizador() == 0);
        assertEquals(0, hilosDelSincronizador());
        Path otra = carpeta.resolve("sin-config-async");
        DatosPlanificador quePrueba = new DatosPlanificador() {
            public List<Subject> materias() {
                throw new AssertionError("no se debían leer los datos");
            }

            public List<StudyLog> estudio() {
                throw new AssertionError("no se debían leer los datos");
            }

            public List<ImportantDate> fechas() {
                throw new AssertionError("no se debían leer los datos");
            }

            public boolean confiable() {
                throw new AssertionError("no se debían leer los datos");
            }
        };
        PanelSincronizador s = new PanelSincronizador(new PanelCompartido(ConexionPanel.abrir(otra)),
                new EstadoPanel(otra.resolve("panel-estado.json")), quePrueba, reloj);
        List<Resumen> vistos2 = new CopyOnWriteArrayList<>();

        s.sincronizarAsync();
        s.sincronizarAsync(vistos2::add);
        Thread.sleep(100);

        assertEquals(0, hilosDelSincronizador());
        assertTrue(vistos2.isEmpty());
        assertFalse(Files.exists(otra));
        s.close(); // sin hilo creado, cerrar no hace nada
    }

    // ---- estado serializado e instantánea inmutable ----

    /** Reloj que, desde el hilo del sincronizador asíncrono, se queda esperando dentro de aplicar (al publicar). */
    private static final class RelojQueBloquea extends Reloj {
        volatile CountDownLatch entro;
        volatile CountDownLatch suelta;

        @Override
        public long millis() {
            CountDownLatch e = entro;
            if (e != null && Thread.currentThread().getName().equals("panel-sync-planificador")) {
                e.countDown();
                try {
                    suelta.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
            return super.millis();
        }
    }

    /** Sincronizador propio con un reloj que bloquea; el estado ya tiene lo básico enviado. */
    private PanelSincronizador conRelojQueBloquea(RelojQueBloquea r) throws Exception {
        PanelSincronizador s = new PanelSincronizador(panel, estado, datos, r);
        porCerrar.add(s::close);
        llenarBasico();
        datos.fechas.add(new ImportantDate("f1", "Primer parcial", "m1", "2026-11-05", "parcial", ""));
        assertEquals(new Resumen(true, 2, 0, 0), s.sincronizar());
        assertEquals(2, recibidos(2).size());
        datos.estudio.add(ciclo("l3", "Integrales", "2026-10-29", 15)); // un cambio: la corrida en curso publica 1
        return s;
    }

    @Test
    void reenviarTodoNoSePierdeSiHayUnaCorridaEnCurso() throws Exception {
        RelojQueBloquea r = new RelojQueBloquea();
        PanelSincronizador s = conRelojQueBloquea(r);
        r.entro = new CountDownLatch(1);
        r.suelta = new CountDownLatch(1);
        s.sincronizarAsync();
        assertTrue(r.entro.await(5, TimeUnit.SECONDS), "la corrida asíncrona quedó a mitad de aplicar");

        PanelSincronizador.Instantanea foto = s.tomar();
        java.util.concurrent.atomic.AtomicReference<Resumen> resultado = new java.util.concurrent.atomic.AtomicReference<>();
        Thread t = new Thread(() -> resultado.set(s.reenviarTodo(foto)));
        t.start();
        Thread.sleep(150);
        assertTrue(t.isAlive(), "reenviarTodo espera a que termine la corrida en curso");
        r.suelta.countDown();
        t.join(5000);
        assertFalse(t.isAlive());

        // la corrida en curso no pisó el borrado: se reenviaron los 2 registros como nuevos
        assertEquals(new Resumen(true, 2, 0, 0), resultado.get());
        assertEquals(2, estado.cargar().registros().values().stream().filter(x -> !x.borrado()).count());
        assertEquals(65, estado.cargar().registros().get("est:m1:2026-10-29").duracionMin());
    }

    @Test
    void olvidarEstadoSeSerializaConAplicar() throws Exception {
        RelojQueBloquea r = new RelojQueBloquea();
        PanelSincronizador s = conRelojQueBloquea(r);
        r.entro = new CountDownLatch(1);
        r.suelta = new CountDownLatch(1);
        s.sincronizarAsync();
        assertTrue(r.entro.await(5, TimeUnit.SECONDS));

        Thread t = new Thread(s::olvidarEstado);
        t.start();
        Thread.sleep(150);
        assertTrue(t.isAlive(), "olvidarEstado espera a la corrida en curso");
        r.suelta.countDown();
        t.join(5000);

        assertFalse(t.isAlive());
        assertTrue(estado.cargar().registros().isEmpty(), "el borrado llegó después y no lo pisó el guardado");
    }

    @Test
    void laInstantaneaNoDependeDeLosObjetosMutables() throws Exception {
        llenarBasico();

        PanelSincronizador.Instantanea foto = sincronizador.tomar();
        datos.materias.get(0).setName("Otro nombre");
        datos.estudio.clear();
        Resumen r = sincronizador.aplicar(foto);

        assertEquals(new Resumen(true, 1, 0, 0), r);
        Registro reg = recibidos(1).get("est:m1:2026-10-29");
        assertEquals("Análisis · Límites, Derivadas", reg.resumen());
        assertEquals(50, reg.duracionMin());
    }

    @Test
    void tomarNoLeeElArchivoDeEstadoDespuesDeLaPrimeraVez() throws Exception {
        datos.materias.set(0, conNota("m1", "Análisis", null));
        estado.guardar(new EstadoPanel.Estado(new LinkedHashMap<>(),
                new LinkedHashMap<>(Map.of("nota:m1:parcial1", "2026-01-01"))));

        PanelSincronizador.Instantanea primera = sincronizador.tomar();
        estado.guardar(new EstadoPanel.Estado(new LinkedHashMap<>(),
                new LinkedHashMap<>(Map.of("nota:m1:parcial1", "2026-02-02"))));
        PanelSincronizador.Instantanea segunda = sincronizador.tomar();
        Files.delete(estado.archivo());
        Files.createDirectory(estado.archivo()); // ni siquiera es un archivo
        PanelSincronizador.Instantanea tercera = sincronizador.tomar();

        for (PanelSincronizador.Instantanea f : List.of(primera, segunda, tercera)) {
            assertEquals("2026-01-01", f.deseado().get("nota:m1:parcial1").fecha());
        }
    }

    @Test
    void fechaRecordadaSeUsaEnTomar() throws Exception {
        datos.materias.set(0, conNota("m1", "Análisis", null));
        estado.guardar(new EstadoPanel.Estado(new LinkedHashMap<>(),
                new LinkedHashMap<>(Map.of("nota:m1:parcial1", "2026-01-01"))));

        PanelSincronizador.Instantanea foto = sincronizador.tomar();

        assertEquals("2026-01-01", foto.deseado().get("nota:m1:parcial1").fecha());
        assertTrue(foto.fechasNuevas().isEmpty());
    }

    @Test
    void fechaNuevaSeRecuerdaDespuesDeAplicar() throws Exception {
        datos.materias.set(0, conNota("m1", "Análisis", null));

        PanelSincronizador.Instantanea foto = sincronizador.tomar();
        assertEquals(Map.of("nota:m1:parcial1", "2026-10-30"), foto.fechasNuevas());
        assertTrue(estado.cargar().fechasNotas().isEmpty(), "tomar no escribe nada");
        sincronizador.aplicar(foto);
        reloj.ms += 2 * DIA;
        PanelSincronizador.Instantanea otra = sincronizador.tomar();

        assertEquals("2026-10-30", otra.deseado().get("nota:m1:parcial1").fecha());
        assertTrue(otra.fechasNuevas().isEmpty());
        assertEquals(Map.of("nota:m1:parcial1", "2026-10-30"), estado.cargar().fechasNotas());
    }

    // ---- borrados masivos retenidos ----

    private void fechas20() {
        for (int i = 0; i < 20; i++) {
            datos.fechas.add(new ImportantDate("f" + i, "Fecha " + i, "m1", "2026-12-" + String.format("%02d", i + 1),
                    "parcial", ""));
        }
    }

    private void dejarSolo(int cuantas) {
        datos.fechas.subList(cuantas, datos.fechas.size()).clear();
    }

    @Test
    void borradosMasivosSeRetienen() throws Exception {
        fechas20();
        sync(new Resumen(true, 20, 0, 0));
        dejarSolo(5);

        Resumen r = sincronizador.sincronizar();

        assertEquals(new Resumen(true, 0, 0, 0, 15), r);
        assertEquals(15, sincronizador.borradosRetenidos());
        assertTrue(recibidos(0).isEmpty());
        assertEquals(20, estado.cargar().registros().values().stream().filter(x -> !x.borrado()).count());
    }

    @Test
    void bajoElUmbralSePublican() throws Exception {
        fechas20();
        sync(new Resumen(true, 20, 0, 0));
        dejarSolo(11); // 9 borrados: menos de 10 y de la mitad

        Resumen r = sincronizador.sincronizar();

        assertEquals(new Resumen(true, 0, 0, 9, 0), r);
        assertEquals(0, sincronizador.borradosRetenidos());
        assertEquals(9, recibidos(9).size());
    }

    @Test
    void lasNuevasYLasCambiadasSalenAunqueSeRetenganBorrados() throws Exception {
        fechas20();
        sync(new Resumen(true, 20, 0, 0));
        dejarSolo(5);
        datos.fechas.add(new ImportantDate("nueva", "Nueva", "m1", "2026-12-30", "final", ""));

        Resumen r = sincronizador.sincronizar();

        assertEquals(new Resumen(true, 1, 0, 0, 15), r);
        assertEquals(1, recibidos(1).size());
    }

    @Test
    void confirmarBorradosLosPublica() throws Exception {
        fechas20();
        sync(new Resumen(true, 20, 0, 0));
        dejarSolo(5);
        assertEquals(15, sincronizador.sincronizar().retenidos());

        Resumen r = sincronizador.confirmarBorrados(sincronizador.tomar());

        assertEquals(new Resumen(true, 0, 0, 15, 0), r);
        assertEquals(0, sincronizador.borradosRetenidos());
        assertEquals(15, recibidos(15).size());
    }

    @Test
    void confirmarBorradosRespetaLasOtrasGuardas() throws Exception {
        fechas20();
        sync(new Resumen(true, 20, 0, 0));
        dejarSolo(5);
        datos.confiable = false;
        assertEquals(0, sincronizador.confirmarBorrados(sincronizador.tomar()).borrados());
        datos.confiable = true;
        datos.fechas.clear();
        assertEquals(0, sincronizador.confirmarBorrados(sincronizador.tomar()).borrados());

        assertTrue(recibidos(0).isEmpty());
        assertEquals(20, estado.cargar().registros().values().stream().filter(x -> !x.borrado()).count());
    }

    @Test
    void losRetenidosSeVuelvenAIntentarEnLaSiguientePasada() throws Exception {
        fechas20();
        sync(new Resumen(true, 20, 0, 0));
        dejarSolo(5);

        assertEquals(15, sincronizador.sincronizar().retenidos());
        assertEquals(15, sincronizador.sincronizar().retenidos());
        assertEquals(15, sincronizador.borradosRetenidos());
        assertTrue(recibidos(0).isEmpty());
        datos.fechas.clear();
        fechas20(); // vuelven todas: ya no hay nada que retener
        assertEquals(new Resumen(true, 0, 0, 0, 0), sincronizador.sincronizar());
        assertEquals(0, sincronizador.borradosRetenidos());
    }

    // ---- dirección del servidor ----

    private String direccionActual() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    @Test
    void elEstadoGuardaLaDireccion() throws Exception {
        llenarBasico();
        sync(new Resumen(true, 1, 0, 0));

        assertEquals(direccionActual(), estado.cargar().direccion());
    }

    @Test
    void cambioDeDireccionReenviaTodo() throws Exception {
        llenarBasico();
        ancla();
        sync(new Resumen(true, 2, 0, 0));
        EstadoPanel.Estado e = estado.cargar();
        estado.guardar(new EstadoPanel.Estado(e.registros(), e.fechasNotas(), "http://otro-servidor:1234"));

        Map<String, Registro> reenviados = sync(new Resumen(true, 2, 0, 0));

        assertEquals(2, reenviados.size());
        assertEquals(direccionActual(), estado.cargar().direccion());
    }

    @Test
    void cambioDeDireccionDescartaLasMarcasDeBorrado() throws Exception {
        llenarBasico();
        ancla();
        sync(new Resumen(true, 2, 0, 0));
        datos.estudio.clear();
        sync(new Resumen(true, 0, 0, 1));
        EstadoPanel.Estado e = estado.cargar();
        estado.guardar(new EstadoPanel.Estado(e.registros(), e.fechasNotas(), "http://otro-servidor:1234"));

        sync(new Resumen(true, 1, 0, 0)); // solo la fecha ancla; el estudio borrado no se reenvía

        assertTrue(estado.cargar().registros().values().stream().noneMatch(Registro::borrado));
    }

    @Test
    void estadoSinDireccionAdoptaLaActualSinBorrar() throws Exception {
        llenarBasico();
        sync(new Resumen(true, 1, 0, 0));
        EstadoPanel.Estado e = estado.cargar();
        estado.guardar(new EstadoPanel.Estado(e.registros(), e.fechasNotas(), null));

        sync(new Resumen(true, 0, 0, 0));

        assertEquals(direccionActual(), estado.cargar().direccion());
    }

    @Test
    void mismaDireccionConBarraFinalNoReenvia() throws Exception {
        llenarBasico();
        sync(new Resumen(true, 1, 0, 0));
        EstadoPanel.Estado e = estado.cargar();
        estado.guardar(new EstadoPanel.Estado(e.registros(), e.fechasNotas(),
                direccionActual().replace("http", "HTTP") + "/"));

        sync(new Resumen(true, 0, 0, 0));
    }

    @Test
    void activarEnLaMismaDireccionTrasUnCambioInactivoReenvia() throws Exception {
        llenarBasico();
        sync(new Resumen(true, 1, 0, 0));
        String a = direccionActual();
        // se guarda B con "Enviar" apagado: nada corre, el estado sigue diciendo A
        ConexionPanel.guardarConfig(carpeta, a + "/b", "token-de-prueba-123", "pc", false);
        panel.reabrir(() -> ConexionPanel.abrir(carpeta));
        assertEquals(new Resumen(false, 0, 0, 0), sincronizador.sincronizar());
        assertEquals(a, estado.cargar().direccion());
        // se vuelve a activar, ahora en B
        ConexionPanel.guardarConfig(carpeta, a + "/b", "token-de-prueba-123", "pc", true);
        panel.reabrir(() -> ConexionPanel.abrir(carpeta));

        Resumen r = sincronizador.sincronizar();

        assertEquals(new Resumen(true, 1, 0, 0), r);
        assertEquals(a + "/b", estado.cargar().direccion());
    }

    // ---- la conexión no cambia en medio de una corrida ----

    @Test
    void reabrirEsperaALaCorridaEnCurso() throws Exception {
        RelojQueBloquea r = new RelojQueBloquea();
        PanelSincronizador s = conRelojQueBloquea(r);
        r.entro = new CountDownLatch(1);
        r.suelta = new CountDownLatch(1);
        s.sincronizarAsync();
        assertTrue(r.entro.await(5, TimeUnit.SECONDS));
        java.util.concurrent.atomic.AtomicBoolean cambio = new java.util.concurrent.atomic.AtomicBoolean();

        Thread t = new Thread(() -> s.conConexionDetenida(() -> cambio.set(true)));
        t.start();
        Thread.sleep(150);
        assertFalse(cambio.get(), "el cambio espera a que termine la corrida");
        r.suelta.countDown();
        t.join(5000);

        assertTrue(cambio.get());
    }

    @Test
    void unaCorridaPosteriorUsaLaConexionNueva() throws Exception {
        llenarBasico();
        sincronizador.conConexionDetenida(() -> {
            try {
                ConexionPanel.guardarConfig(carpeta, direccionActual(), "token-de-prueba-123", "otra-pc", true);
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            panel.reabrir(() -> ConexionPanel.abrir(carpeta));
        });

        assertEquals(new Resumen(true, 1, 0, 0), sincronizador.sincronizar());
        panel.get().vaciarAhora(); // la conexión nueva, no la vieja del campo
        esperar(() -> !cuerpos.isEmpty());

        assertFalse(cuerpos.isEmpty());
        for (String b : cuerpos) assertEquals("otra-pc", GSON.fromJson(b, Registro.class).dispositivo());
    }

    // ---- instantáneas viejas ----

    @Test
    void unaInstantaneaViejaSeIgnora() throws Exception {
        llenarBasico();
        ancla();
        sync(new Resumen(true, 2, 0, 0));
        List<StudyLog> original = new ArrayList<>(datos.estudio);
        datos.estudio.clear();
        PanelSincronizador.Instantanea s1 = sincronizador.tomar(); // sin estudio
        datos.estudio.addAll(original);
        datos.estudio.add(ciclo("l3", "Integrales", "2026-10-29", 15));
        PanelSincronizador.Instantanea s2 = sincronizador.tomar();

        assertEquals(new Resumen(true, 0, 1, 0), sincronizador.aplicar(s2));
        assertEquals(new Resumen(true, 0, 0, 0), sincronizador.aplicar(s1));

        assertTrue(estado.cargar().registros().values().stream().noneMatch(Registro::borrado));
        assertEquals(65, estado.cargar().registros().get("est:m1:2026-10-29").duracionMin());
    }

    @Test
    void lasInstantaneasEnOrdenSeAplican() throws Exception {
        llenarBasico();
        ancla();
        sync(new Resumen(true, 2, 0, 0));
        datos.estudio.clear();
        PanelSincronizador.Instantanea s1 = sincronizador.tomar();
        datos.estudio.add(ciclo("l3", "Integrales", "2026-10-29", 15));
        PanelSincronizador.Instantanea s2 = sincronizador.tomar();

        assertEquals(new Resumen(true, 0, 0, 1), sincronizador.aplicar(s1));
        assertEquals(new Resumen(true, 1, 0, 0), sincronizador.aplicar(s2));
    }

    @Test
    void noSePodanLasFechasConDatosNoConfiables() throws Exception {
        datos.materias.set(0, conNota("m1", "Análisis", null));
        ancla();
        sync(new Resumen(true, 2, 0, 0));
        assertEquals(Map.of("nota:m1:parcial1", "2026-10-30"), estado.cargar().fechasNotas());
        datos.materias.get(0).getCourse().getEvaluations().clear();
        datos.confiable = false;

        sincronizador.sincronizar();

        assertEquals(Map.of("nota:m1:parcial1", "2026-10-30"), estado.cargar().fechasNotas());
    }
}
