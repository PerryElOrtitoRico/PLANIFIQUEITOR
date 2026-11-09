package com.planificador.app.datos;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RutasTest {

    @Test
    void laCarpetaEsDocumentsEstudieitorDatos() {
        assertEquals(Path.of("/sdcard/Documents/Estudieitor/datos"), Rutas.carpetaDatos(Path.of("/sdcard/Documents")));
    }
}
