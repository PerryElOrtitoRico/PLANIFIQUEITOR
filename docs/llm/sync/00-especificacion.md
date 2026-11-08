# ESTUDIEITOR, etapa carpeta sincronizada — Especificación (contrato)

*2026-11-08 · Se le da al LLM en todas las tareas (las secciones que usa cada una). Si una tarea y este documento no coinciden, manda este documento.*

Los datos del Planificador pasan de un solo JSON a una carpeta con un archivo JSON por entidad, que sincroniza Syncthing entre la PC, el celular y la tablet.

## 1. Reglas de código
- Java 21, Maven, Gson. Paquete base `com.planificador`: `model` (datos), `service` (lógica y persistencia), `ui` (Swing). No toques `pom.xml` ni agregues dependencias.
- Nombres de clases y métodos como el código que ya existe; comentarios en español.
- Las clases de `model` tienen getters: `log.getId()`, `s.getName()`. Los **records** se leen con `x()`: `datos.subjects()`, `lectura.cambio()`. No existen `getSubjects()` en los records.
- Tests con JUnit 5 (`org.junit.jupiter.api.*`); archivos con `@TempDir`.

## 2. Código que ya existe
```java
// model
StudyLog(String id, String subjectId, String topicId, String topicTitle, String date, String startTime, int minutes)  // getId()
Subject(String id, String name, String color, String professor, String schedule)    // getId(), getName(), setName(), setColor()
StudySession(String id, String dayOfWeek, String subjectId, String topicDescription, int durationMinutes, boolean completed) // getId()
ImportantDate(String id, String title, String subjectId, String date, String type, String notes) // getId()
PomodoroSettings()  // 25, 5, 15, 4 ;  PomodoroSettings(int work, int shortBreak, int longBreak, int cycles); getWorkMinutes()

// service.CarpetaDatos
record Datos(List<Subject> subjects, List<StudySession> studySessions, List<ImportantDate> importantDates,
             List<StudyLog> studyLogs, PomodoroSettings pomodoroSettings)
record Lectura(Datos datos, int conflictos, List<String> ilegibles, boolean cambio)
record Escritura(int escritos, int borrados, List<String> noPisados)
static final String SESIONES = "sesiones", MATERIAS = "materias", GUIA = "guia", FECHAS = "fechas", AJUSTES = "ajustes";
static final Path POMODORO = Path.of("ajustes", "pomodoro.json");
private Map<Path, String> conocido;            // ruta relativa → JSON normalizado de lo último leído o escrito
// ayudas ya hechas (no las cambies):
List<Path> archivos(String subcarpeta)          // rutas relativas ordenadas de los .json de entidad; vacía si no existe la carpeta
int conflictos(String subcarpeta)               // cantidad de *.sync-conflict-*.json
Object parsear(Path rel)                        // lee y parsea con la clase de su subcarpeta; lanza JsonParseException si es inválido
String enDisco(Path rel)                        // JSON normalizado del archivo; null si no existe; "\u0000ilegible" si no parsea
String json(Object o)                           // gson.toJson(o), con el mismo formato que enDisco
void escribirAtomico(Path rel, String contenido) // también crea .stignore la primera vez
void borrar(Path rel)
static String texto(Path rel)                   // "sesiones/l1.json" (con '/')
Map<Path, String> conocido();  void setConocido(Map<Path, String> nuevo);  static Map<Path, String> mapa(); // LinkedHashMap nuevo
static boolean iguales(String a, String b)      // Objects.equals
```

## 3. Formato de la carpeta
- `sesiones/<nombreArchivo(id)>` (StudyLog), `materias/…` (Subject), `guia/…` (StudySession), `fechas/…` (ImportantDate), y `ajustes/pomodoro.json` (PomodoroSettings).
- `nombreArchivo(id)`: cada carácter fuera de `[A-Za-z0-9._-]` pasa a `_`, y se agrega `.json`.
- Se ignoran los archivos que empiezan con `.`, los que no terminan en `.json` y los que contienen `.sync-conflict-`.
- Las rutas de los mapas son **relativas** a la raíz: `Path.of("sesiones", "l1.json")`.

## 4. Escribir sin pisar
Para cada entidad, el JSON deseado se compara con `conocido`:
- Igual → no se hace nada.
- Distinto → si `enDisco(ruta)` no es igual a `conocido.get(ruta)` (los dos pueden ser null), el archivo cambió en otro dispositivo: **no se escribe** y la ruta va a `noPisados`. Si es igual, se escribe y se actualiza `conocido`.
- Ruta que está en `conocido` y ya no está en los datos → si `enDisco` es igual a lo conocido, se borra y se saca de `conocido`; si no, va a `noPisados`.
- Los archivos ilegibles nunca están en `conocido`, así que nunca se pisan ni se borran.
