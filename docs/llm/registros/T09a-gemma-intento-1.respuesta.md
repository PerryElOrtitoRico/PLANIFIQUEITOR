```java
// src/main/java/com/planificador/ui/CourseRulesForm.java
    public CourseRules toRules() {
        int n = (Integer) examCount.getValue();
        List<Boolean> r = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            r.add(i < retakeBoxes.size() && retakeBoxes.get(i).isSelected());
        }
        return new CourseRules(((Number) minAttendance.getValue()).doubleValue(), ((Number) passGrade.getValue()).doubleValue(), ((Number) promotionGrade.getValue()).doubleValue(), n, r, (Integer) plannedClasses.getValue());
    }
```

```java
// src/main/java/com/planificador/ui/EvaluationForm.java
    public Evaluation toEvaluation(String id) {
        boolean parcial = kindCombo.getSelectedIndex() == 0;
        String kind = parcial ? Evaluation.PARCIAL : Evaluation.TP;
        int number = (Integer) numberSpinner.getValue();
        boolean retake = parcial && retakeCheck.isSelected();
        double grade = ((Number) gradeSpinner.getValue()).doubleValue();
        String text = dateField.getText().trim();
        String date = null;
        if (!text.isEmpty()) {
            try {
                date = LocalDate.parse(text).toString();
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Fecha inválida: usá AAAA-MM-DD");
            }
        }
        return new Evaluation(id, kind, number, retake, grade, date);
    }
```

```java
// src/test/java/com/planificador/ui/CourseFormsTest.java
    @Test
    void reglasDelFormulario() throws Exception {
        CourseRulesForm f = new CourseRulesForm(new CourseRules(80, 6, 8, 3, List.of(true, false, true), 14));
        CourseRules r = f.toRules();
        assertEquals(80, r.getMinAttendancePercent(), 1e-9);
        assertEquals(3, r.getExamCount());
        assertEquals(List.of(true, false, true), r.getRetakes());
        assertEquals(14, r.getPlannedClasses());
    }

    @Test
    void notaDelFormulario() throws Exception {
        EvaluationForm f = new EvaluationForm();
        f.numberSpinner.setValue(2);
        f.retakeCheck.setSelected(true);
        f.gradeSpinner.setValue(7.5);
        f.dateField.setText("2026-10-01");
        Evaluation e = f.toEvaluation("e1");
        assertEquals(Evaluation.PARCIAL, e.getKind());
        assertEquals(2, e.getNumber());
        assertTrue(e.isRetake());
        assertEquals(7.5, e.getGrade(), 1e-9);
        assertEquals("2026-10-01", e.getDate());
        f.dateField.setText("1/10");
        assertThrows(IllegalArgumentException.class, () -> f.toEvaluation("e2"));
    }
```

<!-- 50 s · entrada 3524 · salida 732 · finish_reason stop -->
