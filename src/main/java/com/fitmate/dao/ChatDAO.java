package com.fitmate.dao;

import com.fitmate.model.ChatMessage;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Data Access Object interface for AI chat operations.
 */
public interface ChatDAO {

    /** Save a chat message (with question and reply). */
    CompletableFuture<Void> saveChat(String uid, ChatMessage message);

    /** Get all chat messages sorted by createdAt ascending. */
    CompletableFuture<List<ChatMessage>> getChatHistory(String uid);
}
