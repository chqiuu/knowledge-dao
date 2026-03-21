package com.knowledge.model;

import java.time.LocalDateTime;

public class UserProfile {
    private String key;
    private Object value;
    private LocalDateTime updatedAt;
    private Long userId;

    public UserProfile() {}

    public UserProfile(String key, Object value, Long userId) {
        this.key = key;
        this.value = value;
        this.userId = userId;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    @Override
    public String toString() {
        return "UserProfile{key='" + key + "', value=" + value + "}";
    }
}
