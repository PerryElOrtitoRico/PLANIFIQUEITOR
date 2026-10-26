<!-- modelo google/gemma-4-e4b-qat, temperatura 0.2, max_tokens 5996, razonamiento no -->

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

## 6. Cuenta regresiva
- En Hoy se muestran las fechas importantes de hoy a 30 días: "Primer parcial (Mecánica de Fluidos) en 6 días", "… es mañana", "… es hoy".

---

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

---

# Archivos actuales

```java
// src/main/java/com/planificador/ui/CountdownPanel.java
package com.planificador.ui;

import com.planificador.model.ImportantDate;
import com.planificador.model.Subject;
import com.planificador.service.Countdown;
import com.planificador.service.DataManager;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/** Cuenta regresiva de las fechas importantes de los próximos días, para la pantalla de Hoy. */
public class CountdownPanel extends JPanel {

    static final int DIAS = 30;
    static final Color URGENTE = new Color(225, 29, 72);   // 7 días o menos
    static final Color NORMAL = new Color(51, 65, 85);

    public CountdownPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));
        refresh(LocalDate.now());
    }

    /** Vuelve a armar la lista con la fecha de hoy. */
    public void refresh() {
        refresh(LocalDate.now());
    }

    /** Arma una etiqueta por fecha próxima (de hoy a DIAS días); si no hay, una sola etiqueta que lo dice. */
    void refresh(LocalDate today) {
        // TODO T06
        throw new UnsupportedOperationException();
    }

    /** Las etiquetas que hay ahora (para los tests). */
    java.util.List<JLabel> labels() {
        java.util.List<JLabel> out = new java.util.ArrayList<>();
        for (Component c : getComponents()) if (c instanceof JLabel l) out.add(l);
        return out;
    }
}
```

```java
// src/test/java/com/planificador/ui/CountdownPanelTest.java
package com.planificador.ui;

import com.planificador.model.*;
import com.planificador.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CountdownPanelTest {

    static final LocalDate HOY = LocalDate.of(2026, 10, 25);

    @TempDir
    Path tmp;

    @Test
    void muestraLasProximas() throws Exception {
        // TODO T06
    }

    @Test
    void sinFechas() throws Exception {
        // TODO T06
    }
}
```

Completá los `// TODO` que pide la tarea T06. Devolvé **solo los métodos que completaste**, cada uno entero (firma y cuerpo), sin el resto de la clase: un bloque por archivo, con la ruta en la primera línea y después los métodos (y los imports nuevos, si agregaste). Archivos: src/main/java/com/planificador/ui/CountdownPanel.java, src/test/java/com/planificador/ui/CountdownPanelTest.java.
