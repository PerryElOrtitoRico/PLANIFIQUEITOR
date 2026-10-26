package com.planificador.service;

import java.util.List;

/** Condición calculada de una materia, con el detalle para mostrar cuánto falta. */
public record CourseStatus(CourseCondition condition, AttendanceSummary attendance, List<ExamResult> exams,
                           boolean canPromote, boolean canRegularize) { }
