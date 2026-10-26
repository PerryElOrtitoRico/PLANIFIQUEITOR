package com.planificador.service;

import com.planificador.model.AttendanceRecord;
import com.planificador.model.CourseRules;
import com.planificador.model.Evaluation;
import com.planificador.model.PomodoroSettings;
import com.planificador.model.StudyLog;
import com.planificador.model.Subject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class T05DatosOcultoTest {

    /** Formato del archivo real de antes de esta etapa, con datos inventados. */
    static final String VIEJO = """
            {
              "subjects": [
                { "id": "s1", "name": "Mecánica de Fluidos", "color": "#4f46e5", "professor": "Ing. X",
                  "schedule": "Lun 18:00", "units": [ { "id": "u1", "number": 1, "title": "Estática",
                  "topics": [ { "id": "t1", "title": "Presión", "completed": true } ] } ] }
              ],
              "studySessions": [
                { "id": "ss1", "dayOfWeek": "lunes", "subjectId": "s1", "topicDescription": "Bernoulli",
                  "durationMinutes": 60, "completed": false }
              ],
              "importantDates": [
                { "id": "d1", "title": "Primer parcial", "subjectId": "s1", "date": "2026-11-02",
                  "type": "parcial", "notes": "" }
              ]
            }
            """;

    @TempDir
    Path tmp;

    Path escribir(String nombre, String json) throws IOException {
        Path a = tmp.resolve(nombre + "/planificador_datos.json");
        Files.createDirectories(a.getParent());
        Files.writeString(a, json, StandardCharsets.UTF_8);
        return a;
    }

    @Test
    void jsonViejoCargaConValoresPorDefecto() throws IOException {
        Path a = escribir("viejo", VIEJO);
        DataManager dm = new DataManager(a, List.of());
        assertEquals("Mecánica de Fluidos", dm.getSubjects().get(0).getName());
        assertEquals(1, dm.getStudySessions().size());
        assertNotNull(dm.getStudyLogs());
        assertTrue(dm.getStudyLogs().isEmpty());
        PomodoroSettings p = dm.getPomodoroSettings();
        assertEquals(List.of(25, 5, 15, 4), List.of(p.getWorkMinutes(), p.getShortBreakMinutes(),
                p.getLongBreakMinutes(), p.getCyclesBeforeLongBreak()));
        assertNull(dm.getSubjects().get(0).getCourse());
        assertEquals(VIEJO, Files.readString(a, StandardCharsets.UTF_8), "cargar no reescribe el archivo");
    }

    @Test
    void ajustesYCursadaIncompletosTomanLosValoresPorDefecto() throws IOException {
        String json = VIEJO.replace("\"importantDates\"", "\"pomodoroSettings\": { \"workMinutes\": 50 },\n  \"importantDates\"")
                .replace("\"units\": [", "\"course\": { \"rules\": { \"passGrade\": 4 } }, \"units\": [");
        DataManager dm = new DataManager(escribir("parcial", json), List.of());
        assertEquals(50, dm.getPomodoroSettings().getWorkMinutes());
        assertEquals(5, dm.getPomodoroSettings().getShortBreakMinutes());
        CourseRules r = dm.getSubjects().get(0).getCourse().getRules();
        assertEquals(4, r.getPassGrade(), 1e-9);
        assertEquals(75, r.getMinAttendancePercent(), 1e-9);
        assertTrue(dm.getSubjects().get(0).getCourse().getEvaluations().isEmpty());
        assertTrue(dm.getSubjects().get(0).getCourse().getAttendance().isEmpty());
    }

    @Test
    void idaYVuelta() throws IOException {
        Path a = escribir("ida", VIEJO);
        DataManager dm = new DataManager(a, List.of());
        dm.addStudyLog(new StudyLog("l1", "s1", "t1", "Presión", "2026-10-25", "10:00", 25));
        dm.setPomodoroSettings(new PomodoroSettings(45, 10, 20, 3));
        Subject s = dm.getSubjects().get(0);
        s.getOrCreateCourse().setRules(new CourseRules(80, 6, 0, 3, List.of(true, false, true), 14));
        s.getCourse().getEvaluations().add(new Evaluation("e1", Evaluation.PARCIAL, 2, true, 6.5, "2026-10-01"));
        s.getCourse().getAttendance().add(new AttendanceRecord("2026-08-10", false));
        dm.saveData();

        DataManager otra = new DataManager(a, List.of());
        StudyLog l = otra.getStudyLogs().get(0);
        assertEquals("Presión", l.getTopicTitle());
        assertEquals(25, l.getMinutes());
        assertEquals(3, otra.getPomodoroSettings().getCyclesBeforeLongBreak());
        CourseRules r = otra.getSubjects().get(0).getCourse().getRules();
        assertEquals(3, r.getExamCount());
        assertFalse(r.hasRetake(2));
        assertTrue(r.hasRetake(3));
        assertEquals(14, r.getPlannedClasses());
        Evaluation e = otra.getSubjects().get(0).getCourse().getEvaluations().get(0);
        assertTrue(e.isRetake());
        assertEquals(6.5, e.getGrade(), 1e-9);
        assertFalse(otra.getSubjects().get(0).getCourse().getAttendance().get(0).isPresent());
        assertEquals("Bernoulli", otra.getStudySessions().get(0).getTopicDescription());
    }

    @Test
    void borrarMateriaBorraSusSesionesYEjemploVacia() throws IOException {
        DataManager dm = new DataManager(escribir("borrar", VIEJO), List.of());
        dm.addStudyLog(new StudyLog("l1", "s1", null, null, "2026-10-25", "10:00", 25));
        dm.addStudyLog(new StudyLog("l2", "otra", null, null, "2026-10-25", "11:00", 25));
        dm.removeSubject("s1");
        assertEquals(1, dm.getStudyLogs().size());
        assertEquals("otra", dm.getStudyLogs().get(0).getSubjectId());
        dm.setPomodoroSettings(new PomodoroSettings(50, 10, 20, 2));
        dm.loadInitialDemoData();
        assertTrue(dm.getStudyLogs().isEmpty());
        assertEquals(25, dm.getPomodoroSettings().getWorkMinutes());
    }
}
