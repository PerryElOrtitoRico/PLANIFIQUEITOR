# ESTUDIEITOR, etapa carpeta sincronizada — paquete para LLM locales

Mismo método que `../README.md` (esqueleto + subtareas con pasos en Java + tests ocultos + `pedir.py --metodos`). `pedir.py` ahora toma la carpeta de la etapa del camino de la tarea (`<etapa>/tareas/<tarea>.md`), así sirve para esta etapa y para la anterior. Ocultos: `evaluar.sh T10a` (o `T10` para todos los de la clase).

Plan: `docs/superpowers/plans/2026-11-08-etapa1-almacenamiento-syncthing.md`.

## Planilla de resultados

| Tarea | Modelo | Ocultos | Correcciones | Tiempo LLM | Notas |
|---|---|---|---|---|---|
| T10a nombres y mapa deseado | Gemma | 4/4 | 0 | 22 s | perfecto al primer intento |
| T10b leer | qwen | 3/3 (+1 que dependía de T10c) | 0 | 38 s | perfecto al primer intento |
| T10c escribir sin pisar | qwen | 10/10 | 0 | 29 s | 1 aserción mal **de Claude** (esperaba en `noPisados` un archivo que no cambió en la PC); corregida la aserción |
| T11a migración | qwen | 4/4 | 0 | 31 s | perfecto al primer intento |
| T11b guardar y recargar | qwen | 5/5 | 0 | 52 s | perfecto; 2 tests viejos que leían el JSON los adaptó Claude |
| T12a nota en DataManager | Gemma | 2/2 | 0 | 14 s | perfecto al primer intento |
| T12b nota en Pomodoro y cronómetro | Gemma | 1/1 | 1 | 44 s | primer pedido con 3 archivos cortado (`length`, 7815 de entrada): se dividió en T12b/T12c; olvidó `noteField.setText("")`, lo agregó con la corrección |
| T12c nota en el Historial | Gemma | (manual) | 0 | 21 s | perfecto al primer intento |

**Resumen.** 8 subtareas, 10 pedidos, ~4,5 min de LLM. 29/29 ocultos con el código de los modelos. Lo hizo Claude: esqueletos, tareas, tests, el cableado de `MainWindow` (tarea 3), el README y los arreglos del revisor (archivo sin id o con otro nombre, ajustes ilegibles, migración a medias, `RuntimeException` en `saveData`).

**Aprendizaje nuevo.** Con `--metodos`, tres archivos de interfaz en un pedido ya pasan las ~7800 de entrada y cortan la respuesta: una subtarea, uno o dos archivos.
