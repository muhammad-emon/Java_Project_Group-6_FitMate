package com.fitmate.model;

/**
 * A single food item within a meal.
 */
public class MealItem {

    private String name;
    private int calories;
    private int protein;
    private int carbs;
    private int fat;
    private int fiber;
    private String vitamins;

    public MealItem() { }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

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

    public String getVitamins() { return vitamins; }
    public void setVitamins(String vitamins) { this.vitamins = vitamins; }
}
