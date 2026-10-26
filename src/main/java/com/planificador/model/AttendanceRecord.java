package com.planificador.model;

/** Asistencia a una clase. */
public class AttendanceRecord {
    private String date; // YYYY-MM-DD
    private boolean present;

    public AttendanceRecord(String date, boolean present) {
        this.date = date;
        this.present = present;
    }

    public String getDate() { return date; }
    public boolean isPresent() { return present; }
}
