package com.planificador.panel;

import com.planificador.panel.EstadoPanel.Estado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import panel.core.Registro;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstadoPanelTest {

    @TempDir
    Path tmp;

    private final java.util.logging.Logger log = java.util.logging.Logger.getLogger(EstadoPanel.class.getName());

    @org.junit.jupiter.api.BeforeEach
    void callarAvisos() {
        log.setUseParentHandlers(false); // los avisos esperados no ensucian la salida
    }

    @org.junit.jupiter.api.AfterEach
    void restaurarAvisos() {
        log.setUseParentHandlers(true);
    }

    private static Registro completo() {
        return new Registro("esfuerzo", "planificador", "est:m1:2026-10-29", "estudio", "2026-10-29", 50,
                "Análisis · límites \"y\" <más>", "{\"materia\":\"Análisis\",\"ciclos\":2}", 3, "nota:analisis", 7.5,
                "pc", 1_700_000_000_123L, true);
    }

    private static Registro conNulos() {
        return new Registro("evento", "planificador", "fec:f1", "estudio", "2026-11-02", null,
                "Parcial", null, null, null, null, null, 0L, false);
    }

    @Test
    void ausenteEsVacio() {
        Estado e = new EstadoPanel(tmp.resolve("no/existe/panel-estado.json")).cargar();
        assertTrue(e.registros().isEmpty());
        assertTrue(e.fechasNotas().isEmpty());
    }

    @Test
    void danadoEsVacio() throws IOException {
        Path f = tmp.resolve("panel-estado.json");
        for (String basura : List.of("{ esto no es json", "[1,2,3]", "", "null", "{\"registros\": 5}",
                "{\"registros\": {\"a\": \"texto\"}}")) {
            Files.writeString(f, basura);
            Estado e = new EstadoPanel(f).cargar();
            assertTrue(e.registros().isEmpty(), basura);
            assertTrue(e.fechasNotas().isEmpty(), basura);
        }
    }

    @Test
    void idaYVueltaConservaTodosLosComponentesDelRegistro() {
        EstadoPanel estado = new EstadoPanel(tmp.resolve("sub/panel-estado.json"));
        Map<String, Registro> registros = new LinkedHashMap<>();
        registros.put("est:m1:2026-10-29", completo());
        registros.put("fec:f1", conNulos());
        Map<String, String> fechas = Map.of("nota:m1:parcial1", "2026-10-30");

        estado.guardar(new Estado(registros, fechas));
        Estado leido = estado.cargar();

        assertEquals(registros, leido.registros());
        assertEquals(completo(), leido.registros().get("est:m1:2026-10-29"));
        assertEquals(conNulos(), leido.registros().get("fec:f1"));
        assertEquals(fechas, leido.fechasNotas());
    }

    @Test
    void escrituraAtomicaNoDejaTemporales() throws IOException {
        Path carpeta = tmp.resolve("datos");
        EstadoPanel estado = new EstadoPanel(carpeta.resolve("panel-estado.json"));
        estado.guardar(new Estado(Map.of("fec:f1", conNulos()), Map.of()));
        estado.guardar(new Estado(Map.of("fec:f1", conNulos()), Map.of("nota:m1:parcial1", "2026-10-30")));

        try (var lista = Files.list(carpeta)) {
            assertEquals(List.of("panel-estado.json"), lista.map(p -> p.getFileName().toString()).toList());
        }
        assertEquals(Map.of("nota:m1:parcial1", "2026-10-30"), estado.cargar().fechasNotas());
    }

    @Test
    void vaciarConservaFechas() {
        EstadoPanel estado = new EstadoPanel(tmp.resolve("panel-estado.json"));
        estado.guardar(new Estado(Map.of("fec:f1", conNulos()), Map.of("nota:m1:parcial1", "2026-10-30")));

        estado.vaciar();

        Estado e = estado.cargar();
        assertTrue(e.registros().isEmpty());
        assertEquals(Map.of("nota:m1:parcial1", "2026-10-30"), e.fechasNotas());
    }

    @Test
    void juntoUsaLaCarpetaDelArchivoDeDatos() {
        EstadoPanel estado = EstadoPanel.junto(tmp.resolve("x/planificador_datos.json"));
        assertEquals(tmp.resolve("x/panel-estado.json"), estado.archivo());
    }

    @Test
    void guardarEnUnLugarImposibleNoLanza() throws IOException {
        Path archivo = tmp.resolve("archivo");
        Files.writeString(archivo, "x");
        new EstadoPanel(archivo.resolve("panel-estado.json")).guardar(Estado.vacio());
        assertFalse(Files.isDirectory(archivo));
    }

    @Test
    void entradaConIdQueNoCoincideEsDanado() throws IOException {
        Path f = tmp.resolve("panel-estado.json");
        Registro r = conNulos(); // idEnFuente = fec:f1
        String json = new com.google.gson.Gson().toJson(Map.of("registros", Map.of("otro", r),
                "fechasNotas", Map.of("nota:m1:parcial1", "2026-10-30")));
        Files.writeString(f, json);

        Estado e = new EstadoPanel(f).cargar();

        assertTrue(e.registros().isEmpty());
        assertTrue(e.fechasNotas().isEmpty(), "todo el archivo se trata como dañado");
    }

    @Test
    void entradaSinIdEsDanado() throws IOException {
        Path f = tmp.resolve("panel-estado.json");
        Files.writeString(f, "{\"registros\":{\"a\":{}}}");

        Estado e = new EstadoPanel(f).cargar();

        assertTrue(e.registros().isEmpty());
        assertTrue(e.fechasNotas().isEmpty());
    }
}
