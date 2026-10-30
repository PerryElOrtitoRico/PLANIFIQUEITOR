package com.planificador.panel;

import com.planificador.model.ImportantDate;
import com.planificador.model.StudyLog;
import com.planificador.model.Subject;

import java.util.List;

/** Lo mínimo que el sincronizador necesita leer de los datos del Planificador. */
public interface DatosPlanificador {
    List<Subject> materias();

    List<StudyLog> estudio();

    List<ImportantDate> fechas();
}
