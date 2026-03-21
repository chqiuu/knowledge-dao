package com.knowledge.api.dto;

import java.util.Map;

/** Response DTO for GET /api/admin/searches/stats */
public class SearchStatsResponse {
    private long totalSearches;
    private long searchesToday;
    private Map<String, Long> topQueries;
    private Map<String, Long> searchesByUser;
    private double avgHitCount;

    public long getTotalSearches() { return totalSearches; }
    public void setTotalSearches(long totalSearches) { this.totalSearches = totalSearches; }
    public long getSearchesToday() { return searchesToday; }
    public void setSearchesToday(long searchesToday) { this.searchesToday = searchesToday; }
    public Map<String, Long> getTopQueries() { return topQueries; }
    public void setTopQueries(Map<String, Long> topQueries) { this.topQueries = topQueries; }
    public Map<String, Long> getSearchesByUser() { return searchesByUser; }
    public void setSearchesByUser(Map<String, Long> searchesByUser) { this.searchesByUser = searchesByUser; }
    public double getAvgHitCount() { return avgHitCount; }
    public void setAvgHitCount(double avgHitCount) { this.avgHitCount = avgHitCount; }
}
