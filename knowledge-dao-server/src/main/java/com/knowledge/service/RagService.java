package com.knowledge.service;

import com.knowledge.controller.dto.ChatReq;
import com.knowledge.controller.dto.ChatResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG (Retrieval-Augmented Generation) Service.
 * Provides semantic search and context building.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final KnowledgeService knowledgeService;
    private final EmbeddingService embeddingService;

    /**
     * Retrieve relevant knowledge entries for a query.
     */
    public List<KnowledgeService.SearchHit> retrieve(String query, Long userId, int topK) {
        return knowledgeService.search(query, userId, topK);
    }

    /**
     * Build context string from retrieved entries for LLM prompting.
     */
    public String buildContext(String query, Long userId, int topK) {
        List<KnowledgeService.SearchHit> entries = retrieve(query, userId, topK);
        if (entries.isEmpty()) {
            return "No relevant knowledge found.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("## Relevant Knowledge (Top ").append(entries.size()).append(" results):\n\n");
        for (int i = 0; i < entries.size(); i++) {
            KnowledgeService.SearchHit entry = entries.get(i);
            sb.append("### [").append(i + 1).append("] ").append(entry.getTitle()).append("\n");
            sb.append(entry.getContent()).append("\n");
            if (entry.getTags() != null && !entry.getTags().isEmpty()) {
                sb.append("Tags: ").append(String.join(", ", entry.getTags())).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Full RAG chat response.
     */
    public ChatResp chat(ChatReq req) {
        List<KnowledgeService.SearchHit> hits = retrieve(req.getQuery(), req.getUserId(), req.getTopK() > 0 ? req.getTopK() : 5);
        String context = buildContext(req.getQuery(), req.getUserId(), req.getTopK() > 0 ? req.getTopK() : 5);

        ChatResp resp = new ChatResp();
        resp.setHitCount(hits.size());
        resp.setContext(context);
        resp.setHits(hits.stream().map(h -> {
            ChatResp.KnowledgeHit hit = new ChatResp.KnowledgeHit();
            hit.setId(h.getId());
            hit.setTitle(h.getTitle());
            hit.setContent(h.getContent());
            hit.setTags(h.getTags());
            hit.setSource(h.getSource());
            hit.setScore(1.0); // cosine distance is already used in sort
            return hit;
        }).collect(Collectors.toList()));
        return resp;
    }
}
