package com.knowledge.api.dto;

import java.util.List;

/** Response DTO for RAG chat results */
public class ChatResponse {
    private String answer;        // Placeholder - LLM integration point
    private String context;       // Built RAG context from retrieved entries
    private List<KnowledgeHit> hits;
    private int hitCount;

    public static class KnowledgeHit {
        private Long id;
        private String title;
        private String content;
        private List<String> tags;
        private double score;
        private String source;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
    }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }
    public List<KnowledgeHit> getHits() { return hits; }
    public void setHits(List<KnowledgeHit> hits) { this.hits = hits; }
    public int getHitCount() { return hitCount; }
    public void setHitCount(int hitCount) { this.hitCount = hitCount; }
}
