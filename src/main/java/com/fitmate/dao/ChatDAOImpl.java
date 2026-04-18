package com.fitmate.dao;

import com.fitmate.model.ChatMessage;
import com.fitmate.service.FirebaseService;
import com.google.cloud.firestore.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Firestore implementation of ChatDAO.
 * Reads/writes users/{uid}/aiChats subcollection.
 */
public class ChatDAOImpl implements ChatDAO {

    private final Firestore db;

    public ChatDAOImpl() {
        this.db = FirebaseService.getInstance().getFirestore();
    }

    @Override
    public CompletableFuture<Void> saveChat(String uid, ChatMessage msg) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("uid", uid);
                data.put("message", msg.getMessage());
                data.put("reply", msg.getReply());
                data.put("createdAt", msg.getCreatedAt());

                db.collection("users").document(uid)
                        .collection("aiChats")
                        .add(data).get();
            } catch (Exception e) {
                throw new RuntimeException("Failed to save chat message", e);
            }
        });
    }

    @Override
    public CompletableFuture<List<ChatMessage>> getChatHistory(String uid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                QuerySnapshot snapshot = db.collection("users").document(uid)
                        .collection("aiChats")
                        .orderBy("createdAt", Query.Direction.ASCENDING)
                        .get().get();

                List<ChatMessage> messages = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
                    ChatMessage msg = new ChatMessage();
                    msg.setId(doc.getId());
                    msg.setUid(doc.getString("uid"));
                    msg.setMessage(doc.getString("message"));
                    msg.setReply(doc.getString("reply"));
                    msg.setCreatedAt(doc.getString("createdAt"));
                    messages.add(msg);
                }
                return messages;
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch chat history", e);
            }
        });
    }
}
