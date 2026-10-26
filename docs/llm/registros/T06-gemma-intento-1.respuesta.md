```java
// src/main/java/com/planificador/ui/CountdownPanel.java
    void refresh(LocalDate today) {
        removeAll();
        DataManager dm = DataManager.getInstance();
        List<ImportantDate> proximas = Countdown.upcoming(dm.getImportantDates(), today, DIAS);
        if (proximas.isEmpty()) {
            JLabel l = new JLabel("Sin fechas importantes en los próximos " + DIAS + " días");
            l.setForeground(NORMAL);
            add(l);
        } else {
            for (ImportantDate d : proximas) {
                Subject s = dm.getSubjectById(d.getSubjectId());
                String nombre = s != null ? s.getName() : null;
                JLabel l = new JLabel("⏳ " + Countdown.text(d, nombre, today));
                Long n = Countdown.daysUntil(d.getDate(), today);
                l.setForeground(n != null && n <= 7 ? URGENTE : NORMAL);
                l.setFont(new Font("SansSerif", Font.BOLD, 13));
                add(l);
            }
        }
        revalidate();
        repaint();
    }
```

```java
// src/test/java/com/planificador/ui/CountdownPanelTest.java
    @Test
    void muestraLasProximas() throws Exception {
        UiDatos.cargar(tmp);
        CountdownPanel p = new CountdownPanel();
        p.refresh(HOY);
        assertEquals(4, p.labels().size());
        assertEquals("⏳ Charla es hoy", p.labels().get(0).getText());
        assertEquals("⏳ Primer parcial (Mecánica de Fluidos) en 6 días", p.labels().get(2).getText());
        assertEquals(CountdownPanel.NORMAL, p.labels().get(3).getForeground());
    }

    @Test
    void sinFechas() throws Exception {
        UiDatos.cargar(tmp);
        CountdownPanel p = new CountdownPanel();
        p.refresh(LocalDate.of(2027, 6, 1));
        assertEquals(1, p.labels().size());
        assertEquals("Sin fechas importantes en los próximos 30 días", p.labels().get(0).getText());
    }
```

<!-- 33 s · entrada 2191 · salida 520 · finish_reason stop -->
