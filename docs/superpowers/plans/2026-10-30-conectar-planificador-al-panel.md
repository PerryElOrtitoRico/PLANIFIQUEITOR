---
tipo: proyecto
creado: 2026-10-30
tags:
  - proyecto
---
# Conectar PLANIFICADOR al panel: plan de implementación

> **Para quien implemente:** usar subagent-driven-development (recomendado) o executing-plans, tarea por tarea. Los pasos usan casillas `- [ ]`.

**Objetivo:** que el Planificador de estudio mande solo al panel el estudio (un registro por materia y día), las fechas importantes y las notas de los parciales, sin tocar su funcionamiento cuando el panel no está configurado; y que las piezas de cliente que RUTINEITOR escribió pasen a la librería compartida para no copiarlas por tercera vez.

**Arquitectura:** el Planificador guarda todo en `planificador_datos.json` con un único punto de guardado (`DataManager.saveData()`). Después de cada guardado exitoso, un sincronizador arma los registros que "deberían estar" en el panel, los compara con los últimos que mandó (archivo de estado) y publica solo lo nuevo, lo cambiado y lo borrado (marca `borrado`). Así cubre todos los caminos (Pomodoro, cronómetro, guía, historial, notas, fechas, borrar una materia) sin tocar la interfaz de cada uno. Las clases genéricas del cliente (`ConexionPanel`, `PanelCompartido`, `ValidacionConexion`, `NombreDispositivo`, la regla de claves) se suben de RUTINEITOR a `sync-core`.

**Tecnología:** Java 21, Maven, Swing + FlatLaf, Gson (ya en el Planificador), JUnit 5, `panel:sync-core:1.0.0-SNAPSHOT`.

**Spec:** [Diseño](Dise%C3%B1o.md), [Plan 1](Plan%201%20-%20Panel%20y%20sincronizaci%C3%B3n.md), [Plan 2](Plan%202%20-%20Conectar%20RUTINEITOR.md) y las decisiones de abajo. Contratos del servidor: `README.md` y `docs/detalle-json.md` del repo `Panel`.

**Repos:** `~/Escritorio/Hobbies/programacion/Proyectos/Panel` (Tareas 1 y 2, rama `feature/cliente-compartido`), `.../ProyectoRutina` (Tarea 3, rama `refactor/cliente-compartido`) y `.../PlanificadorEstudio` (Tareas 4 a 7, rama `feature/panel`). Los tres son repositorios git y están en `main` limpios. Al empezar, copiar este plan a `docs/superpowers/plans/` del repo `PlanificadorEstudio`. Las tareas 1 y 2 van primero y se instalan con `mvn -q -pl sync-core -am install -DskipTests`.

## Decisiones de Franco
- Estudio: **un registro por materia y día** (suma todos los ciclos, cronómetros y sesiones de la guía marcadas ese día).
- Al activar el panel por primera vez se manda **todo el historial** existente.
- Fechas importantes: **todas** (parcial, final, tp, entrega) como eventos.
- Notas: **sí**, las de parciales y recuperatorios (los TP no).

## Restricciones globales
- Sin configuración del panel (sin dirección o token, o `enviar=false`) el Planificador se comporta exactamente como hoy: no crea la cola ni el archivo de estado, no hace red, no muestra avisos.
- Un error del panel (servidor caído, token inválido, disco lleno) nunca interrumpe, bloquea ni deshace un guardado: `saveData()` termina igual.
- El token nunca aparece en logs ni mensajes; la ventana usa campo de contraseña; el archivo de configuración queda con permisos 600 cuando el sistema lo permite.
- Archivos en la carpeta de datos del Planificador (`~/.local/share/planificador-estudio/`, o `$XDG_DATA_HOME/planificador-estudio/`): `panel-cliente.properties`, `panel-cola.jsonl`, `panel-estado.json`.
- `fuente = "planificador"`, `dispositivo` el de la configuración, `actualizadoEn` en milisegundos y **estrictamente creciente por id** (`max(ahora, anterior + 1)`), `borrado = false` salvo las marcas de borrado.
- **Estudio:** `tipo = "esfuerzo"`, `area = "estudio"`, `idEnFuente = "est:<subjectId>:<fecha>"`, `fecha` = día del registro de estudio (`StudyLog.date`), `duracionMin` = suma de los minutos de esa materia ese día (si da menos de 1 no se manda), `esfuerzo = null`, `resumen = "<Materia>"` seguido de `" · "` y los primeros 3 temas separados por `", "` (si hay más, `" y N más"`; sin temas, solo la materia), `detalleJson = {"materia":"…","temas":["…"],"ciclos":N}` con todos los temas distintos (título del tema; para sesiones de la guía, su descripción) en orden de aparición por (`startTime`, `id`), sin vacíos, y `ciclos` = cantidad de `StudyLog` del grupo. Los registros de estudio de una materia que ya no existe se ignoran (si estaban mandados, se borran del panel).
- **Fecha importante:** `tipo = "evento"`, `area = "estudio"`, `idEnFuente = "fec:<importantDateId>"`, `fecha` = `ImportantDate.date`, `metrica` = tipo en minúsculas (`parcial`, `final`, `tp`, `entrega`; vacío o desconocido → `fecha`), `resumen = "<Tipo> · <Materia> · <título>"` con el tipo capitalizado (`Parcial`, `Final`, `TP`, `Entrega`; sin materia: `"<Tipo> · <título>"`), `detalleJson = {"materia":"…","tipo":"…","titulo":"…","minutosEstudiados":N}` donde N = minutos de estudio de esa materia con fecha ≤ el menor entre hoy y la fecha del evento (sin materia: sin `materia` ni `minutosEstudiados`).
- **Nota:** `tipo = "resultado"`, `area = "estudio"`, uno por cada número de parcial que tenga nota, usando la nota efectiva (si hay recuperatorio, el recuperatorio reemplaza a la original aunque sea más baja): `idEnFuente = "nota:<subjectId>:parcial<N>"`, `metrica = "nota:<clave de la materia>"` (regla de claves de `sync-core`), `valor` = nota, `fecha` = fecha de esa nota (si no tiene, el día en que el sincronizador la vio por primera vez, guardado en el archivo de estado), `resumen = "Parcial <N>[ (recuperatorio)] · <Materia>: <nota>"` (coma decimal si no es entera), `detalleJson = {"materia","numero","recuperatorio":bool,"notaAprobacion":N}`. Materia con clave vacía → no se manda. El panel usa la **mejor** nota de la métrica para el progreso; es una limitación conocida.
- Fechas inválidas, nulas o sin materia (cuando la regla pide materia) se omiten en silencio y no rompen nada.
- Estilo del Planificador (nombres en inglés en el modelo, textos de interfaz en español, sin colores de relleno ni emojis); mantener su estilo FlatLaf liso.

## Foco de revisión
- Sincronizar dos veces seguidas sin cambios publica cero registros la segunda vez (Tarea 5).
- Borrar un ciclo del historial, desmarcar una sesión de la guía, borrar una fecha, una nota o una materia produce las marcas de borrado correctas; el estudio de un día que queda en 0 minutos se borra del panel (Tarea 5).
- Un grupo que sigue existiendo pero cambia (se agrega otro ciclo ese día, cambia un tema) se reenvía con `actualizadoEn` mayor (Tareas 4 y 5).
- Ciclos que cruzan la medianoche (cuenta el día en que terminó), temas nulos o en blanco, minutos 0, y duplicados guía + cronómetro del mismo día (Tarea 4).
- Archivo de estado dañado o ausente: se vuelve a mandar todo (los envíos son idempotentes) sin lanzar (Tarea 5).
- El hook de guardado no puede romper `saveData()` ni bloquear la ventana más de lo razonable (Tarea 5).
- Panel apagado: cero archivos nuevos, cero hilos, cero red (Tareas 5 y 6).
- La migración de RUTINEITOR no cambia su comportamiento (Tarea 3).

---

### Task 1: Panel: cliente compartido en sync-core

**Files:**
- Modify: `sync-core/src/main/java/panel/core/ConfigPanel.java`
- Create: `sync-core/src/main/java/panel/cliente/ConexionPanel.java`, `PanelCompartido.java`, `ValidacionConexion.java`, `NombreDispositivo.java`, `Clave.java`, `TextosConexion.java`
- Test: `sync-core/src/test/java/panel/cliente/*Test.java`

**Interfaces:**
- Consume: las clases equivalentes que ya existen en RUTINEITOR (`~/Escritorio/Hobbies/programacion/Proyectos/ProyectoRutina/src/main/java/com/rutina/panel/` y sus tests; solo lectura): `ConexionPanel`, `PanelCompartido`, `ValidacionConexion`, `NombreDispositivo`, `ClaveEjercicio`, y de `TextosPanel` solo la función `causa`.
- Produce: en el paquete `panel.cliente`, las mismas clases con la **misma API pública** que en RUTINEITOR (para que migrar sea cambiar importaciones): `ConexionPanel.abrir/guardarConfig/probarValores/activo/publicar/vaciarAhora/probar/url/dispositivo/pendientes/close`, `PanelCompartido(get/reemplazar/reabrir/close)`, `ValidacionConexion.errores(url, token)`, `NombreDispositivo.porDefecto(env)`, `Clave.de(texto)` (la regla de `ClaveEjercicio.de`, con la misma batería de pruebas), `TextosConexion.causa(Throwable)`. Además `ConfigPanel.inactiva()` y `ConexionPanel.inactiva()` públicos (reemplazan al truco de cargar la carpeta actual y al `abrir(null)` que lanzaba una excepción a propósito).

- [ ] **Step 1:** portar los tests de las clases listadas (mismos nombres y aserciones, ajustando paquete) y agregar `configInactivaNoTieneNada`, `conexionInactivaNoCreaNada`, `claveCoincideConLaDeRutineitor` (los mismos casos de `ClaveEjercicioTest`).
- [ ] **Step 2:** `mvn -q -pl sync-core test` falla (clases inexistentes).
- [ ] **Step 3:** portar el código sin cambiar comportamiento, usando `ConfigPanel.inactiva()` donde RUTINEITOR usaba el truco; `ConexionPanel.abrir` sigue sin lanzar nunca.
- [ ] **Step 4:** `mvn -q test` de la raíz limpio; `mvn -q -pl sync-core -am install -DskipTests`; commit `feat: cliente compartido en sync-core`.

### Task 2: Panel: web con temas y minutos estudiados

**Files:**
- Modify: `panel-server/src/main/resources/web/dia.js`, `calendario.js`, `docs/detalle-json.md`, `README.md`

**Interfaces:**
- Produces: (a) el detalle de estudio muestra `Materia: … · Temas: a, b, c` cuando `detalleJson.temas` es un arreglo no vacío (con `tema` suelto sigue funcionando como hoy); (b) la nota de un evento muestra una línea "Llevás <X> de estudio en la materia." cuando su `detalleJson.minutosEstudiados` es un número (formato `1 h 05` / `45 min`); (c) cuando una fecha tiene varios eventos, el tooltip de la celda los lista a todos (hoy muestra solo el primero) y la nota del día muestra una por evento. Todo con `textContent`.

- [ ] **Step 1:** implementar siguiendo las reglas ya establecidas en la web (sin `innerHTML` con datos, tokens de CSS, sin tocar el foco ni los contadores de peticiones).
- [ ] **Step 2:** verificar en el navegador con `scripts/demo.sh` mandando con `curl` un estudio con `temas`, dos eventos el mismo día y uno con `minutosEstudiados`; decir qué se vio y qué no.
- [ ] **Step 3:** documentar los tres campos en `docs/detalle-json.md`; `mvn -q test` limpio; commit `feat: temas y minutos estudiados en la web`.

### Task 3: RUTINEITOR: usar el cliente compartido

**Files:**
- Modify: `pom.xml` (sin cambios si ya depende de `sync-core`), imports en `src/main/java/com/rutina/**` y `src/test/java/com/rutina/**`
- Delete: `src/main/java/com/rutina/panel/{ConexionPanel,PanelCompartido,ValidacionConexion,NombreDispositivo,ClaveEjercicio}.java` y sus tests (ya están portados en `sync-core`); dejar en `TextosPanel` solo lo propio de RUTINEITOR (`historialEnviado`) y que use `TextosConexion.causa`

**Interfaces:**
- Consume: `panel.cliente.*` (Tarea 1, instalada).
- Produce: el mismo comportamiento de RUTINEITOR con menos código propio; `ClaveEjercicio.de` pasa a ser `Clave.de`.

- [ ] **Step 1:** rama `refactor/cliente-compartido` desde `main`; reemplazar las clases por las de `sync-core`; compilar.
- [ ] **Step 2:** `mvn -q test` completo: el resultado debe ser el mismo salvo los tests que se mudaron (anotar cuántos tests menos y por qué); correr la prueba de extremo a extremo opcional contra `scripts/demo.sh` del repo `Panel` (instrucciones en el README de RUTINEITOR) y confirmar que da igual que antes.
- [ ] **Step 3:** `mvn -q package -DskipTests` y confirmar que el jar tiene `panel/cliente/ConexionPanel.class`; commit `refactor: usar el cliente compartido de sync-core`.

### Task 4: Planificador: de sus datos a registros del panel

**Files:**
- Modify: `pom.xml` (dependencia `panel:sync-core:1.0.0-SNAPSHOT`; confirmar que el proyecto compila con Java 21 y que `mvn package` sigue armando el jar con dependencias)
- Create: `src/main/java/com/planificador/panel/RegistrosPanel.java`
- Test: `src/test/java/com/planificador/panel/RegistrosPanelTest.java`

**Interfaces:**
- Consume: `Subject`, `StudyLog`, `ImportantDate`, `Evaluation`, `CourseRules` (modelo existente), `panel.core.Registro`, `panel.cliente.Clave`.
- Produce: `RegistrosPanel.construir(List<Subject> materias, List<StudyLog> estudio, List<ImportantDate> fechas, LocalDate hoy, String dispositivo, Function<String, String> fechaDeNotaSinFecha) -> Map<String, Registro>` (clave = `idEnFuente`, ordenado por inserción: estudio, eventos, notas). Función pura, nunca lanza; los `Registro` salen con `actualizadoEn = 0` (lo asigna el sincronizador) y deben pasar `errores()` salvo ese campo (el test lo comprueba cambiando `actualizadoEn` a 1).

- [ ] **Step 1:** tests en `RegistrosPanelTest` con datos armados a mano: `agrupaCiclosPorMateriaYDia` (suma de minutos, `ciclos`, temas en orden y sin repetir, resumen con 3 temas y `y N más`), `cicloQueTerminaDespuesDeMedianocheCuentaEnElDiaDeFin`, `temasNulosOEnBlancoSeOmiten`, `minutosCeroNoGeneranRegistro`, `estudioDeMateriaInexistenteSeIgnora`, `fechaInvalidaDeUnCicloSeOmite`, `eventoConMateria` (resumen, metrica, `minutosEstudiados` hasta hoy o hasta la fecha), `eventoFuturoNoCuentaEstudioPosterior`, `eventoSinMateria`, `eventoDeTipoDesconocidoUsaMetricaFecha`, `notaUsaElRecuperatorioAunqueSeaMasBaja`, `notaConComaDecimal`, `notaSinFechaUsaLaFechaRecordada`, `notaDeTPNoSeEnvia`, `materiaConClaveVaciaNoMandaNota`, `todosLosRegistrosPasanErrores`.
- [ ] **Step 2:** `mvn -q test -Dtest=RegistrosPanelTest` falla. **Step 3:** implementar. **Step 4:** suite completa limpia. **Step 5:** commit `feat: registros del panel desde los datos del planificador`.

### Task 5: Planificador: sincronizador por diferencias

**Files:**
- Create: `src/main/java/com/planificador/panel/PanelSincronizador.java`, `EstadoPanel.java`
- Modify: `src/main/java/com/planificador/service/DataManager.java` (gancho después de guardar)
- Test: `src/test/java/com/planificador/panel/PanelSincronizadorTest.java`, `EstadoPanelTest.java`, ampliar `DataManagerTest`

**Interfaces:**
- Consume: `RegistrosPanel` (T4), `panel.cliente.ConexionPanel`/`PanelCompartido`.
- Produce:
  - `EstadoPanel(Path archivo)` con `Map<String, Registro> cargar()` (archivo ausente o dañado → vacío, sin lanzar), `void guardar(Map<String, Registro>, Map<String,String> fechasRecordadas)`, `Map<String,String> fechasRecordadas()`; escritura atómica (temporal + mover).
  - `PanelSincronizador(PanelCompartido panel, EstadoPanel estado, DatosPlanificador datos, Clock reloj)` donde `DatosPlanificador` es una interfaz mínima (`materias()`, `estudio()`, `fechas()`) para poder probar sin el singleton, y `Resumen sincronizar()`; `record Resumen(boolean panelActivo, int nuevos, int cambiados, int borrados)`. Si el panel no está activo no hace nada (no toca el estado ni crea archivos). Si lo está: arma lo deseado con `RegistrosPanel`, publica lo nuevo y lo cambiado (comparando todo menos `actualizadoEn` y `dispositivo`) con `actualizadoEn = max(ahora, anterior + 1)`, publica una marca de borrado (`borrado = true`, mismos campos, `actualizadoEn` mayor) por cada id del estado que ya no está, y recién después guarda el estado. Nunca lanza; sincronizar dos veces sin cambios devuelve `Resumen(true,0,0,0)`. Es `synchronized`.
  - `DataManager.setAlGuardar(Runnable)` (un solo oyente): se llama al final de `saveData()` solo si el guardado salió bien, atrapando `RuntimeException`.

- [ ] **Step 1:** tests con una `ConexionPanel` real contra un `HttpServer` falso (patrón de RUTINEITOR) o, donde alcance, un publicador de prueba: `primeraVezMandaTodo`, `segundaVezNoMandaNada`, `cambioEnUnGrupoSeReenviaConActualizadoEnMayor`, `agregarUnCicloCambiaElGrupoDelDia`, `borrarUnCicloMandaMarcaDeBorradoOReenviaElGrupo` (según queden minutos), `estudioQueQuedaEnCeroSeBorra`, `borrarUnaMateriaBorraSusRegistros`, `borrarUnaFechaMandaMarca`, `borrarUnaNotaMandaMarca`, `panelInactivoNoTocaNada` (ni archivo de estado ni cola), `estadoDanadoSeTrataComoVacio`, `actualizadoEnNuncaRetrocede` (reloj fijo o hacia atrás), `unaExcepcionDelPublicadorNoSePropaga`; de `DataManager`: `alGuardarSeLlamaDespuesDeGuardar`, `alGuardarNoSeLlamaSiFallaElGuardado`, `unaExcepcionDelOyenteNoRompeElGuardado`.
- [ ] **Step 2:** fallan. **Step 3:** implementar. **Step 4:** suite completa limpia. **Step 5:** commit `feat: sincronizador del planificador con el panel`.

### Task 6: Planificador: ventana "Conexión con el panel" y arranque

**Files:**
- Create: `src/main/java/com/planificador/ui/DialogoPanel.java`
- Modify: `src/main/java/com/planificador/ui/MainWindow.java` (menú "Panel"), `Main.java` (arranque y cierre)
- Test: lo que se pueda sin pantalla (ver abajo)

**Interfaces:**
- Consume: `panel.cliente.*`, `PanelSincronizador`.
- Produce: menú **Panel → Conexión con el panel…** con un diálogo modal: **Dirección del panel**, **Token** (campo de contraseña + «Mostrar»), **Dispositivo** (por defecto `NombreDispositivo.porDefecto(System.getenv())`), casilla **Enviar al panel**; botones **Probar conexión** (con los valores escritos, en un `SwingWorker`: "Conectado", "Token inválido" o "No responde"; valida antes con `ValidacionConexion`), **Reenviar todo al panel** (borra el archivo de estado y sincroniza en un `SwingWorker`, mostrando "Se pusieron N registros en la cola…" o "Se mandaron N…" con el criterio de RUTINEITOR de mirar lo que queda pendiente), **Guardar** (valida; `guardarConfig`; `PanelCompartido.reabrir` fuera del hilo de eventos; después una sincronización para mandar el historial si es la primera vez o si cambió la configuración) y **Cancelar**. `Main` abre la conexión con la carpeta de datos del Planificador (`DataManager.getInstance().getDataFile().getParent()`), crea el `PanelCompartido`, el `EstadoPanel` y el `PanelSincronizador`, registra `DataManager.setAlGuardar(sincronizador::sincronizar)`, hace una sincronización inicial ya con la ventana visible, y al cerrar la ventana cierra el `PanelCompartido` antes de salir. Con el panel apagado nada de esto cambia el comportamiento.

- [ ] **Step 1:** pasar a funciones puras y probar lo que no depende de Swing (decidir qué texto sale en cada caso, validar, pendientes): `TextosPlanificador` con tests de cada mensaje; el resto del diálogo se escribe sin test automático.
- [ ] **Step 2:** implementar; nada de red ni E/S de archivos en el hilo de eventos salvo el propio guardado ya existente; botones deshabilitados mientras corre un worker; el token nunca en un mensaje.
- [ ] **Step 3:** suite completa limpia. **Step 4:** verificación manual (la hace Franco): abrir el diálogo sin configuración; datos inválidos; probar con token bueno, malo y servidor apagado; guardar y ver el historial en el panel; estudiar un Pomodoro corto y ver que aparece; borrar un ciclo del historial y ver que desaparece; desmarcar «Enviar al panel» y confirmar que ya no se manda nada.
- [ ] **Step 5:** commit `feat: ventana de conexión con el panel`.

### Task 7: Planificador: documentación, prueba de extremo a extremo y empaquetado

**Files:**
- Modify: `README.md`
- Create: `src/test/java/com/planificador/panel/PanelExtremoAExtremoManualTest.java`

- [ ] **Step 1:** prueba opcional habilitada solo con las variables `PANEL_E2E_URL` y `PANEL_E2E_TOKEN` (`@EnabledIfEnvironmentVariable`): con `DataManager.initForTests` y datos de ejemplo (2 materias, ciclos de 3 días, una sesión de la guía, 3 fechas importantes de tipos distintos, notas de dos parciales con un recuperatorio), configura una `ConexionPanel` real contra el panel de demo, sincroniza y consulta `GET /api/dia/<fecha>`, `/api/mes/<yyyy-MM>` y `/api/progreso` verificando: un registro por materia y día con minutos y temas, eventos con su `metrica`, notas con la métrica `nota:<clave>`; sincronizar otra vez no duplica; borrar un ciclo actualiza el día; sin servidor todo queda en la cola. Correrla contra `scripts/demo.sh` y reportar la salida.
- [ ] **Step 2:** `README.md`: sección "Panel" en español (qué se manda, cuándo, cómo configurar, archivos, que sin configuración nada cambia, cómo se arman las claves de materia, que las notas usan la mejor nota, que borrar en el Planificador borra en el panel, límites y cómo correr la prueba opcional); requisito de compilación (instalar `sync-core` desde el repo `Panel`).
- [ ] **Step 3:** `mvn -q package -DskipTests`; el jar contiene `panel/cliente/ConexionPanel.class`; commit `docs: conexión con el panel`.

---

## Revisión del plan frente al diseño
- **Cubierto:** estudio con materia, tema y tiempo (T4, T5); fechas de parciales con cuenta regresiva y horas acumuladas (T2, T4); notas como resultado para la meta de aprobar (T4); panel apagado sin efectos (T5, T6); configuración por dirección y token (T6); contrato de detalle documentado (T2, T7).
- **Fuera de este plan:** asistencias y reglamento de cursada; el programa de running (plan 4); la app web/celular del Planificador (otro proyecto); sueño; notas por parcial con métricas separadas.
- **Decisiones mías:** sincronización por diferencias después de cada guardado en lugar de enviar en cada punto de la interfaz; archivo de estado con el último registro enviado de cada id; un solo id por materia y día; nota efectiva con el recuperatorio reemplazando a la original; fecha recordada para notas sin fecha; subir a `sync-core` el cliente que escribió RUTINEITOR y migrarlo (Tarea 3) para no copiarlo otra vez; botón "Reenviar todo"; sincronización inicial al arrancar.
