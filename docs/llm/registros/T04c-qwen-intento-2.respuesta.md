```java
// src/main/java/com/planificador/service/CourseStatusCalculator.java
    public static CourseStatus evaluate(CourseTracking tracking) {
        CourseRules rules = tracking.getRules();
        AttendanceSummary a = attendance(rules, tracking.getAttendance());
        List<ExamResult> exams = exams(rules, tracking.getEvaluations());

        boolean anyFailed = false;
        boolean anyOpen = false;
        boolean allPromo = true;
        boolean promoPossible = true;

        for (ExamResult e : exams) {
            if (e.state() == ExamState.DESAPROBADO) anyFailed = true;
            if (e.state() == ExamState.PENDIENTE || e.state() == ExamState.A_RECUPERAR) anyOpen = true;
            if (e.effective() == null || e.effective() < rules.getPromotionGrade()) allPromo = false;
            boolean ok = e.effective() == null || e.effective() >= rules.getPromotionGrade() || e.state() == ExamState.A_RECUPERAR;
            if (!ok) promoPossible = false;
        }

        boolean attendanceOpen = rules.getPlannedClasses() > 0 && a.registered() < rules.getPlannedClasses();
        CourseCondition c;
        if (a.failed() || anyFailed) c = CourseCondition.LIBRE;
        else if (anyOpen || attendanceOpen) c = CourseCondition.EN_CURSO;
        else if (rules.getPromotionGrade() > 0 && allPromo) c = CourseCondition.PROMOCION;
        else c = CourseCondition.REGULAR;

        boolean canPromote = c != CourseCondition.LIBRE && rules.getPromotionGrade() > 0 && promoPossible;
        return new CourseStatus(c, a, exams, canPromote, c != CourseCondition.LIBRE);
    }
```

```java
// src/test/java/com/planificador/service/CourseStatusCalculatorTest.java
    @Test
    void condiciones() {
        assertEquals(CourseCondition.PROMOCION, CourseStatusCalculator.evaluate(new CourseTracking(reglas(), List.of(parcial(1, 8), parcial(2, 9)), asistencias(14, 2))).condition());
        assertEquals(CourseCondition.REGULAR, CourseStatusCalculator.evaluate(new CourseTracking(reglas(), List.of(parcial(1, 7), parcial(2, 9)), asistencias(16, 0))).condition());
        assertEquals(CourseCondition.LIBRE, CourseStatusCalculator.evaluate(new CourseTracking(reglas(), List.of(parcial(1, 8), parcial(2, 9)), asistencias(11, 5))).condition());
        assertEquals(CourseCondition.EN_CURSO, CourseStatusCalculator.evaluate(new CourseTracking(reglas(), List.of(parcial(1, 8), parcial(2, 9)), asistencias(10, 0))).condition());
    }

    @Test
    void todaviaPuedePromocionar() {
        assertTrue(CourseStatusCalculator.evaluate(new CourseTracking(reglas(), List.of(parcial(1, 8), parcial(2, 4)), asistencias(16, 0))).canPromote());
        assertFalse(CourseStatusCalculator.evaluate(new CourseTracking(reglas(), List.of(parcial(1, 6)), asistencias(5, 0))).canPromote());
        assertTrue(CourseStatusCalculator.evaluate(new CourseTracking(reglas(), List.of(parcial(1, 6)), asistencias(5, 0))).canRegularize());
    }
```

<!-- 84 s · entrada 4942 · salida 787 · finish_reason stop -->
