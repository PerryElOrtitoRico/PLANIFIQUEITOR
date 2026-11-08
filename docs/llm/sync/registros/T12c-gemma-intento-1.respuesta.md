```java
// src/main/java/com/planificador/ui/HistorialPanel.java
    private void mostrarDetalle(List<StudyLog> logs) {
        filas.clear();
        DefaultTableModel m = new DefaultTableModel(new String[]{"Hora", "Materia", "Tema", "Minutos", "Origen", "Nota"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        int total = 0;
        for (StudyLog l : logs) {
            if (!elegido.toString().equals(l.getDate())) continue;
            Subject s = DataManager.getInstance().getSubjectById(l.getSubjectId());
            m.addRow(new Object[]{l.getStartTime() == null ? "—" : l.getStartTime(),
                    s != null ? s.getName() : (l.getSubjectId() == null ? "General" : "(materia eliminada)"),
                    l.getTopicTitle() == null ? "" : l.getTopicTitle(),
                    l.getMinutes(), origen(l), l.getNote() == null ? "" : l.getNote()});
            filas.add(l);
            total += l.getMinutes();
        }
        detalle.setModel(m);
        detalleTitulo.setText(elegido.getDayOfMonth() + "/" + elegido.getMonthValue() + "/" + elegido.getYear()
                + (total > 0 ? " — total " + formatear(total) : " — no hay estudio registrado"));
    }
```

<!-- 21 s · entrada 2848 · salida 355 · finish_reason stop -->
