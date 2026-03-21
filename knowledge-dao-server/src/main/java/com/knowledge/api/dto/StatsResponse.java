package com.knowledge.api.dto;

/** Response DTO for GET /api/rag/stats */
public class StatsResponse {
    private long totalEntries;
    private String contentTypes;
    private String topTags;

    public long getTotalEntries() { return totalEntries; }
    public void setTotalEntries(long totalEntries) { this.totalEntries = totalEntries; }
    public String getContentTypes() { return contentTypes; }
    public void setContentTypes(String contentTypes) { this.contentTypes = contentTypes; }
    public String getTopTags() { return topTags; }
    public void setTopTags(String topTags) { this.topTags = topTags; }
}
