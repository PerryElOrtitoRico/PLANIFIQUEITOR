package com.planificador.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ImportantDate {
    private String id;
    private String title;
    private String subjectId;
    private String date; // ISO YYYY-MM-DD
    private String type; // parcial, final, tp, entrega
    private String notes;

    public ImportantDate(String id, String title, String subjectId, String date, String type, String notes) {
        this.id = id;
        this.title = title;
        this.subjectId = subjectId;
        this.date = date;
        this.type = type;
        this.notes = notes;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getSubjectId() { return subjectId; }
    public String getDate() { return date; }
    public String getType() { return type; }
    public String getNotes() { return notes; }

    public long getDaysRemaining() {
        try {
            LocalDate target = LocalDate.parse(date);
            LocalDate now = LocalDate.now();
            return ChronoUnit.DAYS.between(now, target);
        } catch (Exception e) {
            return 0;
        }
    }
}