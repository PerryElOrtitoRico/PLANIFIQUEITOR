package com.planificador.ui;

import com.planificador.model.PomodoroSettings;
import com.planificador.model.StudyLog;
import com.planificador.service.DataManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** La nota escrita en el Pomodoro se guarda con el ciclo. */
class NotaPomodoroTest {

    @TempDir
    Path tmp;

    @Test
    void T12b_elCicloGuardaLaNotaEscritaYVaciaElCampo() throws Exception {
        DataManager dm = UiDatos.cargar(tmp);
        dm.setPomodoroSettings(new PomodoroSettings(1, 1, 2, 2));
        PomodoroPanel p = new PomodoroPanel(null);
        p.subjectCombo.setSelectedIndex(0);
        p.noteField.setText("  guía 3 ");
        for (int i = 0; i < 60; i++) p.onTick();
        StudyLog l = dm.getStudyLogs().get(dm.getStudyLogs().size() - 1);
        assertEquals("guía 3", l.getNote());
        assertEquals("", p.noteField.getText());
    }

    @Test
    void T12b_sinNotaGuardaNull() throws Exception {
        DataManager dm = UiDatos.cargar(tmp);
        dm.setPomodoroSettings(new PomodoroSettings(1, 1, 2, 2));
        PomodoroPanel p = new PomodoroPanel(null);
        p.subjectCombo.setSelectedIndex(0);
        for (int i = 0; i < 60; i++) p.onTick();
        assertNull(dm.getStudyLogs().get(dm.getStudyLogs().size() - 1).getNote());
    }
}
