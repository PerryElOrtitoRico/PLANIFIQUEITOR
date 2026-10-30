package com.planificador.panel;

import com.planificador.panel.EstadoPanel.Estado;
import panel.cliente.ConexionPanel;
import panel.cliente.PanelCompartido;
import panel.core.Registro;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Compara lo que dicen los datos del Planificador con lo ya mandado al panel y publica solo la diferencia:
 * registros nuevos, cambiados y marcas de borrado. Nunca lanza; lo único que hace es leer/escribir el
 * archivo de estado y encolar (la red la hace el hilo de la conexión).
 */
public final class PanelSincronizador {

    private static final Logger LOG = Logger.getLogger(PanelSincronizador.class.getName());

    public record Resumen(boolean panelActivo, int nuevos, int cambiados, int borrados) {
    }

    private static final long RETENCION_MARCAS_MS = 30L * 24 * 60 * 60 * 1000;

    private final PanelCompartido panel;
    private final EstadoPanel estado;
    private final DatosPlanificador datos;
    private final Clock reloj;

    public PanelSincronizador(PanelCompartido panel, EstadoPanel estado, DatosPlanificador datos, Clock reloj) {
        this.panel = panel;
        this.estado = estado;
        this.datos = datos;
        this.reloj = reloj;
    }

    public synchronized Resumen sincronizar() {
        int nuevos = 0;
        int cambiados = 0;
        int borrados = 0;
        try {
            ConexionPanel conexion = panel.get();
            if (conexion == null || !conexion.activo()) return new Resumen(false, 0, 0, 0);

            Estado previo = estado.cargar();
            Map<String, String> fechas = new LinkedHashMap<>(previo.fechasNotas());
            LocalDate hoy = LocalDate.now(reloj);
            RegistrosPanel.Resultado armado = RegistrosPanel.construirConEstado(datos.materias(), datos.estudio(),
                    datos.fechas(), hoy, conexion.dispositivo(),
                    id -> fechas.computeIfAbsent(id, k -> hoy.toString()));
            Map<String, Registro> deseado = armado.registros();
            // Solo se borra del panel con datos de fiar y completos: si no, lo que falta puede ser un error nuestro.
            boolean sinMarcas = !datos.confiable() || !armado.completo()
                    || (deseado.isEmpty() && previo.registros().values().stream().anyMatch(r -> !r.borrado()));

            Map<String, Registro> nuevo = new LinkedHashMap<>(previo.registros());
            int omitidos = 0;
            try {
                for (Map.Entry<String, Registro> e : deseado.entrySet()) {
                    String id = e.getKey();
                    Registro quiere = e.getValue();
                    Registro enviado = previo.registros().get(id);
                    boolean vivo = enviado != null && !enviado.borrado();
                    if (vivo && iguales(enviado, quiere)) continue;
                    Registro aEnviar = copia(quiere, conexion.dispositivo(), siguiente(enviado), false);
                    conexion.publicar(aEnviar);
                    nuevo.put(id, aEnviar);
                    if (vivo) cambiados++;
                    else nuevos++;
                }
                for (Map.Entry<String, Registro> e : previo.registros().entrySet()) {
                    if (deseado.containsKey(e.getKey()) || e.getValue().borrado()) continue;
                    if (sinMarcas) {
                        omitidos++;
                        continue;
                    }
                    Registro lapida = copia(e.getValue(), e.getValue().dispositivo(), siguiente(e.getValue()), true);
                    conexion.publicar(lapida);
                    nuevo.put(e.getKey(), lapida); // se recuerda: si el id vuelve, su marca de tiempo debe superarla
                    borrados++;
                }
            } finally {
                if (omitidos > 0) LOG.log(Level.WARNING, "borrados omitidos: {0}", omitidos);
                // Lo ya publicado queda anotado aunque algo falle a mitad de camino.
                long limite = reloj.millis() - RETENCION_MARCAS_MS;
                nuevo.values().removeIf(r -> r.borrado() && r.actualizadoEn() < limite);
                Map<String, String> guardadas = new LinkedHashMap<>();
                fechas.forEach((id, f) -> {
                    if (deseado.containsKey(id)) guardadas.put(id, f);
                });
                estado.guardar(new Estado(nuevo, guardadas));
            }
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "La sincronización con el panel falló: {0}", e.getClass().getSimpleName());
        }
        return new Resumen(true, nuevos, cambiados, borrados);
    }

    /** actualizadoEn estrictamente creciente por id, aunque el reloj retroceda. */
    private long siguiente(Registro anterior) {
        long ahora = reloj.millis();
        return anterior == null ? ahora : Math.max(ahora, anterior.actualizadoEn() + 1);
    }

    private static Registro copia(Registro r, String dispositivo, long actualizadoEn, boolean borrado) {
        return new Registro(r.tipo(), r.fuente(), r.idEnFuente(), r.area(), r.fecha(), r.duracionMin(),
                r.resumen(), r.detalleJson(), r.esfuerzo(), r.metrica(), r.valor(), dispositivo,
                actualizadoEn, borrado);
    }

    /** Igual en todo salvo actualizadoEn y dispositivo. */
    private static boolean iguales(Registro a, Registro b) {
        return copia(a, null, 0, a.borrado()).equals(copia(b, null, 0, b.borrado()));
    }
}
