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

Verificado al armar el paquete: los ocultos de T01–T04d compilan contra el esqueleto y fallan todos con él (30 de 30).

## Planilla de resultados

| Tarea | Método | Modelo | ¿Compila? | Tests propios | Ocultos k/n | Correcciones | Tiempo LLM | Quién | Notas |
|---|---|---|---|---|---|---|---|---|---|
| T03a | esqueleto | qwen | sí (tras arreglar el extractor) | 2/2 | 3/3 | 0 | 1,6 min | qwen | dos archivos en un bloque: lo separó el extractor |
| T04b | esqueleto | qwen | sí | 2/2 | 3/3 | 0 | 2,2 min | qwen | perfecto al primer intento |
