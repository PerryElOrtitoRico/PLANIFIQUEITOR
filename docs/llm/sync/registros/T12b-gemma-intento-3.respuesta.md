```java
// src/main/java/com/planificador/ui/PomodoroPanel.java
    /** Guarda el ciclo de estudio que acaba de terminar como StudyLog. */
    void saveCycle() {
        Subject s = cycleSubject != null ? cycleSubject : selectedSubject();
        if (s == null) return;
        Topic t = cycleSubject != null ? cycleTopic : selectedTopic();
        String titulo = t != null ? t.getTitle() : (cycleSubject != null ? cycleDesc : null);
        int minutes = engine.minutesFor(PomodoroPhase.WORK);
        StudyLog log = StudyLogFactory.fromWorkCycle("pl_" + java.util.UUID.randomUUID(), s.getId(), t != null ? t.getId() : null, titulo, java.time.LocalDateTime.now(), minutes);
        log.setNote(DataManager.limpiarNota(noteField.getText()));
        noteField.setText("");
        DataManager.getInstance().addStudyLog(log);
        if (onSaved != null) onSaved.run();
    }
```

<!-- 16 s · entrada 4568 · salida 239 · finish_reason stop -->
