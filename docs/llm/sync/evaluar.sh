#!/bin/bash
# Uso: docs/llm/sync/evaluar.sh T10a  → corre los tests propios y los ocultos de esa subtarea (T10a → métodos T10a_* de T10OcultoTest).
cd "$(dirname "$0")/../../.." || exit 1
T=$1; CLASE=${T:0:3}
echo "== tests propios"
mvn -B -q -o test > /tmp/evaluar-sync-$USER.log 2>&1; echo "mvn test: exit $?"
grep -E "Tests run:|COMPILATION ERROR|ERROR\] /|expected:|but was|Exception:" /tmp/evaluar-sync-$USER.log | head -20
echo "== ocultos $T"
for pk in service ui; do cp docs/llm/sync/evaluacion/java/com/planificador/$pk/${CLASE}OcultoTest.java src/test/java/com/planificador/$pk/ 2>/dev/null; done
mvn -B -o test -Dtest="**/${CLASE}OcultoTest#${T}*" -Dsurefire.failIfNoSpecifiedTests=false > /tmp/evaluar-oculto-sync-$USER.log 2>&1
grep -E "Tests run:|COMPILATION ERROR|ERROR\] /|expected|<<<" /tmp/evaluar-oculto-sync-$USER.log | grep -v "^\[INFO\] *$" | head -20
rm -f src/test/java/com/planificador/{service,ui}/${CLASE}OcultoTest.java
