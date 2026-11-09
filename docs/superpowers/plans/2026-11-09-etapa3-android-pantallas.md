# Etapa 3: el resto de las pantallas en Android — plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: executing-plans. Franco pidió "haz la etapa 3" y se fue: Claude programa directo (como la etapa 2), con un revisor al final. Rama `android/etapa3`; no se mergea sin Franco.

**Goal:** que la app de Android tenga el mismo control que la PC: Hoy, Guía semanal, Materias, Fechas clave, Progreso, Tiempo, Cursada y Ajustes del Pomodoro, además del Reloj y el Historial de la etapa 2.

**Architecture:** las reglas ya son código compartido (`DataManager`, `Countdown`, `StudyTimeStats`, `CourseStatusCalculator`). Android agrega solo la interfaz y la lógica chica que en la PC vive dentro de Swing, extraída a `com.planificador.app.logica` con tests en la JVM. Las pantallas leen siempre de `DataManager` al refrescarse (en `onResume` y después de cada cambio), así no guardan objetos viejos después de una recarga.

**Tech Stack:** el de la etapa 2, sin dependencias nuevas.

**Spec:** `docs/superpowers/specs/2026-11-08-estudieitor-android-sync-design.md` (meta 1: "todas las pantallas de la PC").

## Global Constraints

- Los mismos de la etapa 2 (`docs/superpowers/plans/2026-11-09-etapa2-android.md`).
- Los cambios se guardan **solo** con la API de `DataManager` que usa la PC: `addStudySession`, `removeStudySession`, `setSessionCompleted`, `addSubject`, `removeSubject`, `addImportantDate`, `removeImportantDate`, `setPomodoroSettings`, o cambiando el objeto y llamando a `saveData()`, como `SubjectsPanel` y `CoursePanel`.
- Los mismos textos, validaciones y valores por defecto que la PC: tema obligatorio, 60 min por defecto, fecha `AAAA-MM-DD` validada con `Countdown.daysUntil`, tipos de fecha `parcial/final/tp/entrega`, ids `ss_<millis>`, `u_<uuid>`, `t_<uuid>`, `ev_<millis>`.
- Navegación: barra de abajo con **Hoy · Reloj · Guía · Historial · Más**. **Más** abre una lista con Materias, Fechas clave, Progreso, Tiempo, Cursada y Ajustes del Pomodoro, y cada una se abre encima, con la flecha atrás para volver.

## Review Focus

1. **Cronómetro de una sesión de la guía**: su id tiene que empezar con `cron_<idSesión>_<fecha>_`, como en la PC, así marcar la sesión como hecha ese día no duplica los minutos (`DataManager.setSessionCompleted`).
2. **Borrar una materia** borra también sus sesiones de la guía y sus fechas (lo hace `removeSubject`), con una confirmación antes.
3. **Datos raros que llegan de la PC** (materia sin unidades, sesión sin materia, fecha mal escrita): ninguna pantalla se cierra con un error.
4. **El "hecho" de la guía vale solo esa semana** (`StudySession.isCompleted` mira `completedWeek`): Hoy y Guía muestran lo mismo que la PC.

---

### Task 1: navegación, días de la semana y pantalla Hoy
- `logica/DiasSemana.java`: `CLAVES = {"lunes","martes","miercoles","jueves","viernes","sabado","domingo"}`, `NOMBRES = {"Lunes",…,"Domingo"}`, `String clave(LocalDate)`. Test `DiasSemanaTest`: el 2026-11-09 (lunes) da `"lunes"` y el 2026-11-15 (domingo) da `"domingo"`.
- `ui/SesionDialogo.java`: alta y edición de una sesión de la guía (materia, día, tema y minutos), igual que `WeeklyPanel.editarSesion` y `TodayPanel.showAddTodaySessionDialog`. Borrar pide confirmación ("¿Eliminar la sesión: …?").
- `ui/HoyFragment.java`: las fechas importantes de los próximos 30 días (`Countdown.upcoming` + `Countdown.text`, en rojo si faltan 7 días o menos) y las sesiones de hoy, cada una con su casilla de hecha (`setSessionCompleted`), **▶ Cronómetro**, Editar y Borrar, más el botón **Agregar estudio hoy**.
- `MainActivity`: 5 pestañas y `MasFragment` con la lista. `ui/Pantallas.abrir(activity, fragment, titulo)` abre una pantalla encima, con back stack.

### Task 2: Guía semanal
- `ui/GuiaFragment.java`: los 7 días con sus sesiones (casilla, ▶, Editar, Borrar) y un **+** por día. El total de minutos planeados de cada día en el encabezado.
- Cronómetro ligado a la sesión: `Reloj.nuevoCronometro(minutos, subjectId, topicTitle, prefijoId)` con `prefijoId = "cron_" + sesion.getId() + "_" + hoy + "_"`; `EstadoReloj.prefijoId` se usa en `empezarFase`. Test en `RelojTest`: `cronometroDeUnaSesionDeLaGuiaUsaElPrefijo`. El botón ▶ arma el estado (sin iniciarlo) y abre la pestaña Reloj.

### Task 3: Materias
- `ui/MateriasFragment.java`: lista de materias. **Nueva materia** pide nombre, profesor, horario y color, como la PC. Cada materia se despliega con sus unidades, y cada unidad con sus temas: casilla de completado, renombrar y eliminar. **Agregar unidad**, y **Agregar temas** (uno por línea, como la PC). **Eliminar materia** pide confirmación.

### Task 4: Fechas clave
- `ui/FechasFragment.java`: lista ordenada por fecha, con los días que faltan (`Countdown.daysUntil`) y **Eliminar**. **Agregar fecha** pide título, tipo, materia ("General" si no hay), fecha `AAAA-MM-DD` y notas, y avisa si la fecha no es válida.

### Task 5: Progreso y Tiempo
- `logica/ResumenProgreso.java`: `record Resumen(int minutosPlaneados, int minutosHechos, int temas, int temasHechos, int materias)` y `Resumen de(List<Subject>, List<StudySession>)`, con las mismas cuentas que `StatsPanel`. Test `ResumenProgresoTest`.
- `ui/ProgresoFragment.java`: las 4 cifras y, por materia, una barra con los temas hechos y los min/semana planeados.
- `ui/TiempoFragment.java`: semana con ◀ ▶ y **Esta semana**: por materia, planeado, hecho y avance (`StudyTimeStats.compareWeek`), y debajo las últimas 8 semanas (`minutesBySubject`).

### Task 6: Cursada
- `ui/CursadaFragment.java`: elegir materia, ver la condición y las líneas de `CourseStatusCalculator.describe`, **Reglas de cursada** (diálogo con asistencia mínima, nota para regularizar y para promocionar, cantidad de parciales con su casilla de recuperatorio cada uno, y clases del cuatrimestre), **Nota** (parcial o TP, número, si es recuperatorio, nota y fecha), borrar nota, y asistencia del día (**Presente**, **Ausente**, borrar).
- `logica/FormularioCursada.java`: lee los números del diálogo y valida los rangos de `CourseRulesForm`/`EvaluationForm`. Test `FormularioCursadaTest`: nota fuera de 0–10 rechazada, fecha inválida rechazada, y la lista de recuperatorios ajustada a la cantidad de parciales.

### Task 7: Ajustes del Pomodoro y README
- `ui/AjustesFragment.java`: los 4 valores (`setPomodoroSettings`), mínimo 1. El Pomodoro en curso conserva su duración y los nuevos valores rigen en la próxima fase.
- README: la sección de Android pasa a "todas las pantallas".
