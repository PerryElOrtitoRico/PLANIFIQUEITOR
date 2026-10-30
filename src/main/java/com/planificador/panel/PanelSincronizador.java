package com.planificador.panel;

import com.planificador.panel.EstadoPanel.Estado;
import panel.cliente.ConexionPanel;
import panel.cliente.PanelCompartido;
import com.planificador.model.ImportantDate;
import com.planificador.model.StudyLog;
import com.planificador.model.Subject;
import panel.core.Registro;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Compara lo que dicen los datos del Planificador con lo ya mandado al panel y publica solo la diferencia:
 * registros nuevos, cambiados y marcas de borrado. Nunca lanza; lo único que hace es leer/escribir el
 * archivo de estado y encolar (la red la hace el hilo de la conexión).
 */
public final class PanelSincronizador implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(PanelSincronizador.class.getName());

    public record Resumen(boolean panelActivo, int nuevos, int cambiados, int borrados) {
    }

    /**
     * Lo que se quiere tener en el panel, armado en el hilo que llama con los datos de ese momento. Es todo
     * inmutable (registros, mapas y booleanos): {@link #aplicar} no vuelve a tocar el modelo del Planificador.
     * {@code fechasNuevas}: fechas de notas sin fecha vistas por primera vez en esta toma.
     */
    public record Instantanea(Map<String, Registro> deseado, boolean completo, boolean confiable,
                              Map<String, String> fechasNuevas) {
    }

    private static final long RETENCION_MARCAS_MS = 30L * 24 * 60 * 60 * 1000;

    private final PanelCompartido panel;
    private final EstadoPanel estado;
    private final DatosPlanificador datos;
    private final Clock reloj;
    private ExecutorService ejecutor; // se crea la primera vez que hace falta; protegido por "cierre"
    private boolean cerrado;
    private final Object cierre = new Object();
    /** Fechas recordadas de las notas sin fecha; null hasta la primera toma con el panel activo. Inmutable. */
    private volatile Map<String, String> fechasMemoria;

    public PanelSincronizador(PanelCompartido panel, EstadoPanel estado, DatosPlanificador datos, Clock reloj) {
        this.panel = panel;
        this.estado = estado;
        this.datos = datos;
        this.reloj = reloj;
    }

    /** Sincroniza ahora, en el hilo que llama: equivale a {@code aplicar(tomar())}. */
    public Resumen sincronizar() {
        return aplicar(tomar());
    }

    /**
     * Arma lo deseado en el hilo que llama (el de eventos, normalmente: es el que modifica los datos), de modo que
     * el hilo de sincronización nunca lea objetos mutables. Devuelve null si el panel está apagado (no lee nada).
     * Si los datos no se pueden leer devuelve una instantánea vacía y no confiable.
     */
    public Instantanea tomar() {
        try {
            ConexionPanel conexion = panel.get();
            if (conexion == null || !conexion.activo()) return null;
            Map<String, String> recordadas = memoria();
            Map<String, String> nuevas = new LinkedHashMap<>();
            LocalDate hoy = LocalDate.now(reloj);
            RegistrosPanel.Resultado armado = RegistrosPanel.construirConEstado(datos.materias(), datos.estudio(),
                    datos.fechas(), hoy, conexion.dispositivo(), id -> {
                        String f = recordadas.get(id);
                        return f != null ? f : nuevas.computeIfAbsent(id, k -> hoy.toString());
                    });
            return new Instantanea(Collections.unmodifiableMap(new LinkedHashMap<>(armado.registros())), armado.completo(), datos.confiable(),
                    Map.copyOf(nuevas));
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "No se pudieron copiar los datos para el panel: {0}", e.getClass().getSimpleName());
            // Sin datos de fiar: se corre igual, pero sin publicar nada ni borrar nada.
            return new Instantanea(Map.of(), false, false, Map.of());
        }
    }

    /** Las fechas recordadas; el archivo de estado se lee una sola vez, la primera que hace falta. */
    private Map<String, String> memoria() {
        Map<String, String> m = fechasMemoria;
        if (m != null) return m;
        synchronized (this) {
            if (fechasMemoria == null) fechasMemoria = Map.copyOf(estado.cargar().fechasNotas());
            return fechasMemoria;
        }
    }

    /** Sincroniza en un hilo propio (los datos se copian ya, en el hilo que llama). Con el panel apagado no hace nada. */
    public void sincronizarAsync() {
        sincronizarAsync(null);
    }

    /**
     * Como {@link #sincronizarAsync()}; {@code alTerminar} se invoca en el hilo de sincronización (no en el de
     * eventos) con lo hecho, salvo que el panel esté apagado, caso en el que no se hace ni se invoca nada.
     */
    public void sincronizarAsync(Consumer<Resumen> alTerminar) {
        Instantanea foto = tomar();
        if (foto == null) return;
        synchronized (cierre) {
            if (cerrado) return;
            if (ejecutor == null) {
                ejecutor = Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "panel-sync-planificador");
                    t.setDaemon(true);
                    return t;
                });
            }
            ejecutor.execute(() -> {
                try {
                    Resumen r = aplicar(foto);
                    if (alTerminar != null) alTerminar.accept(r);
                } catch (RuntimeException e) {
                    LOG.log(Level.WARNING, "La sincronización asíncrona falló: {0}", e.getClass().getSimpleName());
                }
            });
        }
    }

    /** Deja de aceptar corridas nuevas; la que esté en curso termina sola. No espera. */
    @Override
    public void close() {
        synchronized (cierre) {
            cerrado = true;
            if (ejecutor != null) ejecutor.shutdown();
        }
    }

    /** "Reenviar todo": olvida lo mandado y aplica, sin que una corrida en curso pueda pisar el borrado. */
    public synchronized Resumen reenviarTodo(Instantanea foto) {
        estado.vaciar();
        return aplicar(foto);
    }

    /** Olvida lo mandado (cambio de servidor), serializado con las corridas. */
    public synchronized void olvidarEstado() {
        estado.vaciar();
    }

    /** Diferencia contra el estado, publicación, marcas de borrado y guardado del estado. No toca el modelo. */
    public synchronized Resumen aplicar(Instantanea foto) {
        int nuevos = 0;
        int cambiados = 0;
        int borrados = 0;
        try {
            ConexionPanel conexion = panel.get();
            if (foto == null || conexion == null || !conexion.activo()) return new Resumen(false, 0, 0, 0);

            Estado previo = estado.cargar();
            // Una fecha ya recordada en el archivo gana sobre la que vio esta toma.
            Map<String, String> fechas = new LinkedHashMap<>(foto.fechasNuevas());
            fechas.putAll(previo.fechasNotas());
            Map<String, Registro> deseado = foto.deseado();
            // Solo se borra del panel con datos de fiar y completos: si no, lo que falta puede ser un error nuestro.
            boolean sinMarcas = !foto.confiable() || !foto.completo()
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
                fechasMemoria = Map.copyOf(guardadas);
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
