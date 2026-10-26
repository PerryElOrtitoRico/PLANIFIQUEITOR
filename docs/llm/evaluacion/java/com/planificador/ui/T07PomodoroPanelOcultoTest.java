package com.planificador.ui;

import com.planificador.model.PomodoroSettings;
import com.planificador.model.StudyLog;
import com.planificador.service.DataManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class T07PomodoroPanelOcultoTest {

    @TempDir
    Path tmp;

    @Test
    void cicloDeEstudioSeGuardaYAvisa() throws Exception {
        DataManager dm = UiDatos.cargar(tmp);
        dm.setPomodoroSettings(new PomodoroSettings(1, 1, 2, 2));
        int[] avisos = {0};
        PomodoroPanel p = new PomodoroPanel(() -> avisos[0]++);
        assertEquals("01:00", p.timeLabel.getText());
        assertEquals("Estudio", p.phaseLabel.getText());
        p.subjectCombo.setSelectedIndex(0);
        p.topicCombo.setSelectedIndex(2);
        p.running = true;
        for (int i = 0; i < 59; i++) p.onTick();
        assertEquals(3, dm.getStudyLogs().size());
        assertEquals("00:01", p.timeLabel.getText());
        p.onTick();
        assertEquals(4, dm.getStudyLogs().size());
        StudyLog l = dm.getStudyLogs().get(3);
        assertEquals("s1", l.getSubjectId());
        assertEquals("t2", l.getTopicId());
        assertEquals("Viscosidad", l.getTopicTitle());
        assertEquals(1, l.getMinutes());
        assertEquals(LocalDate.now().toString(), l.getDate());
        assertEquals(1, avisos[0]);
        assertEquals("Descanso corto", p.phaseLabel.getText());
        assertEquals("01:00", p.timeLabel.getText());
        assertEquals("Ciclos completos: 1", p.cyclesLabel.getText());
        assertFalse(p.running);
        assertEquals("▶ Seguir", p.startButton.getText());
    }

    @Test
    void elDescansoNoSeGuarda() throws Exception {
        DataManager dm = UiDatos.cargar(tmp);
        dm.setPomodoroSettings(new PomodoroSettings(1, 1, 2, 2));
        PomodoroPanel p = new PomodoroPanel(null);
        p.subjectCombo.setSelectedIndex(1);
        for (int i = 0; i < 120; i++) p.onTick();
        assertEquals(4, dm.getStudyLogs().size());
        assertEquals("s2", dm.getStudyLogs().get(3).getSubjectId());
        assertNull(dm.getStudyLogs().get(3).getTopicId());
        assertEquals("Estudio", p.phaseLabel.getText());
        for (int i = 0; i < 60; i++) p.onTick();
        assertEquals("Descanso largo", p.phaseLabel.getText());
        assertEquals("02:00", p.timeLabel.getText());
        assertEquals(5, dm.getStudyLogs().size());
    }
}
