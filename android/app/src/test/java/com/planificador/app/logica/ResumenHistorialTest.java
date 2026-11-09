package com.planificador.app.logica;

import com.planificador.model.StudyLog;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResumenHistorialTest {

    static StudyLog log(String id, String fecha, String hora, int min) {
        return new StudyLog(id, "m1", null, null, fecha, hora, min);
    }

    @Test
    void minutosPorDiaSumaElMismoDiaEIgnoraOtrosMeses() {
        Map<LocalDate, Integer> m = ResumenHistorial.minutosPorDia(List.of(
                log("a", "2026-11-08", "10:00", 25), log("b", "2026-11-08", "14:00", 50),
                log("c", "2026-10-31", "10:00", 30)), YearMonth.of(2026, 11));
        assertEquals(Map.of(LocalDate.of(2026, 11, 8), 75), m);
    }

    @Test
    void delDiaOrdenaPorHoraYDejaLasSinHoraAlFinal() {
        List<StudyLog> d = ResumenHistorial.delDia(List.of(
                log("sin", "2026-11-08", null, 60), log("tarde", "2026-11-08", "14:00", 25),
                log("temprano", "2026-11-08", "10:30", 25), log("otro", "2026-11-09", "09:00", 25)),
                LocalDate.of(2026, 11, 8));
        assertEquals(List.of("temprano", "tarde", "sin"), d.stream().map(StudyLog::getId).toList());
    }

    @Test
    void formatearComoEnLaPc() {
        assertEquals("1h05", ResumenHistorial.formatear(65));
        assertEquals("45m", ResumenHistorial.formatear(45));
        assertEquals("2h", ResumenHistorial.formatear(120));
    }
}
