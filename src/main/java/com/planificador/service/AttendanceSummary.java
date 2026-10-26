package com.planificador.service;

/**
 * Resumen de asistencia.
 * @param remainingKnown    true si se conoce la cantidad de clases del cuatrimestre
 * @param remainingAbsences faltas que todavía se pueden tener (negativo = se pasó); 0 si remainingKnown es false
 */
public record AttendanceSummary(int registered, int present, int absent, double percent,
                                boolean failed, boolean remainingKnown, int remainingAbsences) { }
