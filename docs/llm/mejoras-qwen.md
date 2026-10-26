# Mejoras del pedido a los modelos locales — retrospectiva por iteración

Una entrada por pedido (tarea o corrección), en orden. Cada una: qué pasó, causa probable con evidencia, y el cambio que se prueba en el pedido siguiente (y si funcionó). Las reglas que se confirman pasan a `~/Escritorio/Hobbies/programacion/Proyectos/qwen-aprendizajes.md`.

Parámetros fijos salvo que se diga: temperatura 0.2, contexto 8192, PARALLEL 1, `max_tokens` = 8192 − entrada estimada − 64, razonamiento apagado. Punto de partida: todas las reglas de `qwen-aprendizajes.md` desde el primer pedido (esqueleto, pasos como código, `switch` dado en Java, ejemplo de formato en el system prompt, otros `// TODO` marcados como "lo completa otra subtarea", aserciones exactas para los tests propios).

Cambios nuevos respecto de ORDENADOR, desde el primer pedido:
- El proyecto usa **clases con getters** en `model` y **records** en `service`: el system prompt dice las dos cosas con ejemplos (`log.getMinutes()` / `status.condition()`).
- El system prompt dice "imports que ya están (aunque todavía no se usen)", porque en ORDENADOR borró imports del esqueleto (T04b).
- El marcado "lo completa otra subtarea" lo hace `pedir.py` solo, en el archivo y en el pedido.

| Iteración | Modelo | Entrada / salida | Segundos | Resultado | Causa probable | Cambio para la siguiente → ¿funcionó? |
|---|---|---|---|---|---|---|
