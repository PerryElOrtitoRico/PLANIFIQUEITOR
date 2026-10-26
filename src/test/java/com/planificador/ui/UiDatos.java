package com.planificador.ui;

import com.planificador.service.DataManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Datos inventados para los tests de la interfaz. "Hoy" en los tests es 2026-10-25 (domingo). */
final class UiDatos {
    private UiDatos() { }

    static final String JSON = """
            {
              "subjects": [
                { "id": "s1", "name": "Mecánica de Fluidos", "color": "#4f46e5", "professor": "Ing. X", "schedule": "Lun",
                  "units": [ { "id": "u1", "number": 1, "title": "Dinámica", "topics": [
                    { "id": "t1", "title": "Bernoulli", "completed": false },
                    { "id": "t2", "title": "Viscosidad", "completed": false } ] } ] },
                { "id": "s2", "name": "Física II", "color": "#0284c7", "professor": "Dra. Y", "schedule": "Mar", "units": [] }
              ],
              "studySessions": [
                { "id": "ss1", "dayOfWeek": "lunes", "subjectId": "s1", "topicDescription": "a", "durationMinutes": 60, "completed": false },
                { "id": "ss2", "dayOfWeek": "jueves", "subjectId": "s1", "topicDescription": "b", "durationMinutes": 60, "completed": false },
                { "id": "ss3", "dayOfWeek": "martes", "subjectId": "s2", "topicDescription": "c", "durationMinutes": 90, "completed": false }
              ],
              "importantDates": [
                { "id": "d1", "title": "Primer parcial", "subjectId": "s1", "date": "2026-10-31", "type": "parcial", "notes": "" },
                { "id": "d2", "title": "Entrega TP", "subjectId": "s2", "date": "2026-10-26", "type": "tp", "notes": "" },
                { "id": "d3", "title": "Final", "subjectId": "s1", "date": "2026-12-20", "type": "final", "notes": "" },
                { "id": "d4", "title": "Viejo", "subjectId": "s2", "date": "2026-10-01", "type": "parcial", "notes": "" },
                { "id": "d5", "title": "Charla", "subjectId": "general", "date": "2026-10-25", "type": "entrega", "notes": "" },
                { "id": "d6", "title": "Coloquio", "subjectId": "s2", "date": "2026-11-04", "type": "parcial", "notes": "" }
              ],
              "studyLogs": [
                { "id": "l1", "subjectId": "s1", "date": "2026-10-20", "startTime": "10:00", "minutes": 25 },
                { "id": "l2", "subjectId": "s1", "date": "2026-10-21", "startTime": "10:00", "minutes": 25 },
                { "id": "l3", "subjectId": "s2", "date": "2026-10-13", "startTime": "10:00", "minutes": 50 }
              ]
            }
            """;

    /** Escribe JSON en una carpeta temporal y hace que DataManager.getInstance() use ese archivo. */
    static DataManager cargar(Path tmp) throws IOException {
        Path archivo = tmp.resolve("datos/planificador_datos.json");
        Files.createDirectories(archivo.getParent());
        Files.writeString(archivo, JSON, StandardCharsets.UTF_8);
        DataManager.initForTests(archivo);
        return DataManager.getInstance();
    }
}
