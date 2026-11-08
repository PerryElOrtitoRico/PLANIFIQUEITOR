package com.planificador.service;

import com.planificador.model.StudyLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Tests ocultos de T12a (nota de sesión en DataManager). No se le muestran al LLM. */
class T12OcultoTest {

    @TempDir
    Path tmp;

    @Test
    void T12a_limpiarNota() {
        assertEquals("guía 3", DataManager.limpiarNota("  guía 3 "));
        assertNull(DataManager.limpiarNota("   "));
        assertNull(DataManager.limpiarNota(null));
    }

    @Test
    void T12a_laNotaDeUnaSesionSeGuarda() {
        Path f = tmp.resolve("d/planificador_datos.json");
        DataManager dm = new DataManager(f, List.of());
        dm.addStudyLog(new StudyLog("l1", "m1", null, null, "2026-11-08", "10:00", 25));
        assertNull(dm.getStudyLogs().get(0).getNote());
        dm.setStudyLogNote("l1", "  hice 5 ejercicios de la guía 3 ");
        assertEquals("hice 5 ejercicios de la guía 3", new DataManager(f, List.of()).getStudyLogs().get(0).getNote());
        dm.setStudyLogNote("l1", "  ");
        assertNull(new DataManager(f, List.of()).getStudyLogs().get(0).getNote());
        assertDoesNotThrow(() -> dm.setStudyLogNote("no-existe", "x"));
    }
}
