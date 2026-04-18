package com.fitmate.dao;

import com.fitmate.service.FirebaseService;
import com.google.cloud.firestore.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Firestore implementation of DietPlanDAO.
 * Reads/writes users/{uid}/dietPlans/latest.
 */
public class DietPlanDAOImpl implements DietPlanDAO {

    private final Firestore db;

    public DietPlanDAOImpl() {
        this.db = FirebaseService.getInstance().getFirestore();
    }

    @Override
    public CompletableFuture<Void> saveDietPlan(String uid, Map<String, Object> planData) {
        return CompletableFuture.runAsync(() -> {
            try {
                db.collection("users").document(uid)
                        .collection("dietPlans").document("latest")
                        .set(planData)
                        .get();
            } catch (Exception e) {
                throw new RuntimeException("Failed to save diet plan", e);
            }
        });
    }

    @Override
    public CompletableFuture<Map<String, Object>> getLatestDietPlan(String uid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DocumentSnapshot doc = db.collection("users").document(uid)
                        .collection("dietPlans").document("latest")
                        .get().get();
                if (!doc.exists()) return null;
                return doc.getData();
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch diet plan", e);
            }
        });
    }
}
