package com.knowledge.model;

import java.time.LocalDateTime;

public class ChatMessage {
    private Long id;
    private String sessionKey;
    private String role;
    private String content;
    private LocalDateTime createdAt;
    private Long userId;

    public ChatMessage() {}

    public ChatMessage(String sessionKey, String role, String content, Long userId) {
        this.sessionKey = sessionKey;
        this.role = role;
        this.content = content;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSessionKey() { return sessionKey; }
    public void setSessionKey(String sessionKey) { this.sessionKey = sessionKey; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    @Override
    public String toString() {
        return "ChatMessage{id=" + id + ", sessionKey='" + sessionKey + "', role='" + role + "'}";
    }
}
