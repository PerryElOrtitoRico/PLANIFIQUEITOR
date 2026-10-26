<!-- contrato: 1,6 | archivos: src/main/java/com/planificador/ui/CountdownPanel.java, src/test/java/com/planificador/ui/CountdownPanelTest.java -->
# T06 — Pantalla de Hoy: panel de cuenta regresiva

## Objetivo
Completar **solo** `refresh(LocalDate today)` de `CountdownPanel` y los tests `muestraLasProximas` y `sinFechas`.

## Código que ya existe
- `DataManager.getInstance()`: `dm.getImportantDates()` (`List<ImportantDate>`), `dm.getSubjectById(id)` (devuelve `Subject` o null). `Subject`: `s.getName()`. `ImportantDate`: `d.getSubjectId()`, `d.getDate()`.
- `Countdown.upcoming(dates, today, maxDays)`, `Countdown.text(date, subjectName, today)` y `Countdown.daysUntil(isoDate, today)` (devuelve `Long`, puede ser null), ya hechos.
- En la clase: `DIAS` (30), `URGENTE` y `NORMAL` (colores) y `labels()` (las `JLabel` del panel, para los tests).
- En los tests: `UiDatos.cargar(tmp)` escribe datos inventados en una carpeta temporal y hace que `DataManager.getInstance()` los use. Fechas de esos datos (hoy = 2026-10-25): "Charla" (sin materia) hoy, "Entrega TP" (Física II) mañana, "Primer parcial" (Mecánica de Fluidos) en 6 días, "Coloquio" (Física II) en 10 días; las demás ya pasaron o están a más de 30 días.

## Método `refresh(today)` (pasos en orden)
1. `removeAll();`
2. `DataManager dm = DataManager.getInstance();`
3. `List<ImportantDate> proximas = Countdown.upcoming(dm.getImportantDates(), today, DIAS);`
4. `if (proximas.isEmpty()) { JLabel l = new JLabel("Sin fechas importantes en los próximos " + DIAS + " días"); l.setForeground(NORMAL); add(l); }`
5. `for (ImportantDate d : proximas) {`
   - `Subject s = dm.getSubjectById(d.getSubjectId()); String nombre = s != null ? s.getName() : null;`
   - `JLabel l = new JLabel("⏳ " + Countdown.text(d, nombre, today));`
   - `Long n = Countdown.daysUntil(d.getDate(), today);`
   - `l.setForeground(n != null && n <= 7 ? URGENTE : NORMAL);`
   - `l.setFont(new Font("SansSerif", Font.BOLD, 13));`
   - `add(l);`
   - `}`
6. `revalidate(); repaint();`

## Tests (cada uno empieza con `UiDatos.cargar(tmp);` y `CountdownPanel p = new CountdownPanel();`)
- `muestraLasProximas`: `p.refresh(HOY);` → `assertEquals(4, p.labels().size());`, `assertEquals("⏳ Charla es hoy", p.labels().get(0).getText());`, `assertEquals("⏳ Primer parcial (Mecánica de Fluidos) en 6 días", p.labels().get(2).getText());` y `assertEquals(CountdownPanel.NORMAL, p.labels().get(3).getForeground());`
- `sinFechas`: `p.refresh(LocalDate.of(2027, 6, 1));` → `assertEquals(1, p.labels().size());` y `assertEquals("Sin fechas importantes en los próximos 30 días", p.labels().get(0).getText());`
