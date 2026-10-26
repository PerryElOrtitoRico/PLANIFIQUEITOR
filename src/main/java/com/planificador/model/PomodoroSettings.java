package com.planificador.model;

/** Duraciones del Pomodoro, en minutos. Un campo que falta en el JSON queda con su valor por defecto. */
public class PomodoroSettings {
    private int workMinutes = 25;
    private int shortBreakMinutes = 5;
    private int longBreakMinutes = 15;
    private int cyclesBeforeLongBreak = 4;

    public PomodoroSettings() { }

    public PomodoroSettings(int workMinutes, int shortBreakMinutes, int longBreakMinutes, int cyclesBeforeLongBreak) {
        this.workMinutes = workMinutes;
        this.shortBreakMinutes = shortBreakMinutes;
        this.longBreakMinutes = longBreakMinutes;
        this.cyclesBeforeLongBreak = cyclesBeforeLongBreak;
    }

    public int getWorkMinutes() { return workMinutes; }
    public int getShortBreakMinutes() { return shortBreakMinutes; }
    public int getLongBreakMinutes() { return longBreakMinutes; }
    public int getCyclesBeforeLongBreak() { return cyclesBeforeLongBreak; }

    public void setWorkMinutes(int workMinutes) { this.workMinutes = workMinutes; }
    public void setShortBreakMinutes(int shortBreakMinutes) { this.shortBreakMinutes = shortBreakMinutes; }
    public void setLongBreakMinutes(int longBreakMinutes) { this.longBreakMinutes = longBreakMinutes; }
    public void setCyclesBeforeLongBreak(int cyclesBeforeLongBreak) { this.cyclesBeforeLongBreak = cyclesBeforeLongBreak; }
}
