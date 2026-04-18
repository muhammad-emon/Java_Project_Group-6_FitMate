package com.fitmate.model;

/**
 * Computed health metrics (not stored in Firestore, calculated on the fly).
 */
public class HealthMetrics {

    private double bmi;
    private String bmiCategory;
    private double bmr;
    private double tdee;
    private Double bfp; // nullable — only for Male/Female

    public HealthMetrics() { }

    public double getBmi() { return bmi; }
    public void setBmi(double bmi) { this.bmi = bmi; }

    public String getBmiCategory() { return bmiCategory; }
    public void setBmiCategory(String bmiCategory) { this.bmiCategory = bmiCategory; }

    public double getBmr() { return bmr; }
    public void setBmr(double bmr) { this.bmr = bmr; }

    public double getTdee() { return tdee; }
    public void setTdee(double tdee) { this.tdee = tdee; }

    public Double getBfp() { return bfp; }
    public void setBfp(Double bfp) { this.bfp = bfp; }
}
