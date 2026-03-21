package com.knowledge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * VectorStore backed by EmbeddingService + pgvector.
 */
@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private final EmbeddingService embeddingService;

    public float[] embed(String text) throws Exception {
        return embeddingService.embed(text);
    }

    public String toVectorString(float[] embedding) {
        return embeddingService.toPgVectorString(embedding);
    }

    public int getDimension() {
        return embeddingService.getDimension();
    }
}
