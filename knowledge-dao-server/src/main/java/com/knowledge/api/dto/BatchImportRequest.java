package com.knowledge.api.dto;

import java.util.List;

/** Request DTO for POST /api/admin/entries/batch */
public class BatchImportRequest {
    private List<BatchEntry> entries;

    public static class BatchEntry {
        private String title;
        private String content;
        private String contentType = "article";
        private List<String> tags;
        private String source = "batch_import";
        private Long userId;

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
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }

    public List<BatchEntry> getEntries() { return entries; }
    public void setEntries(List<BatchEntry> entries) { this.entries = entries; }
}
