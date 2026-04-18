package com.fitmate.model;

/**
 * Daily totals for a diet plan day.
 */
public class DailyTotals {

    private int calories;
    private int protein;
    private int carbs;
    private int fat;
    private int fiber;

    public DailyTotals() { }

    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; }

    public int getProtein() { return protein; }
    public void setProtein(int protein) { this.protein = protein; }

    public int getCarbs() { return carbs; }
    public void setCarbs(int carbs) { this.carbs = carbs; }

    public int getFat() { return fat; }
    public void setFat(int fat) { this.fat = fat; }

    public int getFiber() { return fiber; }
    public void setFiber(int fiber) { this.fiber = fiber; }
}
