package com.planificador.service;

/** Resultado de un parcial. original, retake y effective pueden ser null (sin nota). */
public record ExamResult(int number, Double original, Double retake, Double effective, ExamState state) { }
