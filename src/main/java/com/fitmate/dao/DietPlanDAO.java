package com.fitmate.dao;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Data Access Object interface for diet plan operations.
 */
public interface DietPlanDAO {

    /** Save the latest diet plan. */
    CompletableFuture<Void> saveDietPlan(String uid, Map<String, Object> planData);

    /** Get the latest diet plan as a raw map. */
    CompletableFuture<Map<String, Object>> getLatestDietPlan(String uid);
}
