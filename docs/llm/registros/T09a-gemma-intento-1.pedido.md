<!-- modelo google/gemma-4-e4b-qat, temperatura 0.2, max_tokens 4614, razonamiento no -->

## system

Sos un programador Java 17 cuidadoso. Te paso archivos con métodos marcados `// TODO`: completá solo los cuerpos que nombra la tarea; los métodos marcados "lo completa otra subtarea" quedan exactamente como están.
Reglas:
- No cambies package, imports que ya están (aunque todavía no se usen), nombres de clases, firmas ni métodos que ya están hechos. Podés agregar imports y métodos private.
- Las clases de model se leen con getters: log.getMinutes(), rules.getPassGrade(). Los records de service se leen con x(): status.condition(), a.percent(). No existen getCondition() ni getPercent().
- En los tests, copiá las aserciones de la tarea tal cual (mismos valores y fechas) y no escribas comentarios.
- No crees ni modifiques otros archivos.
- Devolvé solo los métodos que completaste, cada uno entero (firma y cuerpo), sin el resto de la clase: un bloque por archivo, con la ruta en la primera línea. Ejemplo:
```java
// src/main/java/com/planificador/service/Ejemplo.java
    public static int doble(int x) {
        return 2 * x;
    }
```
- Sin explicaciones fuera de los bloques.

## user

# Contrato del proyecto (extracto)

# PLANIFICADOR, etapa Pomodoro y cursada — Especificación (contrato)

## 1. Reglas de código
- Java 17 (el `pom.xml` compila con `source/target` 17), Maven. Paquete base `com.planificador`: `model` (datos), `service` (lógica y persistencia), `ui` (Swing). No toques `pom.xml` ni agregues dependencias.
- Nombres de clases y métodos en inglés, como el código que ya existe; comentarios en español.
- Las clases de `model` tienen getters: `log.getMinutes()`, `rules.getPassGrade()`, `ev.isRetake()`. Los **records** de `service` se leen con `x()`: `status.condition()`, `a.percent()`. No existen `getCondition()` ni `getPercent()`.
- La lógica no usa `LocalDate.now()`: la fecha de hoy se pasa por parámetro (así se testea).
- Tests con JUnit 5 (`org.junit.jupiter.api.*`); archivos con `@TempDir`. Ningún test lee ni escribe `~/.local/share/planificador-estudio`.

## 5. Cursada (por materia)
- **Asistencia**: `porcentaje = presentes * 100 / registradas` (100 si no hay ninguna). Si se sabe la cantidad de clases (`plannedClasses > 0`): `faltasPermitidas = floor(plannedClasses * (100 - minAttendancePercent) / 100)`, `quedan = faltasPermitidas - ausentes`, y queda libre por faltas si `quedan < 0`. Si no se sabe: libre por faltas si hay clases registradas y `porcentaje < minAttendancePercent`.
- **Parciales** 1…`examCount`. Nota efectiva = la del recuperatorio si hay; si no, la del parcial. Estado: sin nota → `PENDIENTE`; efectiva ≥ `passGrade` → `APROBADO`; menor, sin recuperatorio rendido y con recuperatorio permitido → `A_RECUPERAR`; si no → `DESAPROBADO`. Si hay dos notas del mismo parcial (o del mismo recuperatorio), vale la última cargada.
- Los **TP** se cargan y se muestran, pero no cambian la condición.
- **Condición**, en este orden: `LIBRE` si quedó libre por faltas o hay un parcial `DESAPROBADO`; `EN_CURSO` si hay un parcial `PENDIENTE` o `A_RECUPERAR`, o si `plannedClasses > 0` y todavía hay menos clases registradas; `PROMOCION` si `promotionGrade > 0` y todas las notas efectivas ≥ `promotionGrade`; si no, `REGULAR`.
- Todavía puede promocionar si no está libre, `promotionGrade > 0` y cada parcial está sin nota, tiene efectiva ≥ `promotionGrade` o está `A_RECUPERAR`. Todavía puede regularizar si no está libre.

---

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

---

# Archivos actuales

```java
// src/main/java/com/planificador/ui/CourseRulesForm.java
package com.planificador.ui;

import com.planificador.model.CourseRules;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Formulario de las reglas de cursada de una materia (se muestra con JOptionPane). */
public class CourseRulesForm extends JPanel {

    final JSpinner minAttendance;
    final JSpinner passGrade;
    final JSpinner promotionGrade;
    final JSpinner examCount;
    final JSpinner plannedClasses;
    final List<JCheckBox> retakeBoxes = new ArrayList<>();
    private final JPanel retakesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));

    public CourseRulesForm(CourseRules rules) {
        minAttendance = new JSpinner(new SpinnerNumberModel(rules.getMinAttendancePercent(), 0.0, 100.0, 5.0));
        passGrade = new JSpinner(new SpinnerNumberModel(rules.getPassGrade(), 0.0, 10.0, 0.5));
        promotionGrade = new JSpinner(new SpinnerNumberModel(rules.getPromotionGrade(), 0.0, 10.0, 0.5));
        examCount = new JSpinner(new SpinnerNumberModel(rules.getExamCount(), 0, 10, 1));
        plannedClasses = new JSpinner(new SpinnerNumberModel(rules.getPlannedClasses(), 0, 200, 1));

        setLayout(new GridLayout(0, 2, 8, 6));
        add(new JLabel("Asistencia mínima (%):"));
        add(minAttendance);
        add(new JLabel("Nota para regularizar:"));
        add(passGrade);
        add(new JLabel("Nota para promocionar (0 = no hay):"));
        add(promotionGrade);
        add(new JLabel("Cantidad de parciales:"));
        add(examCount);
        add(new JLabel("Con recuperatorio:"));
        add(retakesPanel);
        add(new JLabel("Clases del cuatrimestre (0 = no sé):"));
        add(plannedClasses);

        buildRetakeBoxes(rules.getExamCount(), rules.getRetakes());
        examCount.addChangeListener(e -> buildRetakeBoxes((Integer) examCount.getValue(), currentRetakes()));
    }

    /** Una casilla por parcial ("P1", "P2", …), marcada según previous (las que faltan, marcadas). */
    private void buildRetakeBoxes(int n, List<Boolean> previous) {
        retakeBoxes.clear();
        retakesPanel.removeAll();
        for (int i = 0; i < n; i++) {
            JCheckBox box = new JCheckBox("P" + (i + 1), i >= previous.size() || Boolean.TRUE.equals(previous.get(i)));
            retakeBoxes.add(box);
            retakesPanel.add(box);
        }
        retakesPanel.revalidate();
        retakesPanel.repaint();
    }

    private List<Boolean> currentRetakes() {
        List<Boolean> out = new ArrayList<>();
        for (JCheckBox b : retakeBoxes) out.add(b.isSelected());
        return out;
    }

    /** Las reglas que quedaron cargadas en el formulario. */
    public CourseRules toRules() {
        // TODO T09a
        throw new UnsupportedOperationException();
    }
}
```

```java
// src/main/java/com/planificador/ui/EvaluationForm.java
package com.planificador.ui;

import com.planificador.model.Evaluation;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Formulario para cargar una nota de parcial, recuperatorio o TP (se muestra con JOptionPane). */
public class EvaluationForm extends JPanel {

    final JComboBox<String> kindCombo = new JComboBox<>(new String[]{"Parcial", "TP"});
    final JSpinner numberSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
    final JCheckBox retakeCheck = new JCheckBox("Es el recuperatorio");
    final JSpinner gradeSpinner = new JSpinner(new SpinnerNumberModel(6.0, 0.0, 10.0, 0.25));
    final JTextField dateField = new JTextField(LocalDate.now().toString());

    public EvaluationForm() {
        setLayout(new GridLayout(0, 2, 8, 6));
        add(new JLabel("Tipo:"));
        add(kindCombo);
        add(new JLabel("Número:"));
        add(numberSpinner);
        add(new JLabel(""));
        add(retakeCheck);
        add(new JLabel("Nota:"));
        add(gradeSpinner);
        add(new JLabel("Fecha (AAAA-MM-DD, opcional):"));
        add(dateField);
        kindCombo.addActionListener(e -> retakeCheck.setEnabled(kindCombo.getSelectedIndex() == 0));
    }

    /**
     * La nota cargada en el formulario.
     * @throws IllegalArgumentException si la fecha no está vacía y no es AAAA-MM-DD
     */
    public Evaluation toEvaluation(String id) {
        // TODO T09a
        throw new UnsupportedOperationException();
    }
}
```

```java
// src/test/java/com/planificador/ui/CourseFormsTest.java
package com.planificador.ui;

import com.planificador.model.*;
import com.planificador.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CourseFormsTest {

    static final LocalDate HOY = LocalDate.of(2026, 10, 25);

    @TempDir
    Path tmp;

    @Test
    void reglasDelFormulario() throws Exception {
        // TODO T09a
    }

    @Test
    void notaDelFormulario() throws Exception {
        // TODO T09a
    }
}
```

Completá los `// TODO` que pide la tarea T09a. Devolvé **solo los métodos que completaste**, cada uno entero (firma y cuerpo), sin el resto de la clase: un bloque por archivo, con la ruta en la primera línea y después los métodos (y los imports nuevos, si agregaste). Archivos: src/main/java/com/planificador/ui/CourseRulesForm.java, src/main/java/com/planificador/ui/EvaluationForm.java, src/test/java/com/planificador/ui/CourseFormsTest.java.
