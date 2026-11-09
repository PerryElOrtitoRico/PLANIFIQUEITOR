package com.planificador.app;

import com.planificador.model.PomodoroSettings;
import com.planificador.model.Subject;
import com.planificador.service.CarpetaDatos;
import com.planificador.service.DataManager;
import com.planificador.service.PomodoroEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** El código de la PC (model y service) corre en la app de Android. */
class CompartidoTest {

    @TempDir
    Path tmp;

    @Test
    void elCodigoDeLaPcCorreEnAndroid() throws IOException {
        Path carpeta = tmp.resolve("datos");
        new CarpetaDatos(carpeta).escribir(new CarpetaDatos.Datos(
                new ArrayList<>(List.of(new Subject("m1", "Física", "#000000", "", ""))),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new PomodoroSettings()));
        DataManager.iniciar(tmp.resolve("local/planificador_datos.json"), carpeta);
        assertEquals("Física", DataManager.getInstance().getSubjects().get(0).getName());
        assertEquals(1500, new PomodoroEngine(new PomodoroSettings()).secondsLeft());
    }
}
