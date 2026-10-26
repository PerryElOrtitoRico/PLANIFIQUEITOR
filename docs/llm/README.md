# PLANIFICADOR, etapa Pomodoro y cursada — paquete para LLM locales

Esta etapa del Planificador la programan **modelos locales** de LM Studio (qwen3.5-9b y Gemma 4 E4B), con Claude diseñando y evaluando. Es el mismo método con el que se terminó el ORDENADOR (`../OrdenadorDescargas/docs/llm/`) y aplica desde el primer pedido las reglas de `~/Escritorio/Hobbies/programacion/Proyectos/qwen-aprendizajes.md`.

```
docs/llm/
├── README.md               ← este archivo (método y planilla)
├── 00-especificacion.md    ← el contrato: va (en extracto) en todos los pedidos
├── pedir.py                ← arma el pedido, lo manda a LM Studio, guarda registro y extrae los archivos
├── tareas/                 ← una subtarea por conversación (uno a tres métodos chicos)
├── evaluacion/             ← tests ocultos JUnit 5: NO se le muestran al LLM
├── registros/              ← pedido, respuesta cruda (y razonamiento) de cada intento
└── mejoras-qwen.md         ← retrospectiva por iteración
```

## Método (esqueleto)
1. Claude escribe el modelo completo y, por cada subtarea, el **esqueleto**: package, imports, firmas, javadoc y `// TODO T0x` + `throw new UnsupportedOperationException()`. Compila.
2. Pedido nuevo (`python3 docs/llm/pedir.py docs/llm/tareas/T0x-….md <qwen|gemma> <intento>`): system prompt con un ejemplo del formato, extracto del contrato (las secciones que nombra la cabecera de la tarea), la subtarea con los pasos como código y los archivos actuales. Los `// TODO` de otras subtareas se mandan como `// lo completa otra subtarea (T0y): no lo cambies`.
3. Se extraen los bloques, se comparan las firmas con `javap -p` y se corre `mvn -q -o test`.
4. Si falla: corrección con contrato + subtarea + archivo actual + error recortado con la línea de su código (`--error`). Hasta 3 correcciones.
5. Se copian los tests ocultos de esa subtarea, se corren, se anota el resultado y se sacan.
6. Si después de 3 correcciones no está, se commitea el intento (`test: T0x intento de <modelo> (k/n ocultos)`) y lo termina Claude.
7. Un commit por subtarea: `feat: T0x …`.

Modelo según "Qué modelo para qué": **Gemma** para subtareas cortas y mecánicas con pasos de código; **qwen** para el resto y para corregir. Temperatura 0.2, contexto 8192, PARALLEL 1.

Para evaluar todos los ocultos juntos:
```bash
cp -r docs/llm/evaluacion/java/com src/test/java/
mvn -o test -Dtest='T0*OcultoTest' -Dsurefire.failIfNoSpecifiedTests=false
git clean -fq src/test/java/com/planificador/service/T0* src/test/java/com/planificador/service/CursadaDatos.java src/test/java/com/planificador/ui/T0*
```

Verificado al armar el paquete: los ocultos compilan contra el esqueleto y fallan todos con él (T01–T04d: 30 de 30; T05: 4 de 4; T06–T09b: 12 de 12).

## Planilla de resultados

| Tarea | Método | Modelo | ¿Compila? | Tests propios | Ocultos k/n | Correcciones | Tiempo LLM | Quién | Notas |
|---|---|---|---|---|---|---|---|---|---|
| T03a | esqueleto | qwen | sí (tras arreglar el extractor) | 2/2 | 3/3 | 0 | 1,6 min | qwen | dos archivos en un bloque: lo separó el extractor |
| T04b | esqueleto | qwen | sí | 2/2 | 3/3 | 0 | 2,2 min | qwen | perfecto al primer intento |
| T01 | esqueleto | Gemma | sí | 3/3 | 4/4 | 0 | 1,8 min | Gemma | perfecto al primer intento |
| T02a | esqueleto | Gemma | sí | 3/3 | 3/3 | 0 | 2,1 min | Gemma | perfecto al primer intento |
| T02b | esqueleto | Gemma (+ qwen para corregir) | código sí; test no | 5/6 → 6/6 | 3/3 | 3 | 26 min (Gemma 5,5) | Gemma (código) + test de Claude | cambió una aserción de la tarea ("2026-10-12 is a Sunday"); qwen devolvió el archivo igual dos veces |
| T03b | esqueleto | Gemma | sí | 3/3 | 3/3 | 0 | 2 min | Gemma | perfecto al primer intento, tests sin comentarios |
| T04a | esqueleto | Gemma | sí | 2/2 | 3/3 | 0 | 3,4 min | Gemma | bien al primer intento; comentarios con cuentas mal en el test |
| T04d | esqueleto | Gemma (+ qwen para corregir) | sí, en la 3.ª vuelta | 1/1 | 4/4 | 2 (una cortada) | 15 min | Gemma + corrección de qwen | paso en prosa → `remainingKnown() > 1`; arreglado con el paso en Java |
| T04c | esqueleto, solo métodos | qwen | sí | 2/2 | 4/4 | 1 (la primera cortada) | 10,4 min (el bueno 1,4) | qwen | primer pedido cortado por largo; con `--metodos` perfecto |
| T05 | esqueleto, solo métodos | qwen | sí | 2/2 (+9 viejos) | 4/4 | 0 | 2,4 min | qwen | perfecto al primer intento; JSON viejo carga igual |
| T07 | esqueleto, solo métodos | qwen | sí | 2/2 | 2/2 | 0 | 1 min | qwen | perfecto al primer intento |
| T08 | esqueleto, solo métodos | qwen | sí | 2/2 | 2/2 | 0 | 1,3 min | qwen | bloque sin abrir y métodos hechos `public`: lo absorbe el extractor |
| T06 | esqueleto, solo métodos | Gemma | sí | 2/2 | 2/2 | 0 | 0,6 min | Gemma | perfecto al primer intento |
| T09a | esqueleto, solo métodos | Gemma | sí | 2/2 | 3/3 | 0 | 0,8 min | Gemma | perfecto al primer intento |
| T09b | esqueleto, solo métodos | Gemma | sí | 1/2 → 2/2 | 3/3 | 0 | 1,1 min | Gemma (+ arreglo de Claude a su propia tarea) | la aserción mal era de la tarea, no del modelo |

**Resumen.** 15 subtareas, 21 pedidos (72 min de LLM en total). **46 de 46 ocultos** pasan con el código de los modelos; ninguna subtarea la tuvo que programar Claude. Código bien al primer pedido en 13 de 15 (T04d tuvo un paso en prosa mal leído; T04c se cortó por largo antes de pasar a `--metodos`). Tests propios: uno solo mal del modelo (T02b: Gemma cambió una fecha de la tarea), arreglado por Claude tras 3 correcciones de qwen sin efecto; el de T09b falló por un error de Claude en la tarea. Gemma hizo 8 subtareas (todas mecánicas), qwen 7 y las 4 correcciones.

Al final, en `llm/cursada-pomodoro`: 43 tests propios (incluidos los 9 que ya existían y `PantallasTest`) + 46 ocultos en verde, `mvn -q package` OK. La interfaz se probó con `PantallasTest` (sin pantalla) y abriendo la ventana real con datos inventados (`XDG_DATA_HOME` temporal), recorriendo las 7 pantallas y el Pomodoro con capturas: sin excepciones. No se probó a mano con clics: los diálogos `JOptionPane` (reglas, nota, ajustes del Pomodoro), un Pomodoro de 25 minutos de verdad ni el sonido del aviso.

Herramientas del lado de Claude que nacieron en esta etapa: `pedir.py` (arma el pedido, marca los TODO de otras subtareas, extrae bloques aunque vengan pegados o sin abrir, y con `--metodos` pide solo los métodos y los injerta conservando la firma del esqueleto; `--solo` manda un único archivo en las correcciones), `evaluar.sh` (propios + firmas con `javap` + ocultos) y `aceptar.sh` (borra los `// TODO` sobrantes).
