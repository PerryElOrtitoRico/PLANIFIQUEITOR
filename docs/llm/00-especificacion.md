# PLANIFICADOR, etapa Pomodoro y cursada — Especificación (contrato)

*2026-10-25 · Se le da al LLM en todas las tareas (las secciones que usa cada una). Si una tarea y este documento no coinciden, manda este documento.*

Etapa nueva del Planificador de Estudio (Java + Swing + FlatLaf + Gson): Pomodoro, tiempo por materia, seguimiento de cursada y cuenta regresiva en Hoy. Los datos siguen en el mismo JSON.

## 1. Reglas de código
- Java 17 (el `pom.xml` compila con `source/target` 17), Maven. Paquete base `com.planificador`: `model` (datos), `service` (lógica y persistencia), `ui` (Swing). No toques `pom.xml` ni agregues dependencias.
- Nombres de clases y métodos en inglés, como el código que ya existe; comentarios en español.
- Las clases de `model` tienen getters: `log.getMinutes()`, `rules.getPassGrade()`, `ev.isRetake()`. Los **records** de `service` se leen con `x()`: `status.condition()`, `a.percent()`. No existen `getCondition()` ni `getPercent()`.
- La lógica no usa `LocalDate.now()`: la fecha de hoy se pasa por parámetro (así se testea).
- Tests con JUnit 5 (`org.junit.jupiter.api.*`); archivos con `@TempDir`. Ningún test lee ni escribe `~/.local/share/planificador-estudio`.

## 2. Modelo (`com.planificador.model`, ya escrito)
```java
StudyLog(String id, String subjectId, String topicId, String topicTitle, String date /*YYYY-MM-DD*/, String startTime /*HH:mm*/, int minutes)
PomodoroSettings(int workMinutes, int shortBreakMinutes, int longBreakMinutes, int cyclesBeforeLongBreak)  // por defecto 25, 5, 15, 4
CourseRules(double minAttendancePercent, double passGrade, double promotionGrade, int examCount, List<Boolean> retakes, int plannedClasses)
    // por defecto 75, 6, 8, 2, [true, true], 0;  boolean hasRetake(int examNumber)  (examNumber empieza en 1)
Evaluation(String id, String kind /*Evaluation.PARCIAL o Evaluation.TP*/, int number, boolean retake, double grade, String date)
AttendanceRecord(String date, boolean present)
CourseTracking(CourseRules rules, List<Evaluation> evaluations, List<AttendanceRecord> attendance)  // getters nunca null
Subject: CourseTracking getCourse() (null si no hay) · CourseTracking getOrCreateCourse()
StudySession (ya existía): la guía semanal; getSubjectId(), getDurationMinutes() = minutos planeados por semana
ImportantDate (ya existía): getTitle(), getSubjectId(), getDate() (YYYY-MM-DD)
```
Records y enums de `service` (ya escritos):
```java
enum CourseCondition { PROMOCION, REGULAR, LIBRE, EN_CURSO }
enum ExamState { PENDIENTE, A_RECUPERAR, APROBADO, DESAPROBADO }
enum PomodoroPhase { WORK, SHORT_BREAK, LONG_BREAK }
record AttendanceSummary(int registered, int present, int absent, double percent, boolean failed, boolean remainingKnown, int remainingAbsences)
record ExamResult(int number, Double original, Double retake, Double effective, ExamState state)
record CourseStatus(CourseCondition condition, AttendanceSummary attendance, List<ExamResult> exams, boolean canPromote, boolean canRegularize)
record SubjectWeekTime(String subjectId, int plannedMinutes, int actualMinutes)
```

## 3. Pomodoro
- Empieza en `WORK`. Al terminar un `WORK` se suma un ciclo; si la cantidad de ciclos es múltiplo de `cyclesBeforeLongBreak` sigue `LONG_BREAK`, si no `SHORT_BREAK`. Después de un descanso sigue `WORK`.
- Cada `WORK` terminado se guarda como un `StudyLog` (materia, tema opcional, día y hora de inicio, minutos = `workMinutes`). Saltear una fase no guarda nada.

## 4. Tiempo por materia
- Semana = de lunes a domingo. Estudiado = suma de `StudyLog.minutes` con fecha en esa semana. Planeado = suma de `StudySession.durationMinutes` de la guía semanal (la guía es la misma todas las semanas).

## 5. Cursada (por materia)
- **Asistencia**: `porcentaje = presentes * 100 / registradas` (100 si no hay ninguna). Si se sabe la cantidad de clases (`plannedClasses > 0`): `faltasPermitidas = floor(plannedClasses * (100 - minAttendancePercent) / 100)`, `quedan = faltasPermitidas - ausentes`, y queda libre por faltas si `quedan < 0`. Si no se sabe: libre por faltas si hay clases registradas y `porcentaje < minAttendancePercent`.
- **Parciales** 1…`examCount`. Nota efectiva = la del recuperatorio si hay; si no, la del parcial. Estado: sin nota → `PENDIENTE`; efectiva ≥ `passGrade` → `APROBADO`; menor, sin recuperatorio rendido y con recuperatorio permitido → `A_RECUPERAR`; si no → `DESAPROBADO`. Si hay dos notas del mismo parcial (o del mismo recuperatorio), vale la última cargada.
- Los **TP** se cargan y se muestran, pero no cambian la condición.
- **Condición**, en este orden: `LIBRE` si quedó libre por faltas o hay un parcial `DESAPROBADO`; `EN_CURSO` si hay un parcial `PENDIENTE` o `A_RECUPERAR`, o si `plannedClasses > 0` y todavía hay menos clases registradas; `PROMOCION` si `promotionGrade > 0` y todas las notas efectivas ≥ `promotionGrade`; si no, `REGULAR`.
- Todavía puede promocionar si no está libre, `promotionGrade > 0` y cada parcial está sin nota, tiene efectiva ≥ `promotionGrade` o está `A_RECUPERAR`. Todavía puede regularizar si no está libre.

## 6. Cuenta regresiva
- En Hoy se muestran las fechas importantes de hoy a 30 días: "Primer parcial (Mecánica de Fluidos) en 6 días", "… es mañana", "… es hoy".

## 7. Datos (JSON)
- Mismo archivo de siempre. Campos nuevos, todos opcionales: `studyLogs` (lista), `pomodoroSettings` (objeto) en la raíz, y `course` en cada materia. Un JSON viejo, sin esos campos, carga igual (listas vacías y ajustes por defecto).

## 8. Tareas
T01 `Countdown` · T02a–b `StudyTimeStats` · T03a–b `PomodoroEngine` y `StudyLogFactory` · T04a–d `CourseStatusCalculator` · T05 `DataManager` · T06–T09 interfaz (`CountdownPanel`, `PomodoroPanel`, `StudyTimePanel`, `CoursePanel`).
