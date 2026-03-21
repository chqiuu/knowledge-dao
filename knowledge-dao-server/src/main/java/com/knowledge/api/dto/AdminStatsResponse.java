package com.knowledge.api.dto;

import java.util.Map;

/** Response DTO for GET /api/admin/stats */
public class AdminStatsResponse {
    private long totalEntries;
    private long totalUsers;
    private long totalSessions;
    private long totalSearches;
    private long storageBytes;
    private Map<String, Long> entriesByType;
    private Map<String, Long> topTags;
    private long sharedEntries;

    public long getTotalEntries() { return totalEntries; }
    public void setTotalEntries(long totalEntries) { this.totalEntries = totalEntries; }
    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
    public long getTotalSessions() { return totalSessions; }
    public void setTotalSessions(long totalSessions) { this.totalSessions = totalSessions; }
    public long getTotalSearches() { return totalSearches; }
    public void setTotalSearches(long totalSearches) { this.totalSearches = totalSearches; }
    public long getStorageBytes() { return storageBytes; }
    public void setStorageBytes(long storageBytes) { this.storageBytes = storageBytes; }
    public Map<String, Long> getEntriesByType() { return entriesByType; }
    public void setEntriesByType(Map<String, Long> entriesByType) { this.entriesByType = entriesByType; }
    public Map<String, Long> getTopTags() { return topTags; }
    public void setTopTags(Map<String, Long> topTags) { this.topTags = topTags; }
    public long getSharedEntries() { return sharedEntries; }
    public void setSharedEntries(long sharedEntries) { this.sharedEntries = sharedEntries; }
}
