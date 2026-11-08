# Etapa 1: almacenamiento por archivos para Syncthing — plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-development (recommended) or executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** que la app de escritorio guarde y lea sus datos como un archivo JSON por entidad en una carpeta que sincroniza Syncthing, migrando el JSON actual sin pérdida, sin pisar lo que llegue de otros dispositivos, y con nota libre por sesión.

**Architecture:** una clase nueva `CarpetaDatos` (servicio, sin Swing) lee la carpeta y escribe solo los archivos que cambiaron, comparando contra lo último leído o escrito; si un archivo cambió en disco desde entonces, no lo pisa y lo informa. `DataManager` mantiene su API pública (listas + `saveData()`), así la interfaz casi no cambia: por dentro usa `CarpetaDatos`, migra el JSON viejo una sola vez y suma `recargar()`. `MainWindow` recarga al volver a la ventana.

**Tech Stack:** Java 21, Swing + FlatLaf, Gson 2.10.1, JUnit 5, Maven. Sin dependencias nuevas.

**Spec:** `docs/superpowers/specs/2026-11-08-estudieitor-android-sync-design.md`

La etapa 2 (app Android) va en un plan aparte, escrito cuando esta etapa esté terminada: reutiliza el formato de carpeta que define este plan.

## Global Constraints

- Carpeta sincronizada en la PC: `~/Documentos/Carpeta personal/Estudieitor/Datos/` (`user.home` + `Documentos/Carpeta personal/Estudieitor/Datos`).
- Un archivo por entidad: `sesiones/<id>.json` (StudyLog), `materias/<id>.json` (Subject completo, con unidades, temas y cursada), `guia/<id>.json` (StudySession), `fechas/<id>.json` (ImportantDate), `ajustes/pomodoro.json` (PomodoroSettings).
- La app solo escribe los archivos de lo que cambió; nunca reescribe toda la carpeta.
- Escritura atómica por archivo (temporal en la misma carpeta + `ATOMIC_MOVE`, con la misma caída a move común que hoy usa `DataManager.saveData`).
- Se ignoran al leer: archivos con `.sync-conflict-` en el nombre (se cuentan si terminan en `.json`), los que no terminan en `.json` y los que empiezan con `.`.
- Un archivo ilegible se saltea, se avisa y nunca se pisa ni se borra.
- Lo del Panel (`panel-cliente.properties`, `panel-cola.jsonl`, `panel-estado.json`) sigue en `~/.local/share/planificador-estudio/` y nunca va a la carpeta sincronizada.
- El JSON viejo (`planificador_datos.json`) no se borra.
- Comentarios, README y mensajes en español; commits con prefijo Conventional Commits en inglés y descripción en español, terminando con `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Nunca `push`.
- `mvn test` en verde al final de cada tarea.

## Review Focus

1. **Archivo borrado en otro dispositivo con la PC abierta:** la PC no debe resucitarlo en el próximo guardado (test en Tarea 1: `noResucitaUnArchivoBorradoEnOtroDispositivo`).
2. **Carpeta vaciada a mano o todavía sin sincronizar después de migrar:** no se vuelve a migrar el JSON viejo encima (test en Tarea 2: `noMigraDosVeces`).
3. **Archivo a medio sincronizar o dañado:** no se pisa, no se borra y el Panel no manda borrados mientras exista (`confiable()` falso) (tests en Tarea 1 `unArchivoIlegibleNoSeTocaNiSeBorra` y Tarea 2 `unArchivoIlegibleDejaElAviso`).
4. **Copias de conflicto y temporales de Syncthing en la carpeta:** se ignoran al cargar (test en Tarea 1 `ignoraConflictosYTemporales`).
5. **Ids con caracteres raros** (`/`, espacios) no deben escaparse de la subcarpeta (test en Tarea 1 `unIdConBarraNoSaleDeLaSubcarpeta`).

---

### Task 1: `CarpetaDatos`, lectura y escritura por archivo

**Files:**
- Create: `src/main/java/com/planificador/service/CarpetaDatos.java`
- Test: `src/test/java/com/planificador/service/CarpetaDatosTest.java`

**Interfaces:**
- Produces:
  - `public record Datos(List<Subject> subjects, List<StudySession> studySessions, List<ImportantDate> importantDates, List<StudyLog> studyLogs, PomodoroSettings pomodoroSettings)` (record anidado `CarpetaDatos.Datos`).
  - `public record Lectura(Datos datos, int conflictos, List<String> ilegibles, boolean cambio)`: `conflictos` = cantidad de `*.sync-conflict-*.json`; `ilegibles` = rutas relativas de los archivos que no se pudieron leer; `cambio` = si el contenido leído es distinto del de la lectura o escritura anterior.
  - `public record Escritura(int escritos, int borrados, List<String> noPisados)`: `noPisados` = rutas relativas que cambiaron en disco y no se tocaron.
  - `public CarpetaDatos(Path raiz)`
  - `public Path raiz()`
  - `public boolean vacia()`: no hay ningún `.json` de entidad.
  - `public Lectura leer() throws IOException`: lee todo y deja como "último conocido" lo leído.
  - `public Escritura escribir(Datos datos) throws IOException`
  - `static String nombreArchivo(String id)`: reemplaza todo carácter fuera de `[A-Za-z0-9._-]` por `_` y agrega `.json`.

- [ ] **Step 1: Write the failing tests** en `CarpetaDatosTest` (con `@TempDir`):
  - `guardaUnArchivoPorEntidadYLoVuelveALeer`: escribir `Datos` con 1 materia `m1`, 1 guía `g1`, 1 fecha `f1`, 2 sesiones `l1`/`l2`, ajustes con `workMinutes=30` → existen `materias/m1.json`, `guia/g1.json`, `fechas/f1.json`, `sesiones/l1.json`, `sesiones/l2.json`, `ajustes/pomodoro.json`; un `new CarpetaDatos(raiz).leer()` devuelve los mismos ids, `workMinutes == 30`, `conflictos == 0`, `ilegibles` vacía.
  - `soloEscribeLoQueCambio`: tras escribir y volver a escribir los mismos datos, `escritos == 0`; cambiar el nombre de la materia → `escritos == 1`.
  - `borrarUnaEntidadBorraSuArchivo`: quitar `l2` de la lista y escribir → `borrados == 1` y `sesiones/l2.json` no existe.
  - `noPisaUnArchivoQueCambioEnOtroDispositivo`: escribir; modificar a mano `materias/m1.json` (otro nombre); cambiar en memoria el color de `m1` y escribir → `noPisados == ["materias/m1.json"]` y el archivo conserva el nombre puesto a mano.
  - `noResucitaUnArchivoBorradoEnOtroDispositivo`: escribir; borrar a mano `fechas/f1.json`; cambiar otra cosa y escribir con `f1` todavía en memoria → `fechas/f1.json` sigue sin existir y figura en `noPisados`.
  - `sumaSesionesNuevasQueLlegaron`: escribir; crear a mano `sesiones/l9.json` (copia de `l1` con id `l9`); `leer()` → 3 sesiones y `cambio == true`; un segundo `leer()` → `cambio == false`.
  - `ignoraConflictosYTemporales`: crear `materias/m1.sync-conflict-20261108-120000-ABCDEFG.json`, `materias/m1.json.tmp`, `materias/.syncthing.m1.json.tmp` → `leer()` da 1 materia y `conflictos == 1`.
  - `unArchivoIlegibleNoSeTocaNiSeBorra`: escribir `sesiones/roto.json` con `{no es json`; `leer()` → `ilegibles == ["sesiones/roto.json"]`; `escribir(...)` de lo leído → el archivo sigue con `{no es json`.
  - `unIdConBarraNoSaleDeLaSubcarpeta`: `nombreArchivo("a/b c")` es `"a_b_c.json"`.
  - `creaStignoreConTemporales`: tras el primer `escribir`, `raiz/.stignore` existe y contiene la línea `*.tmp`; si ya existía con otro contenido, no se toca.

- [ ] **Step 2: Run** `mvn -q test -Dtest=CarpetaDatosTest` → falla al compilar (no existe `CarpetaDatos`).

- [ ] **Step 3: Implement `CarpetaDatos`.** Gson con `setPrettyPrinting()`. Guarda un `Map<Path, String>` con el contenido último leído o escrito de cada archivo. Algoritmo de `escribir`:

```
deseado = mapa ruta -> json de cada entidad de datos
para cada (ruta, json) en deseado:
    si conocido[ruta] == json: seguir
    enDisco = contenido actual del archivo, o null si no existe
    si enDisco != conocido[ruta] (null si no lo conocía): noPisados += ruta; seguir
    escribir atómico; conocido[ruta] = json; escritos++
para cada ruta en conocido que no está en deseado:
    si contenido en disco == conocido[ruta]: borrar; quitar de conocido; borrados++
    si no: noPisados += ruta
```

Los ilegibles nunca entran en `conocido`, así que nunca se pisan ni se borran. `cambio` en `leer()` = el mapa nuevo es distinto del anterior.

- [ ] **Step 4: Run** `mvn -q test -Dtest=CarpetaDatosTest` → PASS.

- [ ] **Step 5: Commit** `feat: carpeta de datos con un archivo por entidad`.

---

### Task 2: `DataManager` sobre la carpeta, con migración y recarga

**Files:**
- Modify: `src/main/java/com/planificador/service/DataManager.java`
- Modify: `src/test/java/com/planificador/service/DataManagerTest.java`

**Interfaces:**
- Consumes: `CarpetaDatos` (Tarea 1).
- Produces (además de toda la API pública actual, que no cambia):
  - Constructor de paquete `DataManager(Path dataFile, Path carpeta, List<Path> legacyFiles)`. `dataFile` sigue siendo `~/.local/share/planificador-estudio/planificador_datos.json`: es el origen de la migración y el ancla de los archivos del Panel (`Main` y `EstadoPanel.junto` lo siguen usando igual).
  - `static Path resolveDataFolder(Path home)` → `home/Documentos/Carpeta personal/Estudieitor/Datos`.
  - `public static synchronized void initForTests(Path dataFile)`: misma firma; la carpeta pasa a ser `dataFile.getParent().resolve("sync")`. Así `UiDatos` y las pruebas de UI siguen andando (su JSON se migra al construir).
  - `public Path getDataFolder()`
  - `public boolean recargar()`: relee la carpeta; si hubo cambio, reemplaza las listas, llama al oyente `alGuardar` (para que el Panel mande lo que llegó por Syncthing) y devuelve `true`.
  - `public int getConflictCount()`: copias de conflicto de la última lectura.
  - `getLoadWarning()`: no null si el JSON viejo estaba dañado al migrar **o** hay archivos ilegibles en la carpeta (lista las rutas). `DatosDeDataManager.confiable()` no cambia y así el Panel no manda borrados.

- [ ] **Step 1: Adapt and write the failing tests** en `DataManagerTest`:
  - Mantener sin cambios: `usaXdgDataHome…`, `usaLocalShare…`, `ignoraXdgDataHomeVacio`, `migraElArchivoViejoSinBorrarlo`, `entreVariosArchivosViejos…`, `prefiereElArchivoNuevo…`, las de la guía (`marcarUnaSesion…`, `elHechoDeLaGuia…`, `siSeCronometro…`) y las de `alGuardar`, construyendo con la carpeta en `tmp/sync`. `alGuardarNoSeLlamaSiFallaElGuardado` provoca el fallo haciendo que la carpeta no se pueda escribir.
  - `guardaYVuelveACargarDesdeElMismoArchivo` → renombrar a `guardaYVuelveACargarDesdeLaCarpeta`: un `DataManager` nuevo sobre la misma carpeta ve lo guardado.
  - `unArchivoDanadoSeRespaldaAntesDeReemplazarlo`: igual que hoy, pero sobre la migración (JSON viejo dañado → copia `.danado-`, aviso, arranca vacío, carpeta vacía).
  - `carpetaPorDefecto`: `resolveDataFolder(Path.of("/home/u"))` es `/home/u/Documentos/Carpeta personal/Estudieitor/Datos`.
  - `migraElJsonALaCarpetaSinPerderNada`: con un JSON de 2 materias (una con cursada: 2 evaluaciones y 3 asistencias), 2 guías, 1 fecha, 3 sesiones (25, 25, 50 min) y ajustes `workMinutes=30` → la carpeta tiene 2+2+1+3 archivos de entidad + `ajustes/pomodoro.json`; un `DataManager` nuevo lee las mismas cantidades, 100 minutos en total, las 2 evaluaciones y 3 asistencias, y `workMinutes == 30`; el JSON viejo sigue existiendo con el mismo contenido.
  - `noMigraDosVeces`: después de migrar, borrar todos los archivos de la carpeta y construir otro `DataManager` → arranca vacío (no vuelve a migrar). La marca es el archivo `migrado-a-carpeta` junto a `dataFile` (local, no sincronizado).
  - `recargarVeLoQueLlegoDeOtroDispositivo`: crear a mano `sesiones/x.json` → `recargar()` devuelve `true`, aparece la sesión y se llamó a `alGuardar`; un segundo `recargar()` devuelve `false`.
  - `noPisaLoQueCambioEnOtroDispositivo`: modificar a mano `materias/m1.json`; cambiar en memoria otro campo de `m1` y `saveData()` → el archivo conserva lo puesto a mano, el `saveErrorListener` recibe un mensaje que contiene `otro dispositivo`, y `getSubjectById("m1")` devuelve lo del disco.
  - `unArchivoIlegibleDejaElAviso`: `sesiones/roto.json` inválido → `getLoadWarning()` contiene `sesiones/roto.json`.
  - `cuentaLasCopiasDeConflicto`: un `materias/m1.sync-conflict-….json` → `getConflictCount() == 1`.

- [ ] **Step 2: Run** `mvn -q test -Dtest=DataManagerTest` → fallan los tests nuevos.

- [ ] **Step 3: Implement.** El constructor ejecuta `migrateLegacyFile` (como hoy), migra si `carpeta.vacia()` y no existe la marca `migrado-a-carpeta` (lee el JSON con el código actual de `loadData`, escribe con `CarpetaDatos.escribir` y crea la marca; con el JSON dañado aplica el respaldo y el aviso actuales y no crea la marca), y después hace `recargar()` sin llamar al oyente. `saveData()` llama a `escribir` con las listas actuales. Si `noPisados` no está vacío, recarga y avisa por `saveErrorListener` con este texto: `"Algunos datos cambiaron en otro dispositivo mientras la app estaba abierta y no se pisaron (N). Se cargó la versión más nueva; repetí el cambio si hacía falta."`. Ante una `IOException`, el mensaje de error es el de hoy. El oyente `alGuardar` se sigue llamando solo si no hubo `IOException`. Ya no se escribe el JSON viejo.

- [ ] **Step 4: Run** `mvn -q test` → PASS completo (incluye las pruebas de UI, que usan `UiDatos` → migración).

- [ ] **Step 5: Commit** `feat: guardar los datos en la carpeta sincronizada y migrar el JSON`.

---

### Task 3: recargar al volver a la ventana y avisar conflictos

**Files:**
- Modify: `src/main/java/com/planificador/ui/MainWindow.java`

**Interfaces:**
- Consumes: `DataManager.recargar()`, `getConflictCount()`, `saveErrorListener` (Tarea 2).

- [ ] **Step 1: Implement.** En `initUI()`:
  - Un `WindowAdapter.windowActivated`: si `DataManager.getInstance().recargar()` devuelve `true`, `refreshAllPanels()`.
  - Al `saveErrorListener` actual se le suma `refreshAllPanels()` después del diálogo (tras un "no se pisaron", las pantallas muestran lo recargado).
  - Si `getConflictCount() > 0` al abrir, un aviso no bloqueante en el título: `ESTUDIEITOR — N copia(s) en conflicto en la carpeta de datos`.

- [ ] **Step 2: Run** `mvn -q test` → PASS (`PantallasTest` sigue armando todas las pantallas).

- [ ] **Step 3: Prueba manual (para Franco):** abrir la app, crear a mano un archivo de sesión copiando uno de `sesiones/` con otro id, pasar a otra ventana y volver: la sesión aparece en el Historial. Con la app abierta, editar a mano `materias/<id>.json` (cambiar el nombre), y en la app cambiar el color de esa materia: aparece el aviso y la materia muestra el nombre editado a mano.

- [ ] **Step 4: Commit** `feat: recargar los datos al volver a la ventana`.

---

### Task 4: nota libre por sesión

**Files:**
- Modify: `src/main/java/com/planificador/model/StudyLog.java` (campo `note`, `getNote()`, `setNote(String)`; null = sin nota)
- Modify: `src/main/java/com/planificador/service/DataManager.java`
- Modify: `src/main/java/com/planificador/ui/PomodoroPanel.java`, `TimerDialog.java`, `HistorialPanel.java`
- Test: `src/test/java/com/planificador/service/DataManagerTest.java`, `src/test/java/com/planificador/ui/PomodoroPanelTest.java`

**Interfaces:**
- Produces: `public void setStudyLogNote(String logId, String note)` en `DataManager`: texto recortado; vacío → null; guarda.

- [ ] **Step 1: Write the failing tests:**
  - `DataManagerTest.laNotaDeUnaSesionSeGuarda`: `setStudyLogNote("l1", "  hice 5 ejercicios de la guía 3 ")` → tras un `DataManager` nuevo, `getNote()` es `"hice 5 ejercicios de la guía 3"`; `setStudyLogNote("l1", "  ")` → `null`.
  - `DataManagerTest.unaSesionViejaNoTieneNota`: sesión migrada sin `note` → `getNote() == null`.
  - `PomodoroPanelTest.elCicloGuardaLaNotaEscrita`: con el campo de nota en `"guía 3"`, `saveCycle()` → la última sesión tiene `note == "guía 3"` y el campo queda vacío.

- [ ] **Step 2: Run** `mvn -q test -Dtest=DataManagerTest,PomodoroPanelTest` → falla.

- [ ] **Step 3: Implement.**
  - `PomodoroPanel`: un `JTextField` "Nota de la sesión (opcional)" accesible al test (paquete); `saveCycle()` pone la nota (recortada, vacía → null) y vacía el campo.
  - `TimerDialog`: el mismo campo; `registrar` lo usa.
  - `HistorialPanel`: columna "Nota" en el detalle y botón **Editar nota…** que, con una fila elegida, pide el texto con `JOptionPane.showInputDialog` (con la nota actual como valor inicial) y llama a `setStudyLogNote`. Sin fila elegida, el botón está apagado.

- [ ] **Step 4: Run** `mvn -q test` → PASS.

- [ ] **Step 5: Prueba manual (para Franco):** escribir una nota en el Pomodoro, terminar un ciclo (con ⚙ Ajustes en 1 minuto) y verla en el Historial; editarla desde ahí; usar el cronómetro de una sesión de la guía con nota.

- [ ] **Step 6: Commit** `feat: nota libre en cada sesión de estudio`.

---

### Task 5: README y configuración de Syncthing

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Actualizar** la sección "💾 Dónde se guardan los datos": la carpeta nueva y su estructura (un archivo por entidad), la migración única desde `planificador_datos.json` (que queda como respaldo) y la marca `migrado-a-carpeta`, la regla de "no pisa lo que cambió en otro dispositivo", las copias de conflicto y que lo del Panel sigue en `~/.local/share/planificador-estudio/`. Agregar la nota de sesión en Funciones. Corregir en "Cosas a tener en cuenta" la advertencia de "una sola copia del programa por carpeta de datos": ahora la carpeta sí se comparte por Syncthing, y el token no está en ella.

- [ ] **Step 2: Agregar** la subsección "Sincronizar con el celular y la tablet (Syncthing)", con los pasos para crear la carpeta `estudieitor-datos` en esta PC con la ruta de arriba y compartirla con `Celular`, `TB330FU` y `server-franco`. En el celular y la tablet va en `Documents/Estudieitor/datos`, y en el servidor, en la ruta que elija Franco.

- [ ] **Step 3: Run** `mvn -q package` → genera `target/planificador-estudio-desktop-1.0.0-jar-with-dependencies.jar` sin errores.

- [ ] **Step 4: Commit** `docs: carpeta sincronizada y Syncthing en el README`.
