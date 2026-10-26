<!-- contrato: 1,5 | archivos: src/main/java/com/planificador/ui/CourseRulesForm.java, src/main/java/com/planificador/ui/EvaluationForm.java, src/test/java/com/planificador/ui/CourseFormsTest.java -->
# T09a — Cursada: leer los formularios de reglas y de notas

## Objetivo
Completar **solo** `toRules()` de `CourseRulesForm`, `toEvaluation(id)` de `EvaluationForm` y los tests `reglasDelFormulario` y `notaDelFormulario`.

## Código que ya existe
- `CourseRulesForm`: spinners `minAttendance`, `passGrade`, `promotionGrade` (valores `Double`), `examCount`, `plannedClasses` (valores `Integer`) y la lista `retakeBoxes` (`List<JCheckBox>`, una por parcial; se rearma sola cuando cambia `examCount`).
- `EvaluationForm`: `kindCombo` (índice 0 = "Parcial", 1 = "TP"), `numberSpinner` (`Integer`), `retakeCheck` (`JCheckBox`), `gradeSpinner` (`Double`), `dateField` (`JTextField`).
- Constructores: `new CourseRules(minAttendancePercent, passGrade, promotionGrade, examCount, retakes, plannedClasses)` y `new Evaluation(id, kind, number, retake, grade, date)`; `Evaluation.PARCIAL` y `Evaluation.TP`.

## Métodos (pasos en orden)
- `toRules()`:
  1. `int n = (Integer) examCount.getValue();`
  2. `List<Boolean> r = new ArrayList<>(); for (int i = 0; i < n; i++) r.add(i < retakeBoxes.size() && retakeBoxes.get(i).isSelected());`
  3. `return new CourseRules(((Number) minAttendance.getValue()).doubleValue(), ((Number) passGrade.getValue()).doubleValue(), ((Number) promotionGrade.getValue()).doubleValue(), n, r, (Integer) plannedClasses.getValue());`
- `toEvaluation(id)`:
  1. `boolean parcial = kindCombo.getSelectedIndex() == 0;`
  2. `String kind = parcial ? Evaluation.PARCIAL : Evaluation.TP;`
  3. `int number = (Integer) numberSpinner.getValue();`
  4. `boolean retake = parcial && retakeCheck.isSelected();`
  5. `double grade = ((Number) gradeSpinner.getValue()).doubleValue();`
  6. `String text = dateField.getText().trim(); String date = null;`
  7. `if (!text.isEmpty()) { try { date = LocalDate.parse(text).toString(); } catch (DateTimeParseException e) { throw new IllegalArgumentException("Fecha inválida: usá AAAA-MM-DD"); } }`
  8. `return new Evaluation(id, kind, number, retake, grade, date);`

## Tests (no necesitan datos)
- `reglasDelFormulario`: `CourseRulesForm f = new CourseRulesForm(new CourseRules(80, 6, 8, 3, List.of(true, false, true), 14)); CourseRules r = f.toRules();` → `assertEquals(80, r.getMinAttendancePercent(), 1e-9);`, `assertEquals(3, r.getExamCount());`, `assertEquals(List.of(true, false, true), r.getRetakes());`, `assertEquals(14, r.getPlannedClasses());`
- `notaDelFormulario`: `EvaluationForm f = new EvaluationForm(); f.numberSpinner.setValue(2); f.retakeCheck.setSelected(true); f.gradeSpinner.setValue(7.5); f.dateField.setText("2026-10-01"); Evaluation e = f.toEvaluation("e1");` → `assertEquals(Evaluation.PARCIAL, e.getKind());`, `assertEquals(2, e.getNumber());`, `assertTrue(e.isRetake());`, `assertEquals(7.5, e.getGrade(), 1e-9);`, `assertEquals("2026-10-01", e.getDate());` y después `f.dateField.setText("1/10");` → `assertThrows(IllegalArgumentException.class, () -> f.toEvaluation("e2"));`
