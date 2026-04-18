package com.fitmate.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A full weekly diet plan stored in Firestore: users/{uid}/dietPlans/latest.
 */
public class DietPlan {

    private String weekStart;
    private String createdAt;
    private Map<String, Object> profile;
    private Map<String, Object> metrics;
    private Map<String, Object> preferences;

    /** day-name → DayPlan, kept in insertion order (Mon→Sun). */
    private LinkedHashMap<String, DayPlan> days = new LinkedHashMap<>();

    public DietPlan() { }

    public String getWeekStart() { return weekStart; }
    public void setWeekStart(String weekStart) { this.weekStart = weekStart; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public Map<String, Object> getProfile() { return profile; }
    public void setProfile(Map<String, Object> profile) { this.profile = profile; }

    public Map<String, Object> getMetrics() { return metrics; }
    public void setMetrics(Map<String, Object> metrics) { this.metrics = metrics; }

    public Map<String, Object> getPreferences() { return preferences; }
    public void setPreferences(Map<String, Object> preferences) { this.preferences = preferences; }

    public LinkedHashMap<String, DayPlan> getDays() { return days; }
    public void setDays(LinkedHashMap<String, DayPlan> days) { this.days = days; }
}
