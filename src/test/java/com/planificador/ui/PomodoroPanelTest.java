package com.planificador.ui;

import com.planificador.model.*;
import com.planificador.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PomodoroPanelTest {

    static final LocalDate HOY = LocalDate.of(2026, 10, 25);

    @TempDir
    Path tmp;

    @Test
    void guardaElCicloAlTerminar() throws Exception {
        DataManager dm = UiDatos.cargar(tmp);
        dm.setPomodoroSettings(new PomodoroSettings(1, 1, 2, 2));
        PomodoroPanel p = new PomodoroPanel(null);
        p.subjectCombo.setSelectedIndex(0);
        p.topicCombo.setSelectedIndex(1);
        for (int i = 0; i < 60; i++) p.onTick();
        assertEquals(4, dm.getStudyLogs().size());
        StudyLog l = dm.getStudyLogs().get(3);
        assertEquals("s1", l.getSubjectId());
        assertEquals("t1", l.getTopicId());
        assertEquals(1, l.getMinutes());
        assertEquals("Descanso corto", p.phaseLabel.getText());
    }

    @Test
    void sinTemaGuardaNull() throws Exception {
        DataManager dm = UiDatos.cargar(tmp);
        dm.setPomodoroSettings(new PomodoroSettings(1, 1, 2, 2));
        PomodoroPanel p = new PomodoroPanel(null);
        p.subjectCombo.setSelectedIndex(0);
        p.topicCombo.setSelectedIndex(0);
        for (int i = 0; i < 60; i++) p.onTick();
        assertNull(dm.getStudyLogs().get(3).getTopicId());
        assertEquals("Ciclos completos: 1", p.cyclesLabel.getText());
    }
}
