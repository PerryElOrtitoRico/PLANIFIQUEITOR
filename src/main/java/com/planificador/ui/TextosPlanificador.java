package com.planificador.ui;

import com.planificador.panel.PanelSincronizador.Resumen;
import panel.cliente.TextosConexion;
import panel.core.PanelClient.Conexion;

/** Textos de la ventana de conexión con el panel que dependen de datos; nunca incluyen el token. */
final class TextosPlanificador {

    private TextosPlanificador() {
    }

    static String prueba(Conexion resultado) {
        if (resultado == null) return "No responde";
        return switch (resultado) {
            case OK -> "Conectado";
            case NO_AUTORIZADO -> "Token inválido";
            case SIN_RESPUESTA -> "No responde";
        };
    }

    /** Aviso tras "Reenviar todo": distingue lo entregado de lo que quedó en cola. N = nuevos + cambiados. */
    static String reenvio(Resumen resumen, int pendientes) {
        if (!resumen.panelActivo()) return notaInactiva();
        int n = resumen.nuevos() + resumen.cambiados();
        return pendientes > 0
                ? "Se pusieron " + n + " registros en la cola; el panel no respondió, "
                  + "se mandarán cuando vuelva la conexión."
                : "Se mandaron " + n + " registros al panel.";
    }

    static String notaInactiva() {
        return "No hay una conexión activa guardada: no se envía nada al panel.";
    }

    static String errorAlGuardar(Throwable causa) {
        return "No se pudo guardar la configuración: " + TextosConexion.causa(causa);
    }

    static String errorAlReenviar(Throwable causa) {
        return "No se pudo reenviar: " + TextosConexion.causa(causa);
    }
}
