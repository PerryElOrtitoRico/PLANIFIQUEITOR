```java
// src/main/java/com/planificador/service/CarpetaDatos.java
    public Escritura escribir(Datos datos) throws IOException {
        Map<Path, String> deseado = deseado(datos);
        int escritos = 0;
        int borrados = 0;
        List<String> noPisados = new ArrayList<>();

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

        return new Escritura(escritos, borrados, noPisados);
    }
```

<!-- 29 s · entrada 4170 · salida 294 · finish_reason stop -->
