```java
// src/main/java/com/planificador/ui/CoursePanel.java
    static List<String> statusLines(Subject s) {
        if (s.getCourse() == null) return List.of("Todavía no cargaste las reglas de cursada: tocá «Reglas de cursada».");
        CourseStatus st = CourseStatusCalculator.evaluate(s.getCourse());
        return CourseStatusCalculator.describe(st, s.getCourse().getRules());
    }

    static DefaultTableModel evaluationsModel(CourseTracking t) {
        DefaultTableModel m = StudyTimePanel.readOnlyModel(new Object[]{"Tipo", "N.º", "Recuperatorio", "Nota", "Fecha"});
        for (Evaluation e : t.getEvaluations()) {
            boolean parcial = Evaluation.PARCIAL.equals(e.getKind());
            m.addRow(new Object[]{parcial ? "Parcial" : "TP", e.getNumber(), parcial ? (e.isRetake() ? "Sí" : "No") : "—", CourseStatusCalculator.formatGrade(e.getGrade()), e.getDate() == null ? "" : e.getDate()});
        }
        return m;
    }

    static DefaultTableModel attendanceModel(CourseTracking t) {
        DefaultTableModel m = StudyTimePanel.readOnlyModel(new Object[]{"Fecha", "Asistencia"});
        for (AttendanceRecord r : t.getAttendance()) m.addRow(new Object[]{r.getDate(), r.isPresent() ? "Presente" : "Ausente"});
        return m;
    }

    static AttendanceRecord attendanceFrom(String dateText, boolean present) {
        try { return new AttendanceRecord(LocalDate.parse(dateText.trim()).toString(), present); } catch (DateTimeParseException e) { throw new IllegalArgumentException("Fecha inválida: usá AAAA-MM-DD"); }
    }
```

```java
// src/test/java/com/planificador/ui/CoursePanelTest.java
    @Test
    void textosYTablas() throws Exception {
        Subject s = new Subject("s1", "Física II", "#000000", "", "");
        assertEquals(List.of("Todavía no cargaste las reglas de cursada: tocá «Reglas de cursada»."), CoursePanel.statusLines(s));

        CourseTracking t = s.getOrCreateCourse();
        t.getEvaluations().add(new Evaluation("e1", Evaluation.PARCIAL, 1, false, 7, "2026-09-01"));
        t.getEvaluations().add(new Evaluation("e2", Evaluation.TP, 1, false, 8.5, null));
        t.getAttendance().add(new AttendanceRecord("2026-08-10", false));

        assertEquals("Condición: En curso", CoursePanel.statusLines(s).get(0));

        var m = CoursePanel.evaluationsModel(t);
        assertEquals("Parcial", m.getValueAt(0, 0));
        assertEquals("No", m.getValueAt(0, 2));
        assertEquals("8,5", m.getValueAt(1, 3));
        assertEquals("", m.getValueAt(1, 4));

        assertEquals("Ausente", CoursePanel.attendanceModel(t).getValueAt(0, 1));
    }

    @Test
    void asistenciaConFechaMala() throws Exception {
        assertEquals("2026-10-25", CoursePanel.attendanceFrom(" 2026-10-25 ", true).getDate());
        assertThrows(IllegalArgumentException.class, () -> CoursePanel.attendanceFrom("25/10", true));
    }
```

<!-- 66 s · entrada 5060 · salida 852 · finish_reason stop -->
