package com.planificador.model;

/** Una nota cargada: de un parcial (o su recuperatorio) o de un TP. */
public class Evaluation {
    public static final String PARCIAL = "parcial";
    public static final String TP = "tp";

    private String id;
    private String kind;     // PARCIAL o TP
    private int number;      // 1, 2, …
    private boolean retake;  // true = nota del recuperatorio de ese parcial
    private double grade;
    private String date;     // YYYY-MM-DD, puede ser null

    public Evaluation(String id, String kind, int number, boolean retake, double grade, String date) {
        this.id = id;
        this.kind = kind;
        this.number = number;
        this.retake = retake;
        this.grade = grade;
        this.date = date;
    }

    public String getId() { return id; }
    public String getKind() { return kind; }
    public int getNumber() { return number; }
    public boolean isRetake() { return retake; }
    public double getGrade() { return grade; }
    public String getDate() { return date; }
}
