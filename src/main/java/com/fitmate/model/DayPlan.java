package com.fitmate.model;

import java.util.List;

/**
 * A single day's plan within a weekly diet plan.
 */
public class DayPlan {

    private String dayName;
    private List<MealItem> breakfast;
    private List<MealItem> lunch;
    private List<MealItem> snacks;
    private List<MealItem> dinner;
    private DailyTotals totals;

    public DayPlan() { }

    public String getDayName() { return dayName; }
    public void setDayName(String dayName) { this.dayName = dayName; }

    public List<MealItem> getBreakfast() { return breakfast; }
    public void setBreakfast(List<MealItem> breakfast) { this.breakfast = breakfast; }

    public List<MealItem> getLunch() { return lunch; }
    public void setLunch(List<MealItem> lunch) { this.lunch = lunch; }

    public List<MealItem> getSnacks() { return snacks; }
    public void setSnacks(List<MealItem> snacks) { this.snacks = snacks; }

    public List<MealItem> getDinner() { return dinner; }
    public void setDinner(List<MealItem> dinner) { this.dinner = dinner; }

    public DailyTotals getTotals() { return totals; }
    public void setTotals(DailyTotals totals) { this.totals = totals; }
}
