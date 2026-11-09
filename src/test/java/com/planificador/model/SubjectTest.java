package com.planificador.model;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubjectTest {

    @Test
    void unaMateriaLeidaSinUnidadesNoRompeElProgreso() {
        Subject s = new Gson().fromJson("{\"id\":\"s1\",\"name\":\"Física\"}", Subject.class);
        assertEquals(0, s.getProgressPercentage(), 1e-9);
        Subject t = new Gson().fromJson("{\"id\":\"s2\",\"name\":\"Q\",\"units\":[{\"id\":\"u1\",\"title\":\"U\"}]}", Subject.class);
        assertEquals(0, t.getProgressPercentage(), 1e-9);
    }
}
