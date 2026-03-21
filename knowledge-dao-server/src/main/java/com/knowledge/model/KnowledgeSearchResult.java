package com.knowledge.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lightweight DTO for search results.
 * Excludes embedding to avoid unnecessary parsing overhead.
 */
public class KnowledgeSearchResult {
    private Long id;
    private String title;
    private String content;
    private String contentType;
    private List<String> tags;
    private String source;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long userId;
    private Boolean isShared;
    private double distance;

    public KnowledgeSearchResult() {}

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
    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    /**
     * Convert to full KnowledgeEntry (with embedding set to null).
     */
    public KnowledgeEntry toKnowledgeEntry() {
        KnowledgeEntry entry = new KnowledgeEntry();
        entry.setId(id);
        entry.setTitle(title);
        entry.setContent(content);
        entry.setContentType(contentType);
        entry.setTags(tags);
        entry.setSource(source);
        entry.setCreatedAt(createdAt);
        entry.setUpdatedAt(updatedAt);
        entry.setUserId(userId);
        entry.setIsShared(isShared);
        entry.setEmbedding(null);
        return entry;
    }
}
