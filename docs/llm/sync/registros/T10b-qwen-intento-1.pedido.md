<!-- modelo qwen/qwen3.5-9b, temperatura 0.2, max_tokens 3756, razonamiento no -->

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

# ESTUDIEITOR, etapa carpeta sincronizada — Especificación (contrato)

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

---

# T10b — CarpetaDatos: leer la carpeta

## Objetivo
Completar el `// TODO T10b` de `CarpetaDatos`: el método `leer()`. No cambies nada más. Podés agregar un método `private`.

## Pasos de `leer()` (en orden)
1. `Map<Path, String> nuevo = mapa();`
2. `List<String> ilegibles = new ArrayList<>();`
3. `List<Subject> materias = new ArrayList<>(); List<StudySession> guia = new ArrayList<>(); List<ImportantDate> fechas = new ArrayList<>(); List<StudyLog> sesiones = new ArrayList<>();`
4. `int conflictos = 0;`
5. Para cada `sub` de `List.of(SESIONES, MATERIAS, GUIA, FECHAS)`:
   1. `conflictos += conflictos(sub);`
   2. Para cada `Path rel : archivos(sub)`:
```java
try {
    Object o = parsear(rel);
    nuevo.put(rel, json(o));
    if (o instanceof StudyLog l) sesiones.add(l);
    else if (o instanceof Subject s) materias.add(s);
    else if (o instanceof StudySession s) guia.add(s);
    else if (o instanceof ImportantDate f) fechas.add(f);
} catch (JsonParseException e) {
    ilegibles.add(texto(rel));
}
```
6. Ajustes:
```java
PomodoroSettings ajustes = new PomodoroSettings();
if (Files.exists(raiz.resolve(POMODORO))) {
    try {
        ajustes = (PomodoroSettings) parsear(POMODORO);
        nuevo.put(POMODORO, json(ajustes));
    } catch (JsonParseException e) {
        ilegibles.add(texto(POMODORO));
    }
}
```
7. `boolean cambio = !nuevo.equals(conocido());`
8. `setConocido(nuevo);`
9. `return new Lectura(new Datos(materias, guia, fechas, sesiones, ajustes), conflictos, ilegibles, cambio);`

---

# Archivos actuales

```java
// src/main/java/com/planificador/service/CarpetaDatos.java
package com.planificador.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.planificador.model.ImportantDate;
import com.planificador.model.PomodoroSettings;
import com.planificador.model.StudyLog;
import com.planificador.model.StudySession;
import com.planificador.model.Subject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Los datos como un archivo JSON por entidad, en una carpeta que sincroniza Syncthing.
 * Escribe solo los archivos que cambiaron y nunca pisa uno que cambió en disco desde la última lectura o escritura.
 */
public final class CarpetaDatos {

    public record Datos(List<Subject> subjects, List<StudySession> studySessions, List<ImportantDate> importantDates,
                        List<StudyLog> studyLogs, PomodoroSettings pomodoroSettings) { }

    /** conflictos: copias *.sync-conflict-*.json; ilegibles: rutas relativas; cambio: distinto de lo conocido antes. */
    public record Lectura(Datos datos, int conflictos, List<String> ilegibles, boolean cambio) { }

    /** noPisados: rutas relativas que cambiaron en disco y no se tocaron. */
    public record Escritura(int escritos, int borrados, List<String> noPisados) { }

    static final String SESIONES = "sesiones";
    static final String MATERIAS = "materias";
    static final String GUIA = "guia";
    static final String FECHAS = "fechas";
    static final String AJUSTES = "ajustes";
    static final Path POMODORO = Path.of(AJUSTES, "pomodoro.json");

    private final Path raiz;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    /** Ruta relativa → JSON normalizado de lo último leído o escrito. Los ilegibles nunca entran. */
    private Map<Path, String> conocido = new HashMap<>();

    public CarpetaDatos(Path raiz) {
        this.raiz = raiz;
    }

    public Path raiz() {
        return raiz;
    }

    /** true si no hay ningún archivo de entidad (.json no ignorado) en las subcarpetas. */
    public boolean vacia() throws IOException {
        for (String sub : List.of(SESIONES, MATERIAS, GUIA, FECHAS)) {
            if (!archivos(sub).isEmpty()) return false;
        }
        return !Files.exists(raiz.resolve(POMODORO));
    }

    /** Nombre de archivo para un id: todo carácter fuera de [A-Za-z0-9._-] pasa a '_', y se agrega ".json". */
    static String nombreArchivo(String id) {
        return id.replaceAll("[^A-Za-z0-9._-]", "_") + ".json";
    }

    /** true si el archivo no es de entidad: empieza con '.', no termina en ".json" o tiene ".sync-conflict-". */
    static boolean ignorado(String nombre) {
        return nombre.startsWith(".") || !nombre.endsWith(".json") || nombre.contains(".sync-conflict-");
    }

    /** Ruta relativa → JSON (gson.toJson) de cada entidad de datos, más POMODORO con los ajustes. */
    Map<Path, String> deseado(Datos datos) {
        Map<Path, String> m = mapa();
        for (Subject s : datos.subjects()) m.put(Path.of(MATERIAS, nombreArchivo(s.getId())), json(s));
        for (StudySession s : datos.studySessions()) m.put(Path.of(GUIA, nombreArchivo(s.getId())), json(s));
        for (ImportantDate f : datos.importantDates()) m.put(Path.of(FECHAS, nombreArchivo(f.getId())), json(f));
        for (StudyLog l : datos.studyLogs()) m.put(Path.of(SESIONES, nombreArchivo(l.getId())), json(l));
        m.put(POMODORO, json(datos.pomodoroSettings()));
        return m;
    }

    /** Lee toda la carpeta y deja lo leído como "conocido". */
    public Lectura leer() throws IOException {
        // TODO T10b
        throw new UnsupportedOperationException();
    }

    /** Escribe solo lo que cambió respecto de lo conocido, sin pisar lo que cambió en disco. */
    public Escritura escribir(Datos datos) throws IOException {
        // lo completa otra subtarea (T10c): no lo cambies
        throw new UnsupportedOperationException();
    }

    // ---- ayudas ya hechas ----

    /** Clase de la entidad según la subcarpeta de la ruta relativa. */
    static Class<?> tipo(Path rel) {
        return switch (rel.getName(0).toString()) {
            case SESIONES -> StudyLog.class;
            case MATERIAS -> Subject.class;
            case GUIA -> StudySession.class;
            case FECHAS -> ImportantDate.class;
            default -> PomodoroSettings.class;
        };
    }

    /** Archivos de entidad de una subcarpeta (rutas relativas, ordenadas), sin los ignorados. Vacía si no existe. */
    List<Path> archivos(String subcarpeta) throws IOException {
        Path dir = raiz.resolve(subcarpeta);
        if (!Files.isDirectory(dir)) return new ArrayList<>();
        try (Stream<Path> s = Files.list(dir)) {
            return s.map(p -> p.getFileName().toString())
                    .filter(n -> !ignorado(n))
                    .sorted()
                    .map(n -> Path.of(subcarpeta, n))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }
    }

    /** Cantidad de copias de conflicto de Syncthing (*.sync-conflict-*.json) en una subcarpeta. */
    int conflictos(String subcarpeta) throws IOException {
        Path dir = raiz.resolve(subcarpeta);
        if (!Files.isDirectory(dir)) return 0;
        try (Stream<Path> s = Files.list(dir)) {
            return (int) s.map(p -> p.getFileName().toString())
                    .filter(n -> n.contains(".sync-conflict-") && n.endsWith(".json"))
                    .count();
        }
    }

    /** Lee y parsea un archivo. Lanza JsonParseException si no es JSON válido o está vacío. */
    Object parsear(Path rel) throws IOException {
        String texto = Files.readString(raiz.resolve(rel), StandardCharsets.UTF_8);
        Object o = gson.fromJson(texto, tipo(rel));
        if (o == null) throw new JsonParseException("archivo vacío: " + rel);
        return o;
    }

    /** JSON normalizado del archivo en disco; null si no existe; "\u0000ilegible" si no se puede parsear. */
    String enDisco(Path rel) throws IOException {
        if (!Files.exists(raiz.resolve(rel))) return null;
        try {
            return gson.toJson(parsear(rel));
        } catch (JsonParseException e) {
            return "\u0000ilegible";
        }
    }

    String json(Object o) {
        return gson.toJson(o);
    }

    /** Escribe a un temporal en la misma carpeta y reemplaza (el .tmp lo ignora Syncthing por .stignore). */
    void escribirAtomico(Path rel, String contenido) throws IOException {
        asegurarStignore();
        Path destino = raiz.resolve(rel);
        Files.createDirectories(destino.getParent());
        Path tmp = destino.resolveSibling(destino.getFileName() + ".tmp");
        Files.writeString(tmp, contenido, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    void borrar(Path rel) throws IOException {
        Files.deleteIfExists(raiz.resolve(rel));
    }

    /** Crea .stignore con "*.tmp" si no existe; si existe no lo toca. */
    void asegurarStignore() throws IOException {
        Path f = raiz.resolve(".stignore");
        if (Files.exists(f)) return;
        Files.createDirectories(raiz);
        Files.writeString(f, "*.tmp\n", StandardCharsets.UTF_8);
    }

    /** Ruta relativa con '/' para mostrar y para los tests. */
    static String texto(Path rel) {
        return rel.toString().replace('\\', '/');
    }

    Map<Path, String> conocido() {
        return conocido;
    }

    void setConocido(Map<Path, String> nuevo) {
        this.conocido = nuevo;
    }

    static Map<Path, String> mapa() {
        return new LinkedHashMap<>();
    }

    static boolean iguales(String a, String b) {
        return Objects.equals(a, b);
    }
}
```

Completá los `// TODO` que pide la tarea T10b. Devolvé **solo los métodos que completaste**, cada uno entero (firma y cuerpo), sin el resto de la clase: un bloque por archivo, con la ruta en la primera línea y después los métodos (y los imports nuevos, si agregaste). Archivos: src/main/java/com/planificador/service/CarpetaDatos.java.
