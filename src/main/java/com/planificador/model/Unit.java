package com.planificador.model;

import java.util.ArrayList;
import java.util.List;

public class Unit {
    private String id;
    private int number;
    private String title;
    private List<Topic> topics;

    public Unit(String id, int number, String title) {
        this.id = id;
        this.number = number;
        this.title = title;
        this.topics = new ArrayList<>();
    }

    public String getId() { return id; }
    public int getNumber() { return number; }
    public String getTitle() { return title; }
    public List<Topic> getTopics() { return topics; }

    public void setTitle(String title) { this.title = title; }
    public void setNumber(int number) { this.number = number; }

    public void addTopic(Topic topic) {
        this.topics.add(topic);
    }
}