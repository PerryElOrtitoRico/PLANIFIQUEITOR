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

    /**
     * false si los datos no son de fiar (p. ej. el archivo no se pudo leer y se arrancó vacío): en ese caso
     * el sincronizador no borra nada del panel.
     */
    boolean confiable();
}
