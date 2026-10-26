```java
// src/main/java/com/planificador/ui/PomodoroPanel.java
    void onTick() {
        PomodoroPhase finished = engine.tick();
        if (finished == PomodoroPhase.WORK) saveCycle();
        updateLabels();
        if (finished != null) {
            timer.stop();
            running = false;
            startButton.setText("▶ Seguir");
            Toolkit.getDefaultToolkit().beep();
        }
    }

    void saveCycle() {
        Subject s = selectedSubject();
        if (s == null) return;
        Topic t = selectedTopic();
        int minutes = engine.minutesFor(PomodoroPhase.WORK);
        StudyLog log = StudyLogFactory.fromWorkCycle("pl_" + System.currentTimeMillis(), s.getId(), t != null ? t.getId() : null, t != null ? t.getTitle() : null, java.time.LocalDateTime.now(), minutes);
        DataManager.getInstance().addStudyLog(log);
        if (onSaved != null) onSaved.run();
    }

    void updateLabels() {
        timeLabel.setText(PomodoroEngine.format(engine.secondsLeft()));
        phaseLabel.setText(phaseName(engine.phase()));
        cyclesLabel.setText("Ciclos completos: " + engine.completedWorkCycles());
    }
```

```java
// src/test/java/com/planificador/ui/PomodoroPanelTest.java
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
```

<!-- 57 s · entrada 3756 · salida 623 · finish_reason stop -->
