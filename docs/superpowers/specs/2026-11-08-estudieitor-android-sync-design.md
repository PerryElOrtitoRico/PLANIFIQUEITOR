# ESTUDIEITOR en Android con sincronización por Syncthing — diseño

Fecha: 2026-11-08. Estado: borrador para revisar.

## Por qué

Franco usa ESTUDIEITOR (app Swing de escritorio) para planificar y registrar su estudio. Cuando estudia en la biblioteca solo tiene la tablet, y cuando se junta a estudiar solo tiene el celular: en esos momentos no puede largar el cronómetro ni anotar qué estudió, y esas sesiones se pierden. Quiere la app en el celular y la tablet, con **el mismo control que en la PC**, y que lo que haga en un dispositivo aparezca solo en los otros.

## Metas

1. **App nativa de Android** de ESTUDIEITOR con todas las pantallas de la PC: Hoy (con cuenta regresiva), Guía semanal, Materias (unidades y temas), Fechas clave, Progreso, Pomodoro, cronómetro suelto, Tiempo, Cursada e Historial, más los Ajustes del Pomodoro.
2. **Funciona sin internet** (fuera de casa es normal quedarse sin conexión). Cada dispositivo tiene su copia completa de los datos.
3. **Sincronización automática** entre la PC, el celular, la tablet y el servidor de casa. Lo hecho en un dispositivo aparece en los otros al abrir la app y cada vez que se guarda (lo que tarde Syncthing en mover los archivos, sin pedir nada al usuario).
4. **Ninguna sesión de estudio se pierde nunca**, aunque se hayan registrado sesiones en dos dispositivos sin conexión.
5. **Nota libre por sesión** ("hice 5 ejercicios de la guía 3"), opcional, visible en todos los dispositivos (nuevo campo; hoy no existe).
6. **El cronómetro / Pomodoro sigue corriendo con la pantalla bloqueada o en otra app**, y avisa al terminar cada fase con sonido y vibración.
7. **Migración sin pérdida** de los datos actuales (`~/.local/share/planificador-estudio/planificador_datos.json`): historial del Pomodoro, guía, materias, cursada (notas y asistencia), fechas y ajustes.

## No metas

- Nada web (ni PWA, ni app web, ni Capacitor/WebView). Decisión explícita de Franco: no le gustan las apps web.
- Varios usuarios o cuentas: es solo para Franco.
- Ver un Pomodoro "en vivo" desde otro dispositivo: alcanza con que la sesión aparezca cuando termina. Nunca va a haber dos dispositivos con ESTUDIEITOR andando a la vez.
- iOS.
- Servidor de sincronización propio ni cambios al Panel.
- Play Store: la app se instala por cable (`adb install`), como las otras apps de Android de Franco.

## Anti-metas (si pasa esto, fracasó aunque funcione)

- **Que se pierda una sesión de estudio.**
- **Que haya que loguearse.** Es su app, nadie más la tiene.
- Que tarde mucho en abrir. (Esperar a que Syncthing sincronice un rato es aceptable; lo que no, es que la app en sí sea lenta.)

## Restricciones

- **Dispositivos:** celular Android 14, tablet Android 15 (Lenovo, nombre en Syncthing `TB330FU`), PC Debian, servidor de casa (`server-franco`) prendido todo el día.
- **Syncthing ya está instalado y emparejado en los cuatro dispositivos** (`debian`, `Celular`, `TB330FU`, `server-franco`). Ya sincroniza las partidas del ajedrez (`ajedrez-partidas`) y las notas de Obsidian. Tailscale también está instalado.
- **Stack:** Java. Escritorio: el Swing + FlatLaf + Gson actual, Maven, JUnit 5. Android: Java nativo, como las apps de Android que ya tiene Franco (`ProyectoRutina/android`, `Running/android`, `Panel/android`: Java, Fragments/Views, minSdk 26, Gradle, se compilan con `./gradlew` y el SDK en `~/Android/Sdk`). Antes de agregar una dependencia nueva, decirle a Franco cuál y por qué (regla de su `CLAUDE.md`).
- **Convenciones del `CLAUDE.md` de Proyectos** (español en docs/comentarios, paquetes `model`/`service`/`ui`, lógica testeable fuera de la UI, guardado atómico, commits chicos con Conventional Commits).
- El repo `PlanificadorEstudio` ya es Git (rama `main`, remoto `PLANIFIQUEITOR`). Nunca `push` sin que lo pida.

## Decisiones tomadas (por Franco)

- App **nativa de Android**, no web.
- **Mismo control en todos los dispositivos:** todas las pantallas.
- **Los datos de estudio son de ESTUDIEITOR**, no del Panel. El Panel sigue siendo una app aparte que recibe un resumen.
- **Dónde viven los datos: como en el ajedrez**, en una carpeta sincronizada por Syncthing, con un archivo por cosa.
- **Conflictos:** si se editó lo mismo en dos dispositivos, **gana el último cambio guardado**, salvo las **sesiones de estudio, que se suman y nunca se pisan**.
- **Cuándo se sincroniza:** al abrir la app y cada vez que se guarda.
- Las sesiones se ven en los otros dispositivos al terminar (no en vivo).
- Nota libre por sesión: sí. Aviso con pantalla bloqueada: sí.
- La app Swing sigue siendo la de la PC.
- Sin fecha límite, pero cuanto antes pueda probarlo, mejor.

## Cómo funciona (diseño)

### Almacenamiento: una carpeta de Syncthing, un archivo por cosa

Modelo tomado del ajedrez (`Ajedrez/README.md`, "Dónde guarda los datos"): cada partida es un `.pgn` propio en una carpeta que sincroniza Syncthing, escritura atómica (`.tmp` + renombrar), sin índice en disco (se rearma al abrir), y la app avisa si Syncthing dejó copias en conflicto.

Para ESTUDIEITOR:

- **Carpeta Syncthing nueva** `estudieitor-datos`, compartida entre los cuatro dispositivos. El servidor de casa hace de punto de encuentro: así una sesión hecha en la tablet sin la PC prendida llega igual a la PC más tarde.
- **Un archivo JSON por entidad**, con el `id` (que todas las entidades ya tienen) en el nombre:
  - `sesiones/<id>.json`: un `StudyLog` (ciclo de Pomodoro, cronómetro o sesión de la guía marcada). **Se crean y no se editan** salvo la nota o un borrado explícito. Como cada una es un archivo nuevo con id único, dos dispositivos sin conexión nunca chocan: las sesiones se suman solas. Esto cumple "las sesiones nunca se pisan".
  - `materias/<id>.json`: un `Subject` con sus unidades, temas y cursada (`CourseTracking`).
  - `guia/<id>.json`: un `StudySession` de la guía semanal.
  - `fechas/<id>.json`: un `ImportantDate`.
  - `ajustes/pomodoro.json`: `PomodoroSettings`.
- **Último cambio gana** para todo lo que no es sesión: si dos dispositivos editan el mismo archivo, Syncthing se queda con la versión de fecha de modificación más nueva y guarda la otra como `*.sync-conflict-*`. Eso es exactamente la regla de Franco. La app **ignora** esos archivos al cargar y **muestra un aviso discreto** con cuántos hay (como hace el ajedrez), sin pedirle nada.
- **Borrar** una entidad = borrar su archivo (Syncthing propaga el borrado).
- **Escritura atómica** por archivo (temporal + renombrar). Un archivo ilegible no tira abajo la carga: se saltea, se avisa y se deja tal cual (no se pisa).

**Regla crítica para el builder:** la app **solo escribe los archivos de lo que cambió**. Nunca reescribe toda la carpeta desde su memoria: si lo hiciera, una PC con datos viejos en memoria pisaría las sesiones o cambios que acaban de llegar por Syncthing. Hoy `DataManager` guarda todo el JSON entero en cada guardado; eso tiene que cambiar.

**Recarga:** al abrir la app y antes/después de cada guardado se relee la carpeta (es barata: son archivos chicos). Una sesión que llega por Syncthing mientras la app está abierta aparece a más tardar en la próxima recarga.

### Ubicación de la carpeta

- PC: `~/Documentos/Carpeta personal/Estudieitor/Datos/` (mismo lugar que el ajedrez: `~/Documentos/Carpeta personal/Ajedrez/Partidas/`).
- Android: `Documents/Estudieitor/datos` en el almacenamiento compartido, con el permiso de "acceso a todos los archivos" que se pide la primera vez (como el ajedrez), para que Syncthing pueda leer y escribir esa carpeta.
- Esto se aparta a propósito de la regla de `~/.local/share/<programa>/` del `CLAUDE.md`, porque los datos tienen que estar en una carpeta que Syncthing comparta, igual que el ajedrez.
- **Lo que no se sincroniza** se queda en `~/.local/share/planificador-estudio/` como hoy: la configuración del Panel (`panel-cliente.properties`, con el token), `panel-cola.jsonl` y `panel-estado.json`. El token nunca va a la carpeta sincronizada.

### Migración

Al primer arranque de la versión nueva en la PC, si la carpeta de Syncthing está vacía y existe `planificador_datos.json`, se convierte a archivos sueltos. El JSON original **no se borra** (queda como respaldo). La migración tiene que poder verificarse: misma cantidad de sesiones, materias, fechas, etc., y los mismos minutos totales. Los dispositivos Android arrancan vacíos y reciben todo por Syncthing.

### App de Android

- Todas las pantallas listadas en Metas, con el mismo comportamiento que la PC (mismas reglas de Pomodoro, cálculo de cursada, cuenta regresiva y estadísticas de Tiempo). Adaptadas a pantalla táctil y a dos tamaños (celular y tablet); el diseño visual queda a cargo del builder.
- **Pomodoro y cronómetro con pantalla bloqueada:** tienen que seguir contando con la app en segundo plano o la pantalla apagada y avisar al terminar cada fase con sonido y vibración. Por ejemplo, con un servicio en primer plano y una notificación con el tiempo restante.
- **Si el reloj se corta sin que Franco lo frene** (Android mata la app, se apaga o reinicia el dispositivo, se queda sin batería): se guardan **los minutos estudiados hasta ese momento** como sesión (decisión de Franco). Vale para un ciclo de estudio del Pomodoro a medio terminar y para el cronómetro. Para eso el estado del reloj (qué materia/tema, fase, inicio) se guarda en disco local (no en la carpeta sincronizada) y se actualiza seguido; al volver a abrir la app o al reiniciar el dispositivo, se registra la sesión con los minutos hasta el último momento en que se sabe que el reloj andaba. Mismo mínimo que el cronómetro de la PC: menos de 1 minuto no se guarda. Los descansos nunca se guardan. *Decisión del diseño:* saltear una fase a propósito sigue sin guardar nada, igual que en la PC; lo nuevo solo cubre los cortes que Franco no eligió. La PC no cambia esta regla.
- **Nota de la sesión:** al terminar un ciclo o cronómetro se puede escribir una nota opcional. También se puede agregar o editar después desde el Historial.
- Sin login ni configuración de cuenta: lo único que hay que hacer la primera vez es dar el permiso de archivos.

### App de escritorio (cambios)

- `DataManager` pasa a leer y escribir la carpeta de archivos sueltos (con la regla de escribir solo lo que cambió y releer al abrir y al guardar).
- Nota de sesión: campo nuevo en `StudyLog` (opcional; las sesiones viejas no tienen). Se muestra y se edita en el Historial, y se puede escribir al terminar un ciclo o cronómetro.
- Aviso de copias en conflicto.
- **La PC puede quedar abierta mientras Franco estudia en la tablet** (decisión del diseño). Hoy la interfaz cambia los objetos en memoria y llama a `saveData()`; con datos viejos en memoria pisaría en silencio lo que llegó por Syncthing (sin copia de conflicto, porque para Syncthing no hubo edición simultánea). Para evitarlo: (1) al volver a la ventana (foco) se recarga la carpeta y se refrescan las pantallas; (2) antes de escribir el archivo de una entidad se compara con lo que se leyó (fecha de modificación o contenido): si cambió en disco desde la última lectura, **no se pisa**, se recarga esa entidad y se avisa a Franco que tiene que repetir el cambio. Las sesiones nuevas no tienen este problema (son archivos nuevos).

### Panel

- **Solo la PC le sigue mandando al Panel**, como hoy. Las sesiones del celular o la tablet llegan al Panel cuando la PC las recibe por Syncthing y hace su próximo envío (al abrir la app o al guardar). Franco confirmó que le alcanza con eso: no hace falta que el servidor de casa le mande al Panel. Así el token del Panel no está en el celular y no se duplica nada.
- El resumen que se manda no cambia (un registro por materia y por día). La nota libre no se manda al Panel.
- Ojo con las guardas de borrado del Panel: una recarga que todavía no recibió los archivos de Syncthing no debe interpretarse como "se borraron cosas". Ver el README del Planificador, sección "Guardas contra borrados por error".

### Configuración de Syncthing

Crear la carpeta `estudieitor-datos` y compartirla en los cuatro dispositivos lo hace Franco a mano (o se le dan los pasos exactos). La carpeta del celular y la tablet tiene que apuntar a la misma ruta que usa la app.

## Etapas sugeridas

Para que Franco pueda probarlo cuanto antes:

1. **Nuevo almacenamiento en la PC + migración**, con la nota de sesión. La app de escritorio sigue igual por fuera.
2. **App Android con Pomodoro, cronómetro y registro de sesiones (con nota) + Historial**, leyendo y escribiendo la misma carpeta. Acá Franco ya puede estudiar en la biblioteca y ver las sesiones en la PC.
3. **Resto de las pantallas en Android**: Hoy, Guía semanal, Materias, Fechas, Progreso, Tiempo, Cursada, Ajustes.

## Lo que queda a criterio del builder

- Cómo compartir la lógica entre escritorio y Android (los paquetes `model` y `service` son Java puro con Gson y `java.time`, que andan en Android con minSdk 26). Por ejemplo, un módulo común instalado en el Maven local, como se hizo con `panel:sync-core`, o copiar el código. Lo importante es que las reglas de Pomodoro, cursada y estadísticas den lo mismo en los dos.
- El formato exacto de cada JSON y los nombres de archivo, mientras el id esté en el nombre y una sesión sea un archivo.
- Cómo se ve la app de Android.
- Cómo detectar cambios que llegan por Syncthing con la app abierta (relectura periódica, `WatchService`, `FileObserver`, o solo en los puntos de recarga definidos).

## Límites conocidos

- **Editar la misma materia en dos dispositivos sin conexión** (por ejemplo, asistencia en la tablet y una nota en la PC): como la materia entera es un archivo, gana la última y el otro cambio queda solo en la copia de conflicto. Es la regla que pidió Franco, pero acá abarca toda la materia. Las sesiones de estudio no tienen este problema.
- Syncthing decide el "último" por la fecha de modificación del archivo, así que depende de que los relojes de los dispositivos estén bien.
