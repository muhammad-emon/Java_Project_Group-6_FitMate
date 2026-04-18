package com.fitmate.dao;

import com.fitmate.model.UserProfile;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Data Access Object interface for user profile operations.
 */
public interface UserDAO {

    /** Fetch user profile by UID. */
    CompletableFuture<UserProfile> getUserProfile(String uid);

    /** Save or update user profile (merge). */
    CompletableFuture<Void> saveUserProfile(String uid, Map<String, Object> data);
}
