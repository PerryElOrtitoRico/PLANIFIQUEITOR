<!-- contrato: 1,5 | archivos: src/main/java/com/planificador/service/CourseStatusCalculator.java, src/test/java/com/planificador/service/CourseStatusCalculatorTest.java -->
# T04b — Cursada: estado de cada parcial

## Objetivo
Completar **solo** `exams` de `CourseStatusCalculator` y los tests `estadosDeLosParciales` y `elRecuperatorioReemplazaLaNota`. `attendance` ya está hecho.

## Código que ya existe
- `Evaluation` (clase con getters): `e.getKind()` (`Evaluation.PARCIAL` o `Evaluation.TP`), `e.getNumber()`, `e.isRetake()`, `e.getGrade()` (double).
- `CourseRules` (clase con getters): `rules.getExamCount()`, `rules.getPassGrade()`, `rules.hasRetake(n)`.
- `ExamResult` es un record: `new ExamResult(number, original, retake, effective, state)` con `Double` (pueden ser null); se lee con `r.state()`, `r.effective()`.

## Método `exams(rules, evaluations)` (pasos en orden)
1. `List<ExamResult> out = new ArrayList<>();`
2. `for (int n = 1; n <= rules.getExamCount(); n++) {`
3. `  Double original = null; Double retake = null;`
4. `  for (Evaluation e : evaluations) { if (!Evaluation.PARCIAL.equals(e.getKind()) || e.getNumber() != n) continue; if (e.isRetake()) retake = e.getGrade(); else original = e.getGrade(); }`
5. `  Double effective = retake != null ? retake : original;`
6. `  ExamState state; if (effective == null) state = ExamState.PENDIENTE; else if (effective >= rules.getPassGrade()) state = ExamState.APROBADO; else if (retake == null && rules.hasRetake(n)) state = ExamState.A_RECUPERAR; else state = ExamState.DESAPROBADO;`
7. `  out.add(new ExamResult(n, original, retake, effective, state));`
8. `}` y `return out;`

## Tests (usan `reglas()` = 6 regulariza, 2 parciales con recuperatorio, `parcial(n, nota)` y `recuperatorio(n, nota)` que ya están en el test)
- `estadosDeLosParciales`: `List<ExamResult> r = CourseStatusCalculator.exams(reglas(), List.of(parcial(1, 7), parcial(2, 4)));` → `assertEquals(ExamState.APROBADO, r.get(0).state())` y `assertEquals(ExamState.A_RECUPERAR, r.get(1).state())`; y con `List.of()` → `assertEquals(ExamState.PENDIENTE, ...get(0).state())`.
- `elRecuperatorioReemplazaLaNota`: con `List.of(parcial(2, 4), recuperatorio(2, 7))` → `get(1).effective()` es `7.0` y el estado `APROBADO`; con `List.of(parcial(2, 4), recuperatorio(2, 3))` → estado `DESAPROBADO`.
