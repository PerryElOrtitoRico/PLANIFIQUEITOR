#!/bin/bash
# Uso: docs/llm/aceptar.sh T0x archivo...  → borra los "// TODO T0x" que el modelo dejó en métodos ya completos.
T=$1; shift
for f in "$@"; do sed -i "/^\s*\/\/ TODO $T\s*$/d" "$f"; done
