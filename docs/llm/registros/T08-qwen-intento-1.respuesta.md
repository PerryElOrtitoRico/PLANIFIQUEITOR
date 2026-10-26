java
// src/main/java/com/planificador/ui/StudyTimePanel.java
    public DefaultTableModel buildWeekModel(LocalDate weekStart) {
        DataManager dm = DataManager.getInstance();
        DefaultTableModel m = readOnlyModel(new Object[]{"Materia", "Planeado", "Estudiado", "Avance"});
        List<String> ids = new ArrayList<>();
        for (Subject s : dm.getSubjects()) ids.add(s.getId());
        List<SubjectWeekTime> times = StudyTimeStats.compareWeek(ids, dm.getStudyLogs(), dm.getStudySessions(), weekStart);
        for (int i = 0; i < times.size(); i++) {
            SubjectWeekTime t = times.get(i);
            String avance = t.plannedMinutes() > 0 ? (t.actualMinutes() * 100 / t.plannedMinutes()) + " %" : "—";
            m.addRow(new Object[]{dm.getSubjects().get(i).getName(), StudyTimeStats.formatMinutes(t.plannedMinutes()), StudyTimeStats.formatMinutes(t.actualMinutes()), avance});
        }
        return m;
    }

    public DefaultTableModel buildHistoryModel(LocalDate lastWeekStart, int weeks) {
        DataManager dm = DataManager.getInstance();
        List<Subject> subjects = dm.getSubjects();
        Object[] cols = new Object[subjects.size() + 2];
        cols[0] = "Semana";
        for (int i = 0; i < subjects.size(); i++) cols[i + 1] = subjects.get(i).getName();
        cols[cols.length - 1] = "Total";
        DefaultTableModel m = readOnlyModel(cols);
        for (int w = weeks - 1; w >= 0; w--) {
            LocalDate ws = lastWeekStart.minusWeeks(w);
            Map<String, Integer> mins = StudyTimeStats.minutesBySubject(dm.getStudyLogs(), ws);
            Object[] row = new Object[cols.length];
            row[0] = ws.format(DIA);
            int total = 0;
            for (int i = 0; i < subjects.size(); i++) {
                int x = mins.getOrDefault(subjects.get(i).getId(), 0);
                total += x;
                row[i + 1] = StudyTimeStats.formatMinutes(x);
            }
            row[cols.length - 1] = StudyTimeStats.formatMinutes(total);
            m.addRow(row);
        }
        return m;
    }
java
// src/test/java/com/planificador/ui/StudyTimePanelTest.java
    @Test
    void tablaDeLaSemana() throws Exception {
        UiDatos.cargar(tmp);
        StudyTimePanel p = new StudyTimePanel(null);
        var m = p.buildWeekModel(LocalDate.of(2026, 10, 19));
        assertEquals(2, m.getRowCount());
        assertEquals("Mecánica de Fluidos", m.getValueAt(0, 0));
        assertEquals("2 h", m.getValueAt(0, 1));
        assertEquals("50 min", m.getValueAt(0, 2));
        assertEquals("41 %", m.getValueAt(0, 3));
    }

    @Test
    void tablaDeLasUltimasSemanas() throws Exception {
        UiDatos.cargar(tmp);
        StudyTimePanel p = new StudyTimePanel(null);
        var m = p.buildHistoryModel(LocalDate.of(2026, 10, 19), 2);
        assertEquals(4, m.getColumnCount());
        assertEquals("Total", m.getColumnName(3));
        assertEquals("12/10", m.getValueAt(0, 0));
        assertEquals("50 min", m.getValueAt(0, 2));
        assertEquals("50 min", m.getValueAt(1, 3));
    }

<!-- 78 s · entrada 3219 · salida 841 · finish_reason stop -->
