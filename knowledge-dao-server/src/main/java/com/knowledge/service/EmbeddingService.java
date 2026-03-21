package com.knowledge.service;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.knowledge.config.OllamaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Embedding service calling Ollama API to generate vectors.
 * Uses bge-m3 model, dimension 1024.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final OllamaConfig config;

    /**
     * Generate embedding for a single text.
     */
    public float[] embed(String text) throws Exception {
        if (text == null || text.isBlank()) {
            return new float[config.getEmbeddingDimension()];
        }

        JSONObject body = JSONUtil.createObj()
            .set("model", config.getEmbeddingModel())
            .set("prompt", text);

        String response = HttpUtil.createPost(config.getBaseUrl() + "/api/embeddings")
            .header("Content-Type", "application/json")
            .body(body.toString())
            .timeout(60000)
            .execute()
            .body();

        JSONObject json = JSONUtil.parseObj(response);
        if (json.containsKey("error")) {
            throw new RuntimeException("Embedding error: " + json.getStr("error"));
        }

        List<Number> emb = json.getBeanList("embedding", Number.class);
        if (emb == null) {
            throw new RuntimeException("Invalid embedding response");
        }

        float[] result = new float[emb.size()];
        for (int i = 0; i < emb.size(); i++) {
            double val = emb.get(i).doubleValue();
            if (Double.isNaN(val) || Double.isInfinite(val)) {
                result[i] = 0.0f;
            } else {
                result[i] = (float) val;
            }
        }
        return result;
    }

    public int getDimension() {
        return config.getEmbeddingDimension();
    }

    /**
     * Convert float[] to pgvector string format for SQL insertion.
     */
    public String toPgVectorString(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            double val = embedding[i];
            if (Double.isNaN(val) || Double.isInfinite(val)) {
                sb.append("0.0");
            } else {
                sb.append(val);
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
