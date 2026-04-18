package com.fitmate.dao;

import com.fitmate.model.UserProfile;
import com.fitmate.service.FirebaseService;
import com.google.cloud.firestore.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Firestore implementation of UserDAO.
 * Reads/writes the users/{uid} document.
 */
public class UserDAOImpl implements UserDAO {

    private final Firestore db;

    public UserDAOImpl() {
        this.db = FirebaseService.getInstance().getFirestore();
    }

    @Override
    public CompletableFuture<UserProfile> getUserProfile(String uid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DocumentSnapshot doc = db.collection("users").document(uid).get().get();
                if (!doc.exists()) return null;

                UserProfile profile = new UserProfile();
                profile.setUid(uid);
                profile.setFirstName(doc.getString("firstName"));
                profile.setLastName(doc.getString("lastName"));
                profile.setEmail(doc.getString("email"));
                profile.setAge(safeString(doc, "age"));
                profile.setGender(doc.getString("gender"));
                profile.setHeight(safeString(doc, "height"));
                profile.setWeight(safeString(doc, "weight"));
                profile.setTargetWeight(safeString(doc, "targetWeight"));
                profile.setGoal(doc.getString("goal"));
                profile.setActivityLevel(safeString(doc, "activityLevel"));
                profile.setCountry(doc.getString("country"));
                profile.setProfilePic(doc.getString("profilePic"));
                return profile;
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch user profile", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> saveUserProfile(String uid, Map<String, Object> data) {
        return CompletableFuture.runAsync(() -> {
            try {
                db.collection("users").document(uid)
                        .set(data, SetOptions.merge())
                        .get();
            } catch (Exception e) {
                throw new RuntimeException("Failed to save user profile", e);
            }
        });
    }

    /** Safely get a field as String regardless of whether Firestore stores it as a number. */
    private String safeString(DocumentSnapshot doc, String field) {
        Object val = doc.get(field);
        return val != null ? String.valueOf(val) : null;
    }
}
