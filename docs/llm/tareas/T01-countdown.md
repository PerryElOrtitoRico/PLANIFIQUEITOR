<!-- contrato: 1,6 | archivos: src/main/java/com/planificador/service/Countdown.java, src/test/java/com/planificador/service/CountdownTest.java -->
# T01 — Cuenta regresiva de las fechas importantes

## Objetivo
Completar `daysUntil`, `upcoming` y `text` de `Countdown` y los tres tests de `CountdownTest`.

## Código que ya existe
`ImportantDate` es una clase con getters: `d.getTitle()`, `d.getDate()` (texto `YYYY-MM-DD`), `d.getSubjectId()`. Constructor: `new ImportantDate(id, title, subjectId, date, type, notes)`.

## Métodos (pasos en orden)
- `daysUntil(isoDate, today)`:
  1. `if (isoDate == null) return null;`
  2. `try { LocalDate f = LocalDate.parse(isoDate); return ChronoUnit.DAYS.between(today, f); } catch (DateTimeParseException e) { return null; }`
- `upcoming(dates, today, maxDays)`:
  1. `List<ImportantDate> out = new ArrayList<>();`
  2. `for (ImportantDate d : dates) { Long n = daysUntil(d.getDate(), today); if (n != null && n >= 0 && n <= maxDays) out.add(d); }`
  3. `out.sort(Comparator.comparing((ImportantDate d) -> daysUntil(d.getDate(), today)).thenComparing(ImportantDate::getTitle));`
  4. `return out;`
- `text(date, subjectName, today)`:
  1. `String base = date.getTitle();`
  2. `if (subjectName != null && !subjectName.isBlank()) base = base + " (" + subjectName + ")";`
  3. `Long n = daysUntil(date.getDate(), today);`
  4. `if (n == null) return base;` · `if (n == 0) return base + " es hoy";` · `if (n == 1) return base + " es mañana";` · `return base + " en " + n + " días";`

## Tests (usan `HOY` = 2026-10-25 y `fecha(id, titulo, iso)` que ya están en el test)
- `cuentaLosDias`: `assertEquals(6L, Countdown.daysUntil("2026-10-31", HOY));` y `assertNull(Countdown.daysUntil("31/10/2026", HOY));`
- `proximasOrdenadasYSinLasViejas`: con `ImportantDate vieja = fecha("1", "Vieja", "2026-10-20"), lejos = fecha("2", "Lejos", "2026-12-31"), b = fecha("3", "B", "2026-10-28"), a = fecha("4", "A", "2026-10-26");` → `assertEquals(List.of(a, b), Countdown.upcoming(List.of(vieja, lejos, b, a), HOY, 30));`
- `textoSegunLosDias`: `assertEquals("Parcial (Física) en 6 días", Countdown.text(fecha("1", "Parcial", "2026-10-31"), "Física", HOY));`, `assertEquals("Parcial es mañana", Countdown.text(fecha("1", "Parcial", "2026-10-26"), null, HOY));` y `assertEquals("Parcial es hoy", Countdown.text(fecha("1", "Parcial", "2026-10-25"), null, HOY));`
