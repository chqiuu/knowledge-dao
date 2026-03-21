package com.knowledge.api.dto;

/** Request DTO for POST /api/rag/search */
public class SearchRequest {
    private String query;
    private Long userId;
    private int topK = 5;

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }
}
