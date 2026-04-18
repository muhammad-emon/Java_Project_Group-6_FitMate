package com.fitmate.dao;

import com.fitmate.model.WeightLogEntry;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Data Access Object interface for weight log operations.
 */
public interface WeightLogDAO {

    /** Add a new weight log entry. */
    CompletableFuture<Void> addWeightLog(String uid, WeightLogEntry entry);

    /** Get all weight log entries sorted by date ascending. */
    CompletableFuture<List<WeightLogEntry>> getWeightLog(String uid);
}
