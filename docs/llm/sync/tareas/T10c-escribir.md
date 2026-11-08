<!-- contrato: 1,2,4 | archivos: src/main/java/com/planificador/service/CarpetaDatos.java -->
# T10c — CarpetaDatos: escribir sin pisar

## Objetivo
Completar el `// TODO T10c` de `CarpetaDatos`: el método `escribir(Datos datos)`. No cambies nada más.

## Pasos de `escribir(Datos datos)` (en orden)
1. `Map<Path, String> deseado = deseado(datos);`
2. `int escritos = 0; int borrados = 0; List<String> noPisados = new ArrayList<>();`
3. Lo que hay que crear o cambiar:
```java
for (Map.Entry<Path, String> e : deseado.entrySet()) {
    Path rel = e.getKey();
    String antes = conocido().get(rel);
    if (iguales(antes, e.getValue())) continue;
    if (!iguales(enDisco(rel), antes)) {
        noPisados.add(texto(rel));
        continue;
    }
    escribirAtomico(rel, e.getValue());
    conocido().put(rel, e.getValue());
    escritos++;
}
```
4. Lo que hay que borrar (se recorre una copia de las claves):
```java
for (Path rel : new ArrayList<>(conocido().keySet())) {
    if (deseado.containsKey(rel)) continue;
    if (iguales(enDisco(rel), conocido().get(rel))) {
        borrar(rel);
        conocido().remove(rel);
        borrados++;
    } else {
        noPisados.add(texto(rel));
    }
}
```
5. `return new Escritura(escritos, borrados, noPisados);`
