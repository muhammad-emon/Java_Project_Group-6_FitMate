package com.fitmate.dao;

import com.fitmate.model.WeightLogEntry;
import com.fitmate.service.FirebaseService;
import com.google.cloud.firestore.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Firestore implementation of WeightLogDAO.
 * Reads/writes users/{uid}/weightLog subcollection.
 */
public class WeightLogDAOImpl implements WeightLogDAO {

    private final Firestore db;

    public WeightLogDAOImpl() {
        this.db = FirebaseService.getInstance().getFirestore();
    }

    @Override
    public CompletableFuture<Void> addWeightLog(String uid, WeightLogEntry entry) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("weight", entry.getWeight());
                data.put("date", com.google.cloud.Timestamp.now());

                db.collection("users").document(uid)
                        .collection("weightLog")
                        .add(data).get();
            } catch (Exception e) {
                throw new RuntimeException("Failed to add weight log", e);
            }
        });
    }

    @Override
    public CompletableFuture<List<WeightLogEntry>> getWeightLog(String uid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                QuerySnapshot snapshot = db.collection("users").document(uid)
                        .collection("weightLog")
                        .orderBy("date", Query.Direction.ASCENDING)
                        .get().get();

                List<WeightLogEntry> entries = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
                    WeightLogEntry entry = new WeightLogEntry();
                    entry.setId(doc.getId());

                    Object weightObj = doc.get("weight");
                    if (weightObj instanceof Number) {
                        entry.setWeight(((Number) weightObj).doubleValue());
                    }

                    com.google.cloud.Timestamp ts = doc.getTimestamp("date");
                    if (ts != null) {
                        entry.setDate(ts.toDate());
                    }

                    entries.add(entry);
                }
                return entries;
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch weight log", e);
            }
        });
    }
}
