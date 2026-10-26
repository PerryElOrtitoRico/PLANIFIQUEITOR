# Mejoras del pedido a los modelos locales — retrospectiva por iteración

Una entrada por pedido (tarea o corrección), en orden. Cada una: qué pasó, causa probable con evidencia, y el cambio que se prueba en el pedido siguiente (y si funcionó). Las reglas que se confirman pasan a `~/Escritorio/Hobbies/programacion/Proyectos/qwen-aprendizajes.md`.

Parámetros fijos salvo que se diga: temperatura 0.2, contexto 8192, PARALLEL 1, `max_tokens` = 8192 − entrada estimada − 64, razonamiento apagado. Punto de partida: todas las reglas de `qwen-aprendizajes.md` desde el primer pedido (esqueleto, pasos como código, `switch` dado en Java, ejemplo de formato en el system prompt, otros `// TODO` marcados como "lo completa otra subtarea", aserciones exactas para los tests propios).

Cambios nuevos respecto de ORDENADOR, desde el primer pedido:
- El proyecto usa **clases con getters** en `model` y **records** en `service`: el system prompt dice las dos cosas con ejemplos (`log.getMinutes()` / `status.condition()`).
- El system prompt dice "imports que ya están (aunque todavía no se usen)", porque en ORDENADOR borró imports del esqueleto (T04b).
- El marcado "lo completa otra subtarea" lo hace `pedir.py` solo, en el archivo y en el pedido.

| Iteración | Modelo | Entrada / salida | Segundos | Resultado | Causa probable | Cambio para la siguiente → ¿funcionó? |
|---|---|---|---|---|---|---|
| T03a-1 | qwen | 2241 / 1212 | 98 | **Código perfecto al primer intento** (`switch` copiado tal cual, `tick` en orden); test propio con las aserciones exactas de la tarea. Pero devolvió los **dos archivos en un solo bloque**, el segundo con su `// ruta` en el medio, y el extractor metió el test dentro de `PomodoroEngine.java` (no compilaba). Dejó los `// TODO T03a` arriba de cada cuerpo. | El ejemplo del system prompt muestra un archivo por bloque pero no dos; en ORDENADOR ya había pasado sin bloques (T03-1). | Del lado de Claude: el extractor separa un bloque por las líneas `// src/….java` (se re-extrajo la misma respuesta, sin pedir de nuevo: 2/2 propios, 3/3 ocultos). `aceptar.sh` borra los `// TODO` sobrantes. En el pedido no se cambia nada todavía. |
| T04b-1 | qwen | 2768 / 1512 | 134 | **Perfecto al primer intento**: `exams` igual a los pasos, tests propios con las aserciones de la tarea (2/2), otros métodos intactos, imports intactos. **Un bloque por archivo.** | — | Cambio probado: el system prompt dice "un bloque por archivo, nunca dos archivos en el mismo bloque" → **funcionó** (2 bloques). Se mantiene. |
