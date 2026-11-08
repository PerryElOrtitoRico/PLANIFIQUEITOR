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
    /** Rutas relativas que no se pudieron leer en la última lectura: escribir() no las toca. */
    private java.util.Set<Path> ilegiblesLeidos = new java.util.HashSet<>();

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
        Map<Path, String> nuevo = mapa();
        List<String> ilegibles = new ArrayList<>();
        List<Subject> materias = new ArrayList<>();
        List<StudySession> guia = new ArrayList<>();
        List<ImportantDate> fechas = new ArrayList<>();
        List<StudyLog> sesiones = new ArrayList<>();
        int conflictos = 0;

        for (String sub : List.of(SESIONES, MATERIAS, GUIA, FECHAS)) {
            conflictos += conflictos(sub);
            for (Path rel : archivos(sub)) {
                try {
                    Object o = parsear(rel);
                    String id = idDe(o);
                    // Sin id, o con un nombre que no es el de su id (archivo a mano, copia, otra app con un error):
                    // se trata como ilegible para no renombrarlo ni pisar otra versión con el mismo id.
                    if (id == null || id.isBlank() || !rel.getFileName().toString().equals(nombreArchivo(id))) {
                        throw new JsonParseException("id ausente o distinto del nombre: " + rel);
                    }
                    nuevo.put(rel, json(o));
                    if (o instanceof StudyLog l) sesiones.add(l);
                    else if (o instanceof Subject s) materias.add(s);
                    else if (o instanceof StudySession s) guia.add(s);
                    else if (o instanceof ImportantDate f) fechas.add(f);
                } catch (JsonParseException e) {
                    ilegibles.add(texto(rel));
                }
            }
        }

        PomodoroSettings ajustes = new PomodoroSettings();
        if (Files.exists(raiz.resolve(POMODORO))) {
            try {
                ajustes = (PomodoroSettings) parsear(POMODORO);
                nuevo.put(POMODORO, json(ajustes));
            } catch (JsonParseException e) {
                ilegibles.add(texto(POMODORO));
            }
        }

        ilegiblesLeidos = new java.util.HashSet<>();
        for (String t : ilegibles) ilegiblesLeidos.add(Path.of(t));
        boolean cambio = !nuevo.equals(conocido());
        setConocido(nuevo);
        return new Lectura(new Datos(materias, guia, fechas, sesiones, ajustes), conflictos, ilegibles, cambio);
    }

    /** Escribe solo lo que cambió respecto de lo conocido, sin pisar lo que cambió en disco. */
    public Escritura escribir(Datos datos) throws IOException {
        Map<Path, String> deseado = deseado(datos);
        int escritos = 0;
        int borrados = 0;
        List<String> noPisados = new ArrayList<>();

        for (Map.Entry<Path, String> e : deseado.entrySet()) {
            Path rel = e.getKey();
            String antes = conocido().get(rel);
            if (iguales(antes, e.getValue()) || ilegiblesLeidos.contains(rel)) continue;
            if (!iguales(enDisco(rel), antes)) {
                noPisados.add(texto(rel));
                continue;
            }
            escribirAtomico(rel, e.getValue());
            conocido().put(rel, e.getValue());
            escritos++;
        }

        for (Path rel : new ArrayList<>(conocido().keySet())) {
            if (deseado.containsKey(rel)) continue;
            if (iguales(enDisco(rel), conocido().get(rel))) {
                borrar(rel);
                conocido().remove(rel);
                borrados++;
            } else {
                noPisados.add(texto(rel));
            }
        }

        return new Escritura(escritos, borrados, noPisados);
    }

    // ---- ayudas ya hechas ----

    /** Id de una entidad leída; null para los ajustes (no tienen id). */
    static String idDe(Object o) {
        if (o instanceof StudyLog l) return l.getId();
        if (o instanceof Subject s) return s.getId();
        if (o instanceof StudySession s) return s.getId();
        if (o instanceof ImportantDate f) return f.getId();
        return null;
    }

    /** Borra lo que esta instancia escribió y conoce (para deshacer una migración a medias). */
    void deshacerLoEscrito() throws IOException {
        for (Path rel : new ArrayList<>(conocido.keySet())) borrar(rel);
        conocido = new HashMap<>();
    }

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
