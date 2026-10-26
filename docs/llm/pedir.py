#!/usr/bin/env python3
"""Arma el pedido de una subtarea, lo manda a LM Studio, guarda pedido y respuesta en registros/ y extrae los archivos.

Uso: python3 docs/llm/pedir.py <tarea.md> <qwen|gemma> <intento> [--error archivo] [--razonar low|medium] [--solo-ver]

La tarea empieza con un comentario: <!-- contrato: 1,2,5 | archivos: ruta1, ruta2 -->
Los `// TODO T0x` de otras subtareas se marcan como "lo completa otra subtarea" antes de mandar los archivos.
"""
import argparse
import json
import pathlib
import re
import sys
import time
import urllib.request

REPO = pathlib.Path(__file__).resolve().parents[2]
LLM = REPO / "docs" / "llm"
MODELOS = {"qwen": "qwen/qwen3.5-9b", "gemma": "google/gemma-4-e4b-qat"}
CONTEXTO = 8192

SYSTEM = """Sos un programador Java 17 cuidadoso. Te paso archivos con métodos marcados `// TODO`: completá solo los cuerpos que nombra la tarea; los métodos marcados "lo completa otra subtarea" quedan exactamente como están.
Reglas:
- No cambies package, imports que ya están (aunque todavía no se usen), nombres de clases, firmas ni métodos que ya están hechos. Podés agregar imports y métodos private.
- Las clases de model se leen con getters: log.getMinutes(), rules.getPassGrade(). Los records de service se leen con x(): status.condition(), a.percent(). No existen getCondition() ni getPercent().
- En los tests, copiá las aserciones de la tarea tal cual (mismos valores y fechas) y no escribas comentarios.
- No crees ni modifiques otros archivos.
- Devolvé cada archivo completo (nada de '...') en su propio bloque de código, con la ruta en la primera línea: un bloque por archivo, nunca dos archivos en el mismo bloque. Ejemplo:
```java
// src/main/java/com/planificador/service/Ejemplo.java
package com.planificador.service;

public final class Ejemplo { }
```
- Sin explicaciones fuera de los bloques."""


def leer_cabecera(texto):
    m = re.match(r"<!--\s*contrato:\s*([\d,\s]*)\|\s*archivos:\s*(.*?)\s*-->\s*\n", texto)
    if not m:
        sys.exit("la tarea no tiene la cabecera <!-- contrato: … | archivos: … -->")
    secciones = [s.strip() for s in m.group(1).split(",") if s.strip()]
    archivos = [a.strip() for a in m.group(2).split(",") if a.strip()]
    return secciones, archivos, texto[m.end():]


def extracto_contrato(secciones):
    texto = (LLM / "00-especificacion.md").read_text(encoding="utf-8")
    partes = re.split(r"(?m)^(?=## )", texto)
    titulo = partes[0].splitlines()[0]
    elegidas = [p.strip() for p in partes[1:] if re.match(r"## (\d+)\.", p) and re.match(r"## (\d+)\.", p).group(1) in secciones]
    return titulo + "\n\n" + "\n\n".join(elegidas)


def marcar_todos(codigo, tarea_id):
    """Los TODO de otras subtareas pasan a 'lo completa otra subtarea'; los de esta quedan como TODO."""
    codigo = re.sub(r"// lo completa otra subtarea \((T\w+)\): no lo cambies", r"// TODO \1", codigo)

    def cambiar(m):
        tid = m.group(1)
        return m.group(0) if tid == tarea_id else f"// lo completa otra subtarea ({tid}): no lo cambies"
    return re.sub(r"// TODO (T\w+)", cambiar, codigo)


def extraer_bloques(respuesta):
    """Bloques de código con la ruta en la primera línea. Si un bloque trae varios archivos pegados
    (otra línea `// ruta.java` en el medio), se separan por esas líneas."""
    bloques = {}
    for m in re.finditer(r"```[a-zA-Z]*\n(.*?)```", respuesta, re.S):
        actual, lineas = None, []
        for linea in m.group(1).split("\n"):
            r = re.match(r"\s*//\s*(src/\S+\.java)\s*$", linea)
            if r:
                if actual:
                    bloques[actual] = "\n".join(lineas)
                actual, lineas = r.group(1), []
            elif actual:
                lineas.append(linea)
        if actual:
            bloques[actual] = "\n".join(lineas)
    return bloques


def extraer_de_registro(etiqueta, archivos):
    contenido = (LLM / "registros" / f"{etiqueta}.respuesta.md").read_text(encoding="utf-8")
    escribir(extraer_bloques(contenido), archivos)


def escribir(bloques, archivos):
    for ruta, codigo in bloques.items():
        if ruta in archivos:
            (REPO / ruta).write_text(codigo.strip("\n") + "\n", encoding="utf-8")
            print("  escrito", ruta)
        else:
            print("  IGNORADO (no es de la tarea)", ruta)
    for ruta in archivos:
        if ruta not in bloques:
            print("  FALTA en la respuesta", ruta)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("tarea")
    ap.add_argument("modelo", choices=MODELOS)
    ap.add_argument("intento", type=int)
    ap.add_argument("--error")
    ap.add_argument("--razonar")
    ap.add_argument("--solo-ver", action="store_true")
    ap.add_argument("--max", type=int, default=0)
    ap.add_argument("--reextraer", action="store_true", help="no pide nada: vuelve a extraer la respuesta guardada")
    a = ap.parse_args()

    tarea_path = pathlib.Path(a.tarea)
    tarea_id = tarea_path.name.split("-")[0]
    secciones, archivos, tarea = leer_cabecera(tarea_path.read_text(encoding="utf-8"))

    if a.reextraer:
        extraer_de_registro(f"{tarea_id}-{a.modelo}-intento-{a.intento}", archivos)
        return

    bloques_arch = []
    for ruta in archivos:
        p = REPO / ruta
        codigo = marcar_todos(p.read_text(encoding="utf-8"), tarea_id)
        p.write_text(codigo, encoding="utf-8")
        bloques_arch.append(f"```java\n// {ruta}\n{codigo.rstrip()}\n```")

    user = ("# Contrato del proyecto (extracto)\n\n" + extracto_contrato(secciones) + "\n\n---\n\n" + tarea.strip()
            + "\n\n---\n\n# Archivos actuales\n\n" + "\n\n".join(bloques_arch))
    if a.error:
        user += ("\n\n---\n\n# Error al compilar o testear (corregilo)\n\n```\n"
                 + pathlib.Path(a.error).read_text(encoding="utf-8").strip() + "\n```")
    user += (f"\n\nCompletá los `// TODO` que pide la tarea {tarea_id}. Devolvé estos archivos completos, "
             f"cada uno en su bloque: {', '.join(archivos)}.")

    estimado = (len(SYSTEM) + len(user)) // 3
    max_tokens = a.max or max(1000, CONTEXTO - estimado - 64)
    etiqueta = f"{tarea_id}-{a.modelo}-intento-{a.intento}"
    cuerpo = {"model": MODELOS[a.modelo], "temperature": 0.2, "max_tokens": max_tokens,
              "messages": [{"role": "system", "content": SYSTEM}, {"role": "user", "content": user}]}
    if a.razonar:
        cuerpo["reasoning_effort"] = a.razonar
    reg = LLM / "registros"
    reg.mkdir(exist_ok=True)
    cab = (f"<!-- modelo {MODELOS[a.modelo]}, temperatura 0.2, max_tokens {max_tokens}, "
           f"razonamiento {a.razonar or 'no'} -->\n\n")
    (reg / f"{etiqueta}.pedido.md").write_text(cab + "## system\n\n" + SYSTEM + "\n\n## user\n\n" + user + "\n",
                                                encoding="utf-8")
    if a.solo_ver:
        print(f"pedido guardado (sin mandar): estimado {estimado} tokens")
        return

    t0 = time.time()
    req = urllib.request.Request("http://localhost:1234/v1/chat/completions", data=json.dumps(cuerpo).encode(),
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=1800) as r:
        resp = json.load(r)
    seg = time.time() - t0
    ch = resp["choices"][0]
    contenido = ch["message"].get("content") or ""
    razon = ch["message"].get("reasoning_content") or ch["message"].get("reasoning") or ""
    uso = resp.get("usage", {})
    pie = (f"\n\n<!-- {seg:.0f} s · entrada {uso.get('prompt_tokens')} · salida {uso.get('completion_tokens')} · "
           f"finish_reason {ch.get('finish_reason')} -->\n")
    (reg / f"{etiqueta}.respuesta.md").write_text(contenido + pie, encoding="utf-8")
    if razon:
        (reg / f"{etiqueta}.razonamiento.md").write_text(razon, encoding="utf-8")

    print(f"{etiqueta}: {seg:.0f} s, entrada {uso.get('prompt_tokens')}, salida {uso.get('completion_tokens')}, "
          f"finish_reason {ch.get('finish_reason')}, razonamiento {len(razon)} caracteres")
    if ch.get("finish_reason") == "length":
        print("respuesta cortada: no se extrae nada")
        return
    escribir(extraer_bloques(contenido), archivos)

if __name__ == "__main__":
    main()
