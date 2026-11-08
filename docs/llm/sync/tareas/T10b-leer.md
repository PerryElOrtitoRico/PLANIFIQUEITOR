<!-- contrato: 1,2,3 | archivos: src/main/java/com/planificador/service/CarpetaDatos.java -->
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
