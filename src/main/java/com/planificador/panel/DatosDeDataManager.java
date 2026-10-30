package com.planificador.panel;

import com.planificador.model.ImportantDate;
import com.planificador.model.StudyLog;
import com.planificador.model.Subject;
import com.planificador.service.DataManager;

import java.util.ArrayList;
import java.util.List;

/** Los datos reales del singleton {@link DataManager}, siempre como copia para iterar sin sobresaltos. */
public final class DatosDeDataManager implements DatosPlanificador {

    @Override
    public List<Subject> materias() {
        return new ArrayList<>(DataManager.getInstance().getSubjects());
    }

    @Override
    public List<StudyLog> estudio() {
        return new ArrayList<>(DataManager.getInstance().getStudyLogs());
    }

    @Override
    public List<ImportantDate> fechas() {
        return new ArrayList<>(DataManager.getInstance().getImportantDates());
    }

    @Override
    public boolean confiable() {
        return DataManager.getInstance().getLoadWarning() == null;
    }
}
