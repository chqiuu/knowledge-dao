package com.knowledge.model;

import java.time.LocalDateTime;
import java.util.List;

public class KnowledgeEntry {
    private Long id;
    private String title;
    private String content;
    private String contentType;
    private List<String> tags;
    private float[] embedding;
    private String source;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long userId;
    private Boolean isShared;

    public KnowledgeEntry() {}

    public KnowledgeEntry(String title, String content, String contentType,
                          List<String> tags, String source, Long userId) {
        this.title = title;
        this.content = content;
        this.contentType = contentType;
        this.tags = tags;
        this.source = source;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isShared = false;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public float[] getEmbedding() { return embedding; }
    public void setEmbedding(float[] embedding) { this.embedding = embedding; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Boolean getIsShared() { return isShared; }
    public void setIsShared(Boolean isShared) { this.isShared = isShared; }

    @Override
    public String toString() {
        return "KnowledgeEntry{id=" + id + ", title='" + title + "', contentType='" + contentType + "'}";
    }
}
