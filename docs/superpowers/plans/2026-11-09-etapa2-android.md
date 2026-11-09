# Etapa 2: ESTUDIEITOR para Android (reloj, sesiones con nota e Historial) — plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-development (recommended) or executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Franco prefiere que programen los LLM locales con el método de `docs/llm/` (esqueleto + subtareas + tests ocultos + `pedir.py --metodos`) y que los subagentes solo revisen en puntos de control.

**Goal:** una app nativa de Android, instalada por cable en el celular y la tablet, para largar el Pomodoro o el cronómetro, anotar la sesión con nota y ver el Historial, leyendo y escribiendo la misma carpeta que sincroniza Syncthing con la PC.

**Architecture:** proyecto Gradle en `android/`, dentro del repo, igual que ZANCADEITOR (`Running/android`). Una tarea `copiarCompartido` copia a `build/` los paquetes `com.planificador.model` y `com.planificador.service` de la PC: `DataManager`, `CarpetaDatos`, `PomodoroEngine` y las demás corren tal cual en Android, así las reglas son las mismas en los dos. Lo propio de Android va en `com.planificador.app`: `logica` (Java puro, testeable con JUnit en la JVM), `datos` (permisos, rutas, estado local) y `ui` (Activities y Fragments). El reloj se basa en la **hora real**, no en contar ticks: el estado se guarda en disco local, una alarma exacta termina la fase aunque Android haya cerrado la app, y un servicio en primer plano muestra la notificación con la cuenta regresiva y anota un "latido" cada 30 s, para saber hasta cuándo anduvo el reloj si el teléfono se apaga.

**Tech Stack:** Java 17 (Android), Android Gradle Plugin 8.7.3, Gradle 8.10.2, compileSdk/targetSdk 35, minSdk 26, AppCompat 1.7.0, Material 1.12.0, Gson 2.10.1, JUnit 5. Son las mismas dependencias que ZANCADEITOR: no hay ninguna nueva.

**Spec:** `docs/superpowers/specs/2026-11-08-estudieitor-android-sync-design.md` (etapa 2). La etapa 1 (`docs/superpowers/plans/2026-11-08-etapa1-almacenamiento-syncthing.md`) ya está en `main`.

## Global Constraints

- Paquete y applicationId `com.planificador.app`; nombre visible `ESTUDIEITOR`.
- Carpeta de datos en el dispositivo: `/storage/emulated/0/Documents/Estudieitor/datos` (`Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS)/Estudieitor/datos`), con el permiso **acceso a todos los archivos** (`MANAGE_EXTERNAL_STORAGE`), que se pide la primera vez como en el ajedrez.
- Lo que no se sincroniza (estado del reloj, marca de migración) va en `context.getFilesDir()`.
- Los datos se leen y escriben **solo** con `DataManager` (código compartido). La app nunca escribe archivos de la carpeta por su cuenta.
- El código compartido tiene que compilar con Java 17: nada de `switch` con patrones, record patterns ni `getLast()` en `model` y `service`.
- Los campos nuevos de los modelos se agregan en la PC (código compartido), nunca solo en Android (ver "Límites conocidos" del spec).
- Reglas del Pomodoro iguales a las de la PC: cada fase termina, **frena** el reloj y avisa. Un WORK terminado se guarda con los minutos de `workMinutes`. Saltear una fase no guarda nada.
- **Reloj cortado** (el teléfono se apagó o se reinició con el reloj andando, decisión de Franco): se guardan los minutos de estudio hasta el último latido. Menos de 1 minuto no se guarda, y los descansos nunca se guardan. Si Android solo mató la app, sin reiniciar, la alarma igual termina la fase a su hora: eso no cuenta como corte.
- Sin login, sin internet y sin servicios de Google. Se instala con `./gradlew installDebug`.
- Comentarios y textos en español. Commits con Conventional Commits, descripción en español, terminando con `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Nunca `push`.
- `cd android && ./gradlew test` y `mvn -q test` en la raíz, en verde al final de cada tarea.

## Review Focus

1. **Teléfono reiniciado o sin batería con un ciclo de estudio a la mitad:** al volver se guarda la sesión con los minutos hasta el último latido, una sola vez, aunque lleguen juntos el aviso de arranque y la apertura de la app (Tarea 2: `trasReinicioGuardaHastaElUltimoLatido`, `recuperarDosVecesNoDuplica`).
2. **App cerrada por Android y la fase vencida sin que sonara la alarma:** al abrir la app se termina la fase a la hora en que debía terminar y se guarda el ciclo con la hora de fin correcta (Tarea 2: `alAbrirConLaFaseVencidaLaTermina`).
3. **Pausas:** los minutos guardados por un corte no cuentan el tiempo en pausa (Tarea 2: `elTiempoEnPausaNoCuenta`).
4. **Permiso de archivos negado o carpeta sin sincronizar todavía:** la app no crea datos de ejemplo ni escribe nada. Muestra qué falta y un botón para dar el permiso (prueba manual de la Tarea 1).
5. **Sesión registrada en el teléfono mientras la PC está abierta:** llega por Syncthing y aparece en la PC al volver a la ventana. Lo cubre la etapa 1; acá se confirma con la prueba de punta a punta de la Tarea 6.

---

### Task 1: proyecto Android, código compartido y permiso de archivos

**Files:**
- Create: `android/settings.gradle`, `android/build.gradle`, `android/gradle.properties`, `android/gradlew`, `android/gradlew.bat` y `android/gradle/wrapper/*`, copiados de `Running/android` y con `rootProject.name = "estudieitor-android"`. `android/local.properties` lleva `sdk.dir=/home/franco/Android/Sdk` y va en `.gitignore`.
- Create: `android/app/build.gradle`, igual al de `Running/android/app/build.gradle` con estos cambios: `namespace`/`applicationId` `com.planificador.app`, y `copiarCompartido` con `include "com/planificador/model/**", "com/planificador/service/**"`.
- Create: `android/app/src/main/AndroidManifest.xml` con los permisos `MANAGE_EXTERNAL_STORAGE`, `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `USE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE` y la `MainActivity` launcher.
- Create: `android/app/src/main/java/com/planificador/app/datos/Rutas.java`
- Create: `android/app/src/main/java/com/planificador/app/ui/MainActivity.java` y su layout, con dos pestañas abajo (`BottomNavigationView`): **Reloj** e **Historial**, por ahora con texto de prueba.
- Modify: `src/main/java/com/planificador/service/DataManager.java`: agregar `public static synchronized void iniciar(Path dataFile, Path carpeta)`, que crea la instancia con esas rutas y sin ubicaciones viejas.
- Modify: `.gitignore` (`android/**/build/`, `android/.gradle/`, `android/local.properties`).
- Test: `android/app/src/test/java/com/planificador/app/CompartidoTest.java`, `src/test/java/com/planificador/service/DataManagerCarpetaTest.java`.

**Interfaces:**
- Produces:
  - `Rutas.carpetaDatos(): Path`: Documents/Estudieitor/datos.
  - `Rutas.archivoLocal(Context): Path`: `getFilesDir()/planificador_datos.json`. Nunca existe, así que no se migra nada, pero ahí queda el ancla local.
  - `Rutas.tienePermiso(): boolean`: `Environment.isExternalStorageManager()`.
  - `DataManager.iniciar(Path dataFile, Path carpeta)`.
  - `MainActivity.onResume()`: si hay permiso, `DataManager.iniciar(...)` la primera vez y `recargar()` las siguientes. Si no hay permiso, una pantalla con el texto "ESTUDIEITOR necesita acceso a los archivos para leer la carpeta que sincroniza Syncthing (Documents/Estudieitor/datos)." y un botón **Dar permiso** que abre `Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` con el paquete.

- [ ] **Step 1: Write the failing tests:**
  - `DataManagerCarpetaTest.iniciarUsaEsaCarpeta`: `DataManager.iniciar(tmp.resolve("local/planificador_datos.json"), carpeta())` sobre una carpeta con `materias/m1.json` escrito por otro `DataManager` → `DataManager.getInstance().getSubjects().size() == 1`.
  - `CompartidoTest.elCodigoDeLaPcCorreEnAndroid`: en la JVM de los tests de Android, `new CarpetaDatos(tmp).escribir(...)` + `DataManager.iniciar(...)` ven la misma materia, y `new PomodoroEngine(new PomodoroSettings()).secondsLeft() == 1500`.
- [ ] **Step 2: Run** `mvn -q test -Dtest=DataManagerCarpetaTest` → falla (`iniciar` no existe).
- [ ] **Step 3: Implement** lo anterior. `iniciar` es una línea: `instance = new DataManager(dataFile, carpeta, List.of());`.
- [ ] **Step 4: Run** `mvn -q test` y `cd android && ./gradlew test assembleDebug` → todo en verde y `app-debug.apk` generado.
- [ ] **Step 5: Prueba manual (Franco):** `./gradlew installDebug` con el celular por cable. La primera vez muestra el texto y el botón **Dar permiso**; después de darlo y volver, la pestaña Reloj muestra "N materias, M sesiones" (texto de prueba) leídas de la carpeta. Si la carpeta todavía está vacía, muestra 0 y 0, sin crear nada.
- [ ] **Step 6: Commit** `feat: proyecto Android que lee la carpeta sincronizada`.

---

### Task 2: lógica del reloj sobre la hora real (Java puro)

**Files:**
- Create: `android/app/src/main/java/com/planificador/app/logica/EstadoReloj.java`
- Create: `android/app/src/main/java/com/planificador/app/logica/Reloj.java`
- Test: `android/app/src/test/java/com/planificador/app/logica/RelojTest.java`, con ocultos en `docs/llm/android/evaluacion/`.

**Interfaces:**
- Consumes: `PomodoroSettings`, `PomodoroPhase`, `StudyLog`, `StudyLogFactory.fromWorkCycle(...)` (compartidos).
- Produces:
  - `EstadoReloj`: clase con campos públicos, serializable con Gson. `String modo` (`"pomodoro"` o `"cronometro"`); `String subjectId, topicId, topicTitle, note`; `PomodoroPhase phase` (null en el cronómetro); `int ciclos`; `boolean corriendo`; `long finMs` (si corre: cuándo termina la fase); `int restanteSeg` (si no corre); `int faseSeg` (duración total de la fase); `long tramoInicioMs` (cuándo arrancó el tramo actual sin pausa); `int estudiadoSeg` (estudio acumulado en la fase antes de este tramo); `long latidoMs`; `long latidoElapsedMs`; `String idSesion`, el id de la sesión que se guardaría al terminar o cortar esta fase. Se genera al empezar cada fase de estudio (`"pl_" + UUID` en el Pomodoro, `"cron_" + UUID` en el cronómetro, como en la PC). Así, si el aviso de arranque y la apertura de la app recuperan a la vez el mismo estado, las dos guardan el mismo id y pisan el mismo archivo con el mismo contenido: no hay duplicados.
  - `record Resultado(EstadoReloj estado, StudyLog sesion /* null si no hay que guardar */, boolean avisar /* sonar y vibrar */)`.
  - `Reloj` (todo `static`, sin Android):
    - `EstadoReloj nuevoPomodoro(PomodoroSettings s, String subjectId, String topicId, String topicTitle)`: phase WORK, `restanteSeg = faseSeg = work*60`, sin correr.
    - `EstadoReloj nuevoCronometro(int minutos, String subjectId, String topicTitle)`.
    - `EstadoReloj iniciar(EstadoReloj e, long ahoraMs)`: `corriendo = true`, `finMs = ahora + restanteSeg*1000`, `tramoInicioMs = ahora`, `latidoMs = ahora`.
    - `EstadoReloj pausar(EstadoReloj e, long ahoraMs)`: `restanteSeg = ceil((finMs - ahora) / 1000)`, `estudiadoSeg += (ahora - tramoInicioMs) / 1000` si la fase es de estudio, `corriendo = false`.
    - `int restante(EstadoReloj e, long ahoraMs)`: segundos que faltan, nunca negativo.
    - `Resultado terminarFase(EstadoReloj e, PomodoroSettings s)`: con la fase vencida. Pomodoro WORK → sesión con `fromWorkCycle(e.idSesion, …, fin = finMs, minutos = work)` y `note`, `ciclos++`, sigue LONG_BREAK si `ciclos % max(1, cyclesBeforeLongBreak) == 0` o SHORT_BREAK si no. Descanso → WORK. Cronómetro → sesión con todos los minutos y `source = "cronometro"`. En todos los casos `corriendo = false`, `estudiadoSeg = 0`, `avisar = true`, la nota queda vacía después de guardar, y la fase nueva de estudio recibe un `idSesion` nuevo.
    - `Resultado detener(EstadoReloj e, long ahoraMs)`: botón **Terminar** del cronómetro. Guarda los minutos estudiados si son ≥ 1 (como `TimerDialog.registrar`).
    - `EstadoReloj saltear(EstadoReloj e, PomodoroSettings s)`: como `PomodoroEngine.skip()`, sin guardar.
    - `EstadoReloj latido(EstadoReloj e, long ahoraMs, long elapsedMs)`.
    - `Resultado recuperar(EstadoReloj e, PomodoroSettings s, long ahoraMs, long elapsedMs)`: al abrir la app o al arrancar el teléfono. Sin correr → nada. Si `elapsedMs < e.latidoElapsedMs`, el teléfono se reinició: si la fase es de estudio, sesión con `(estudiadoSeg + (latidoMs - tramoInicioMs)/1000) / 60` minutos (≥ 1), fin = `latidoMs`, source según el modo (null en pomodoro, `"cronometro"` en el cronómetro); el estado queda sin correr y vuelve al inicio de esa fase con un `idSesion` nuevo, sin avisar. Si no hubo reinicio y `ahora ≥ finMs` → `terminarFase`. Si no → sigue igual.

- [ ] **Step 1: Ocultos (Claude)** `docs/llm/android/evaluacion/.../T20OcultoTest.java`, copiados después a `RelojTest`. Todos con `ahora` fijo (por ejemplo `long T0 = 1_762_600_000_000L`), sin `System.currentTimeMillis()`:
  - `unCicloTerminadoGuardaLosMinutosDelAjuste`: work 25. `iniciar(T0)` y `terminarFase` → sesión de 25 min, `startTime` = hora de `T0`, phase SHORT_BREAK, `ciclos == 1`, `avisar`, sin correr.
  - `cadaCuatroCiclosVieneElDescansoLargo`: con `cyclesBeforeLongBreak = 4`, el cuarto WORK terminado deja LONG_BREAK.
  - `unDescansoTerminadoNoGuarda`: sesión null y phase WORK.
  - `elTiempoEnPausaNoCuenta`: iniciar en T0, pausar en T0+10 min, iniciar en T0+30 min, latido en T0+35 min y reinicio → sesión de 15 min.
  - `trasReinicioGuardaHastaElUltimoLatido`: iniciar en T0, latido en T0+12 min (elapsed 5000), `recuperar` con elapsed 100 → sesión de 12 min con fin = latido, sin correr, `restanteSeg = 25*60`.
  - `trasReinicioConMenosDeUnMinutoNoGuarda`.
  - `trasReinicioEnUnDescansoNoGuarda`.
  - `recuperarDosVecesNoDuplica`: un segundo `recuperar` sobre el estado devuelto → sesión null; y dos `recuperar` sobre el **mismo** estado original devuelven sesiones con el mismo id.
  - `alAbrirConLaFaseVencidaLaTermina`: iniciar en T0 y `recuperar` en T0+40 min sin reinicio → sesión de 25 min con `startTime` de T0 (fin = `finMs`, no `ahora`).
  - `recuperarConLaFaseEnCursoNoHaceNada`.
  - `saltearNoGuarda`.
  - `cronometroDetenidoGuardaLoEstudiado`: 60 min, iniciar en T0, `detener` en T0+20 min 30 s → 20 min, `source = "cronometro"`.
  - `cronometroConMenosDeUnMinutoNoGuarda`.
  - `laNotaSeGuardaYDespuesQuedaVacia`.
- [ ] **Step 2: Esqueleto (Claude)**: firmas, javadoc y `// TODO T20x`. Compila y los ocultos fallan.
- [ ] **Step 3: Subtareas para los modelos**, en `docs/llm/android/tareas/` con un contrato propio (`00-especificacion.md`, secciones 1 a 3 para esta tarea): T20a `nuevoPomodoro`/`nuevoCronometro`/`iniciar`/`pausar`/`restante` (Gemma); T20b `terminarFase`/`saltear` (qwen); T20c `detener`/`latido`/`recuperar` (qwen). Cada paso con su código en Java, como en la etapa 1.
- [ ] **Step 4: Run** `cd android && ./gradlew test` con los ocultos → 14/14.
- [ ] **Step 5: Commit** uno por subtarea (`feat: T20a …`).

---

### Task 3: alarma, servicio en primer plano y arranque del teléfono

**Files:**
- Create: `android/app/src/main/java/com/planificador/app/datos/AlmacenReloj.java`: lee y escribe `EstadoReloj` en `getFilesDir()/reloj.json` (atómico: temporal y renombrar).
- Create: `android/app/src/main/java/com/planificador/app/ui/ServicioReloj.java`: servicio en primer plano de tipo `specialUse`.
- Create: `android/app/src/main/java/com/planificador/app/ui/AlarmaReloj.java`: `BroadcastReceiver`.
- Create: `android/app/src/main/java/com/planificador/app/ui/ArranqueReceiver.java`: `BOOT_COMPLETED`.
- Create: `android/app/src/main/java/com/planificador/app/ui/ControlReloj.java`: el único punto que aplica un `Resultado`.
- Test: `android/app/src/test/java/com/planificador/app/datos/AlmacenRelojTest.java`.

**Interfaces:**
- Consumes: `Reloj`, `EstadoReloj`, `Resultado` (Tarea 2), `DataManager`.
- Produces:
  - `AlmacenReloj(Path archivo)`: `EstadoReloj leer()` (null si no hay o está dañado) · `void guardar(EstadoReloj e)` · `void borrar()`.
  - `ControlReloj.aplicar(Context c, Resultado r)`: (1) si `r.sesion() != null`, `DataManager.getInstance().addStudyLog(r.sesion())`; (2) guarda el estado; (3) si `r.avisar()`, notificación con sonido y vibración ("Terminó el estudio: 25 min guardados" / "Terminó el descanso"); (4) si el estado corre, programa la alarma exacta en `finMs` y arranca el servicio; si no, cancela la alarma y para el servicio. **El orden importa**: primero se guarda la sesión y después el estado; así un corte entre los dos pasos a lo sumo repite un guardado con el mismo id, que pisa el mismo archivo con el mismo contenido.
  - `ServicioReloj`: notificación persistente con `setUsesChronometer(true)` + `setChronometerCountDown(true)` + `setWhen(finMs)`, botón **Pausar**, y latido cada 30 s (`Reloj.latido` + `AlmacenReloj.guardar`).
  - `AlarmaReloj.onReceive`: `ControlReloj.aplicar(c, Reloj.recuperar(estado, ajustes, System.currentTimeMillis(), SystemClock.elapsedRealtime()))`.
  - `ArranqueReceiver.onReceive`: lo mismo que la alarma (es `recuperar` el que detecta el reinicio).

- [ ] **Step 1: Write the failing test** `AlmacenRelojTest`: guardar y leer devuelve los mismos campos; con un archivo con `{no` → `leer()` devuelve null y no lanza.
- [ ] **Step 2: Implement** (Claude: es pegamento de Android). `AlmacenReloj` lo puede hacer Gemma con los pasos.
- [ ] **Step 3: Run** `./gradlew test assembleDebug` → verde.
- [ ] **Step 4: Commit** `feat: alarma, servicio y arranque del reloj en Android`.

---

### Task 4: pantalla Reloj (Pomodoro y cronómetro)

**Files:**
- Create: `android/app/src/main/java/com/planificador/app/ui/RelojFragment.java` y `res/layout/fragment_reloj.xml`.
- Create: `android/app/src/main/java/com/planificador/app/logica/OpcionesReloj.java`: listas de materias y temas para los selectores, en Java puro.
- Test: `android/app/src/test/java/com/planificador/app/logica/OpcionesRelojTest.java`.

**Interfaces:**
- Consumes: `ControlReloj`, `Reloj`, `AlmacenReloj`, `DataManager`.
- Produces:
  - `OpcionesReloj.materias(List<Subject>): List<String>`: nombres en el orden de la lista. `OpcionesReloj.temas(Subject): List<String>`: `"(sin tema)"` primero y después los títulos de todos los temas de todas sus unidades, en orden. `OpcionesReloj.tema(Subject, int indice): Topic` devuelve null para el índice 0.
  - La pantalla tiene un selector **Pomodoro / Cronómetro**, la materia, el tema, el campo **Nota de la sesión (opcional)** (en el cronómetro, también los minutos), el reloj grande (`MM:SS`, `PomodoroEngine.format`), la fase, "Ciclos completos: N" y los botones **Iniciar/Pausar**, **Saltear** (Pomodoro) y **Terminar** (cronómetro). La materia y el tema se bloquean mientras el reloj corre (como en la PC).
  - Refresca cada segundo con un `Handler` mientras está visible, usando `Reloj.restante(estado, now)`. No cuenta ticks.
  - En `onResume`: `ControlReloj.aplicar(c, Reloj.recuperar(...))` y después muestra el estado.
  - Sin materias en la carpeta: el texto "Todavía no hay materias. Cargalas en la PC: llegan por Syncthing." y el reloj apagado.

- [ ] **Step 1: Write the failing test** `OpcionesRelojTest` (una materia con dos unidades de dos temas → `temas` tiene 5 elementos y empieza con `"(sin tema)"`; `tema(s, 0) == null`; `tema(s, 3)` es el primer tema de la segunda unidad).
- [ ] **Step 2: Implement** `OpcionesReloj` (Gemma) y el Fragment (Claude).
- [ ] **Step 3: Run** `./gradlew test assembleDebug` → verde.
- [ ] **Step 4: Prueba manual (Franco):** Pomodoro con ⚙ Ajustes en 1 minuto (se cambian en la PC y llegan por Syncthing) y una nota: suena y vibra con la pantalla bloqueada, y la sesión aparece en la PC con la nota. Pausar y retomar. Cronómetro de 5 min terminado a los 2 → 2 min guardados. Con un ciclo a la mitad, apagar el teléfono y prenderlo → se guardan los minutos hasta el apagado (±30 s).
- [ ] **Step 5: Commit** `feat: pantalla Reloj en Android`.

---

### Task 5: Historial con nota editable

**Files:**
- Create: `android/app/src/main/java/com/planificador/app/logica/ResumenHistorial.java`
- Create: `android/app/src/main/java/com/planificador/app/ui/HistorialFragment.java` y `res/layout/fragment_historial.xml`.
- Test: `android/app/src/test/java/com/planificador/app/logica/ResumenHistorialTest.java`.

**Interfaces:**
- Produces:
  - `ResumenHistorial.minutosPorDia(List<StudyLog>, YearMonth): Map<LocalDate, Integer>`: solo los días de ese mes que tienen estudio.
  - `ResumenHistorial.delDia(List<StudyLog>, LocalDate): List<StudyLog>`: ordenadas por `startTime` (las null al final).
  - `ResumenHistorial.formatear(int minutos): String`: igual que `HistorialPanel.formatear` de la PC (`"1h05"`, `"45m"`, `"2h"`).
  - La pantalla tiene el mes con flechas ◀ ▶ y **Hoy**, y una grilla de 7 columnas con los minutos de cada día (borde verde si hubo estudio). Al tocar un día, abajo aparece la lista de sus sesiones con hora, materia, tema, minutos, origen y nota. Al tocar una sesión, un diálogo **Nota de la sesión** con la nota actual llama a `DataManager.setStudyLogNote`. En `onResume` se llama a `recargar()`.

- [ ] **Step 1: Write the failing tests** `ResumenHistorialTest`: `minutosPorDia` suma dos sesiones del mismo día e ignora una del mes anterior; `delDia` ordena `10:30` antes que `14:00` y deja la null al final; `formatear(65) == "1h05"`, `formatear(45) == "45m"`, `formatear(120) == "2h"`.
- [ ] **Step 2: Implement** `ResumenHistorial` (Gemma) y el Fragment (Claude).
- [ ] **Step 3: Run** `./gradlew test assembleDebug` → verde.
- [ ] **Step 4: Prueba manual (Franco):** las sesiones de la PC aparecen en el Historial del celular; cambiar una nota en el celular → aparece en la PC al volver a la ventana.
- [ ] **Step 5: Commit** `feat: Historial con nota en Android`.

---

### Task 6: README, ícono y prueba de punta a punta

**Files:**
- Modify: `README.md`: sección "📱 App de Android": qué hace en esta etapa, requisitos (SDK en `~/Android/Sdk`, depuración USB), cómo compilar e instalar (`cd android && ./gradlew test installDebug`), el permiso de archivos, la carpeta `Documents/Estudieitor/datos` y qué pasa si el reloj se corta.
- Create: `android/app/src/main/res/mipmap-*/ic_launcher*` a partir de `src/main/resources/icono.png`.

- [ ] **Step 1: Escribir** la sección del README y el ícono.
- [ ] **Step 2: Run** `mvn -q test` y `cd android && ./gradlew test assembleDebug` → verde.
- [ ] **Step 3: Prueba de punta a punta (Franco):** con Syncthing andando, registrar un Pomodoro en la tablet sin internet, después conectarla. La sesión llega a la PC, y si la PC está abierta aparece al volver a la ventana. Después, en el Panel, cuando la PC manda.
- [ ] **Step 4: Commit** `docs: app de Android en el README`.

## Fuera de esta etapa (etapa 3)

Hoy, Guía semanal (y ligar el Pomodoro a una sesión de la guía), Materias, Fechas clave, Progreso, Tiempo, Cursada y Ajustes del Pomodoro en Android.
