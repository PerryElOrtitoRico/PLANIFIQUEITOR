```java
// src/main/java/com/planificador/ui/PomodoroPanel.java
    void saveCycle() {
        Subject s = cycleSubject != null ? cycleSubject : selectedSubject();
        if (s == null) return;
        Topic t = cycleSubject != null ? cycleTopic : selectedTopic();
        String titulo = t != null ? t.getTitle() : (cycleSubject != null ? cycleDesc : null);
        int minutes = engine.minutesFor(PomodoroPhase.WORK);
        StudyLog log = StudyLogFactory.fromWorkCycle("pl_" + java.util.UUID.randomUUID(), s.getId(), t != null ? t.getId() : null, titulo, java.time.LocalDateTime.now(), minutes);
        log.setNote(DataManager.limpiarNota(noteField.getText()));
        DataManager.getInstance().addStudyLog(log);
        if (onSaved != null) onSaved.run();
    }
```

```java
// src/main/java/com/planificador/ui/TimerDialog.java
    private void registrar(Frame owner) {
        if (registrado) return;
        registrado = true;
        if (timer != null) timer.stop();
        int minutos = (initialSeconds - secondsRemaining) / 60;
        if (minutos < 1) return;
        java.time.LocalDateTime fin = java.time.LocalDateTime.now();
        String id = (sessionId != null ? "cron_" + sessionId + "_" + fin.toLocalDate() + "_" : "cron_") + java.util.UUID.randomUUID();
        com.planificador.model.StudyLog log = com.planificador

<!-- 28 s · entrada 7815 · salida 377 · finish_reason length -->
