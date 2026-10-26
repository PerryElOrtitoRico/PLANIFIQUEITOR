package com.planificador.service;

import com.planificador.model.StudyLog;
import com.planificador.model.StudySession;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StudyTimeStatsTest {

    static StudyLog log(String materia, String fecha, int minutos) {
        return new StudyLog("l", materia, null, null, fecha, "10:00", minutos);
    }

    static StudySession plan(String materia, int minutos) {
        return new StudySession("p", "lunes", materia, "algo", minutos, false);
    }

    @Test
    void lunesDeLaSemana() {
        // TODO T02a
    }

    @Test
    void minutosPorMateriaSoloDeEsaSemana() {
        // TODO T02a
    }

    @Test
    void planeadoSumaLaGuia() {
        // TODO T02a
    }

    @Test
    void comparaPlaneadoYEstudiado() {
        // TODO T02b
    }

    @Test
    void minutosPorSemana() {
        // TODO T02b
    }

    @Test
    void formatoDeMinutos() {
        // TODO T02b
    }
}
