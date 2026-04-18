package com.fitmate.model;

/**
 * A single chat message stored in Firestore: users/{uid}/aiChats/{id}.
 * Each document has both the user message and the AI reply.
 */
public class ChatMessage {

    private String id;
    private String uid;
    private String message;   // user's question
    private String reply;     // AI's answer
    private String createdAt; // ISO string

    public ChatMessage() { }

    public ChatMessage(String uid, String message) {
        this.uid = uid;
        this.message = message;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
