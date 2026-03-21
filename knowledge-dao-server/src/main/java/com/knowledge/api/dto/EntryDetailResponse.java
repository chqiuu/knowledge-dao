package com.knowledge.api.dto;

import com.knowledge.model.KnowledgeEntry;
import java.util.List;

/** Response DTO for GET /api/admin/entries/{id} — includes embedding info */
public class EntryDetailResponse {
    private Long id;
    private String title;
    private String content;
    private String contentType;
    private List<String> tags;
    private float[] embedding;
    private String source;
    private String createdAt;
    private String updatedAt;
    private Long userId;
    private Boolean isShared;
    private int embeddingDimension;

    public static EntryDetailResponse from(KnowledgeEntry e) {
        EntryDetailResponse r = new EntryDetailResponse();
        r.setId(e.getId());
        r.setTitle(e.getTitle());
        r.setContent(e.getContent());
        r.setContentType(e.getContentType());
        r.setTags(e.getTags());
        r.setSource(e.getSource());
        r.setCreatedAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        r.setUpdatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null);
        r.setUserId(e.getUserId());
        r.setIsShared(e.getIsShared());
        r.setEmbedding(e.getEmbedding());
        if (e.getEmbedding() != null) {
            r.setEmbeddingDimension(e.getEmbedding().length);
        }
        return r;
    }

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
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Boolean getIsShared() { return isShared; }
    public void setIsShared(Boolean isShared) { this.isShared = isShared; }
    public int getEmbeddingDimension() { return embeddingDimension; }
    public void setEmbeddingDimension(int embeddingDimension) { this.embeddingDimension = embeddingDimension; }
}
