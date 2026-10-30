package com.planificador.ui;

import com.planificador.panel.PanelSincronizador.Resumen;
import org.junit.jupiter.api.Test;
import panel.core.PanelClient.Conexion;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TextosPlanificadorTest {

    @Test
    void resultadoDeLaPrueba() {
        assertEquals("Conectado", TextosPlanificador.prueba(Conexion.OK));
        assertEquals("Token inválido", TextosPlanificador.prueba(Conexion.NO_AUTORIZADO));
        assertEquals("No responde", TextosPlanificador.prueba(Conexion.SIN_RESPUESTA));
        assertEquals("No responde", TextosPlanificador.prueba(null));
    }

    @Test
    void reenvioSinPendientesDiceMandados() {
        assertEquals("Se mandaron 5 registros al panel.", TextosPlanificador.reenvio(new Resumen(true, 3, 2, 0), 0));
    }

    @Test
    void reenvioConPendientesDiceCola() {
        assertEquals("Se pusieron 5 registros en la cola; el panel no respondió, "
                + "se mandarán cuando vuelva la conexión.", TextosPlanificador.reenvio(new Resumen(true, 3, 2, 1), 4));
    }

    @Test
    void reenvioNoCuentaLosBorrados() {
        assertEquals("Se mandaron 0 registros al panel.", TextosPlanificador.reenvio(new Resumen(true, 0, 0, 7), 0));
    }

    @Test
    void reenvioConElPanelInactivoDiceQueNoHayConexion() {
        assertEquals(TextosPlanificador.notaInactiva(), TextosPlanificador.reenvio(new Resumen(false, 0, 0, 0), 0));
    }

    @Test
    void notaDeConexionInactiva() {
        assertEquals("No hay una conexión activa guardada: no se envía nada al panel.",
                TextosPlanificador.notaInactiva());
    }

    @Test
    void erroresUsanLaCausaOLaClase() {
        assertEquals("No se pudo guardar la configuración: disco lleno",
                TextosPlanificador.errorAlGuardar(new IOException("disco lleno")));
        assertEquals("No se pudo guardar la configuración: IOException",
                TextosPlanificador.errorAlGuardar(new IOException()));
        assertEquals("No se pudo reenviar: IllegalStateException",
                TextosPlanificador.errorAlReenviar(new IllegalStateException(" ")));
        assertFalse(TextosPlanificador.errorAlReenviar(null).isBlank());
    }
}
