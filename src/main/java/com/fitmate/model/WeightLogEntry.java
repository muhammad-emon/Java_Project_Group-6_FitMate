package com.fitmate.model;

import java.util.Date;

/**
 * A single weight-log entry stored in Firestore: users/{uid}/weightLog/{id}.
 */
public class WeightLogEntry {

    private String id;
    private double weight;
    private Date date;

    public WeightLogEntry() { }

    public WeightLogEntry(double weight, Date date) {
        this.weight = weight;
        this.date = date;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
}
