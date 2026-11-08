<!-- contrato: 1,6 | archivos: src/main/java/com/planificador/ui/HistorialPanel.java -->
# T12c — Historial: mostrar la nota

## Objetivo
Completar el `// TODO T12c` de `HistorialPanel.mostrarDetalle(List<StudyLog>)`, que ya existe. Devolvé el método entero; el resto queda igual. Borrá la línea `// TODO T12c…`.

## Cambios
- `HistorialPanel.mostrarDetalle(List<StudyLog> logs)`:
  1. En el lugar del `// TODO T12c`: `filas.clear();`
  2. Los nombres de columna pasan a `new String[]{"Hora", "Materia", "Tema", "Minutos", "Origen", "Nota"}`.
  3. Dentro del `for`, justo antes de `m.addRow(...)`: `filas.add(l);`
  4. En el `m.addRow(new Object[]{...})`, después de `origen(l)` se agrega un elemento más: `l.getNote() == null ? "" : l.getNote()`.
