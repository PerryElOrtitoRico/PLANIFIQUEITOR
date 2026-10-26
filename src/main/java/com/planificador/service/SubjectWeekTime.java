package com.planificador.service;

/** Minutos planeados (guía semanal) y estudiados (Pomodoro) de una materia en una semana. */
public record SubjectWeekTime(String subjectId, int plannedMinutes, int actualMinutes) { }
