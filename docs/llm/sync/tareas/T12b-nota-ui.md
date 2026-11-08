<!-- contrato: 1,6 | archivos: src/main/java/com/planificador/ui/PomodoroPanel.java, src/main/java/com/planificador/ui/TimerDialog.java -->
# T12b — Interfaz: guardar y mostrar la nota

## Objetivo
Completar los `// TODO T12b` de dos métodos que ya existen: `PomodoroPanel.saveCycle()` y `TimerDialog.registrar(Frame)`. Devolvé cada uno de los dos métodos entero; el resto de cada método queda igual. Borrá la línea `// TODO T12b…`.

## Cambios
- `PomodoroPanel.saveCycle()`: en el lugar del `// TODO T12b`, estas dos líneas:
```java
        log.setNote(DataManager.limpiarNota(noteField.getText()));
        noteField.setText("");
```
- `TimerDialog.registrar(Frame owner)`: en el lugar del `// TODO T12b`, esta línea:
```java
        log.setNote(com.planificador.service.DataManager.limpiarNota(noteField.getText()));
```
