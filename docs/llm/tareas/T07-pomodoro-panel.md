<!-- contrato: 1,3 | archivos: src/main/java/com/planificador/ui/PomodoroPanel.java, src/test/java/com/planificador/ui/PomodoroPanelTest.java -->
# T07 — Pomodoro: el reloj de la ventana

## Objetivo
Completar **solo** `onTick`, `saveCycle` y `updateLabels` de `PomodoroPanel` y los tests `guardaElCicloAlTerminar` y `sinTemaGuardaNull`. El resto de la clase (combos, botones, `toggle`, ajustes) ya está hecho.

## Código que ya existe
- Campos: `engine` (`PomodoroEngine`), `timer` (`javax.swing.Timer`), `running`, `onSaved` (`Runnable`, puede ser null), las etiquetas `timeLabel`, `phaseLabel`, `cyclesLabel` y el botón `startButton`.
- `selectedSubject()` (`Subject` o null) y `selectedTopic()` (`Topic` o null); `phaseName(fase)` devuelve "Estudio", "Descanso corto" o "Descanso largo".
- `PomodoroEngine`: `engine.tick()` (devuelve la fase que terminó o null), `engine.secondsLeft()`, `engine.phase()`, `engine.completedWorkCycles()`, `engine.minutesFor(PomodoroPhase.WORK)`, `PomodoroEngine.format(segundos)` ("MM:SS").
- `StudyLogFactory.fromWorkCycle(id, subjectId, topicId, topicTitle, LocalDateTime end, int minutes)` devuelve el `StudyLog`. `DataManager.getInstance().addStudyLog(log)` lo guarda.
- `Subject`: `s.getId()`. `Topic`: `t.getId()`, `t.getTitle()`.

## Métodos (pasos en orden)
- `onTick()`:
  1. `PomodoroPhase finished = engine.tick();`
  2. `if (finished == PomodoroPhase.WORK) saveCycle();`
  3. `updateLabels();`
  4. `if (finished != null) { timer.stop(); running = false; startButton.setText("▶ Seguir"); Toolkit.getDefaultToolkit().beep(); }`
- `saveCycle()`:
  1. `Subject s = selectedSubject(); if (s == null) return;`
  2. `Topic t = selectedTopic();`
  3. `int minutes = engine.minutesFor(PomodoroPhase.WORK);`
  4. `StudyLog log = StudyLogFactory.fromWorkCycle("pl_" + System.currentTimeMillis(), s.getId(), t != null ? t.getId() : null, t != null ? t.getTitle() : null, LocalDateTime.now(), minutes);`
  5. `DataManager.getInstance().addStudyLog(log);`
  6. `if (onSaved != null) onSaved.run();`
- `updateLabels()`:
  1. `timeLabel.setText(PomodoroEngine.format(engine.secondsLeft()));`
  2. `phaseLabel.setText(phaseName(engine.phase()));`
  3. `cyclesLabel.setText("Ciclos completos: " + engine.completedWorkCycles());`

## Tests (cada uno empieza con `DataManager dm = UiDatos.cargar(tmp); dm.setPomodoroSettings(new PomodoroSettings(1, 1, 2, 2));` y `PomodoroPanel p = new PomodoroPanel(null);`; los datos ya traen 3 `StudyLog`; la materia 0 es "s1" y el tema 1 del combo es "Bernoulli" con id "t1")
- `guardaElCicloAlTerminar`: `p.subjectCombo.setSelectedIndex(0); p.topicCombo.setSelectedIndex(1);` y `for (int i = 0; i < 60; i++) p.onTick();` → `assertEquals(4, dm.getStudyLogs().size());`, `StudyLog l = dm.getStudyLogs().get(3);` `assertEquals("s1", l.getSubjectId());` `assertEquals("t1", l.getTopicId());` `assertEquals(1, l.getMinutes());` y `assertEquals("Descanso corto", p.phaseLabel.getText());`
- `sinTemaGuardaNull`: `p.subjectCombo.setSelectedIndex(0); p.topicCombo.setSelectedIndex(0);` y 60 veces `p.onTick();` → `assertNull(dm.getStudyLogs().get(3).getTopicId());` y `assertEquals("Ciclos completos: 1", p.cyclesLabel.getText());`
