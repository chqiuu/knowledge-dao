package com.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.common.exception.ResourceNotFoundException;
import com.knowledge.common.vo.ContentTypeCountVO;
import com.knowledge.controller.dto.*;
import com.knowledge.entity.KnowledgeEntryEntity;
import com.knowledge.mapper.KnowledgeEntryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.PreparedStatement;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Knowledge Entry Service - Business logic layer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgeEntryMapper knowledgeEntryMapper;
    private final VectorStoreService vectorStoreService;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public KnowledgeEntryEntity insert(InsertReq req) {
        try {
            String combined = req.getTitle() + " " + req.getContent();
            float[] embedding = vectorStoreService.embed(combined);
            String vectorStr = vectorStoreService.toVectorString(embedding);

            KnowledgeEntryEntity entity = new KnowledgeEntryEntity();
            entity.setTitle(req.getTitle());
            entity.setContent(req.getContent());
            entity.setContentType(req.getContentType() != null ? req.getContentType() : "article");
            entity.setTags(req.getTags());
            entity.setSource(req.getSource() != null ? req.getSource() : "api");
            entity.setUserId(req.getUserId());
            entity.setIsShared(false);

            // Use raw SQL for vector insert since MyBatis Plus doesn't handle pgvector natively
            knowledgeEntryMapper.insert(entity);

            // Update embedding separately
            updateEmbedding(entity.getId(), vectorStr);
            return entity;
        } catch (Exception e) {
            throw new BusinessException("插入知识失败: " + e.getMessage());
        }
    }

    private void updateEmbedding(Long id, String vectorStr) {
        knowledgeEntryMapper.updateEmbedding(id, vectorStr);
    }

    public KnowledgeEntryEntity findById(Long id, Long userId) {
        QueryWrapper<KnowledgeEntryEntity> q = new QueryWrapper<>();
        q.eq("id", id).eq("user_id", userId.toString());
        KnowledgeEntryEntity entity = knowledgeEntryMapper.selectOne(q);
        if (entity == null) {
            throw new ResourceNotFoundException("知识条目不存在");
        }
        return entity;
    }

    /**
     * Semantic search using vector similarity.
     */
    public List<SearchHit> search(String query, Long userId, int topK) {
        try {
            float[] queryEmbedding = vectorStoreService.embed(query);
            String queryVector = vectorStoreService.toVectorString(queryEmbedding);
            List<KnowledgeEntryEntity> results = knowledgeEntryMapper.searchByVector(queryVector, userId, topK);
            return results.stream().map(e -> {
                SearchHit hit = new SearchHit();
                hit.setId(e.getId());
                hit.setTitle(e.getTitle());
                hit.setContent(e.getContent());
                hit.setContentType(e.getContentType());
                hit.setTags(e.getTags());
                hit.setSource(e.getSource());
                hit.setCreatedAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
                hit.setUserId(e.getUserId());
                hit.setIsShared(e.getIsShared());
                return hit;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            throw new BusinessException("搜索失败: " + e.getMessage());
        }
    }

    @Transactional
    public boolean update(Long id, Long userId, InsertReq req) {
        try {
            String combined = req.getTitle() + " " + req.getContent();
            float[] embedding = vectorStoreService.embed(combined);
            String vectorStr = vectorStoreService.toVectorString(embedding);

            KnowledgeEntryEntity entity = new KnowledgeEntryEntity();
            entity.setId(id);
            entity.setTitle(req.getTitle());
            entity.setContent(req.getContent());
            entity.setContentType(req.getContentType() != null ? req.getContentType() : "article");
            entity.setTags(req.getTags());
            entity.setSource(req.getSource() != null ? req.getSource() : "api");
            knowledgeEntryMapper.updateById(entity);
            updateEmbedding(id, vectorStr);
            return true;
        } catch (Exception e) {
            throw new BusinessException("更新失败: " + e.getMessage());
        }
    }

    @Transactional
    public boolean delete(Long id, Long userId) {
        QueryWrapper<KnowledgeEntryEntity> q = new QueryWrapper<>();
        q.eq("id", id).eq("user_id", userId.toString());
        return knowledgeEntryMapper.delete(q) > 0;
    }

    public Map<String, Object> list(String tag, String keyword, String contentType, Long userId, int page, int pageSize) {
        QueryWrapper<KnowledgeEntryEntity> q = new QueryWrapper<>();
        if (StringUtils.hasText(tag)) {
            q.apply("? = ANY(tags)", tag);
        }
        if (StringUtils.hasText(keyword)) {
            q.and(w -> w.like("title", keyword).or().like("content", keyword));
        }
        if (StringUtils.hasText(contentType)) {
            q.eq("content_type", contentType);
        }
        if (userId != null) {
            q.eq("user_id", userId.toString());
        }
        q.orderByDesc("created_at");

        Page<KnowledgeEntryEntity> p = knowledgeEntryMapper.selectPage(new Page<>(page, pageSize), q);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", p.getTotal());
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("data", p.getRecords().stream().map(this::toItem).collect(Collectors.toList()));
        return result;
    }

    private KnowledgeListResp.KnowledgeItem toItem(KnowledgeEntryEntity e) {
        KnowledgeListResp.KnowledgeItem item = new KnowledgeListResp.KnowledgeItem();
        item.setId(e.getId());
        item.setTitle(e.getTitle());
        item.setContent(e.getContent());
        item.setContentType(e.getContentType());
        item.setSource(e.getSource());
        item.setCreatedAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        item.setUpdatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null);
        item.setUserId(e.getUserId());
        item.setIsShared(e.getIsShared());
        item.setTags(e.getTags());
        return item;
    }

    @Transactional
    public Map<String, Object> batchImport(BatchImportReq req) {
        int success = 0, failed = 0;
        List<Map<String, Object>> results = new ArrayList<>();
        for (BatchImportReq.BatchEntry e : req.getEntries()) {
            try {
                String title = e.getTitle() != null ? e.getTitle() : "";
                String content = e.getContent() != null ? e.getContent() : "";
                String source = e.getSource() != null ? e.getSource() : "api";
                String contentType = e.getContentType() != null ? e.getContentType() : "article";
                Long userId = e.getUserId() != null ? e.getUserId() : 1L;

                // 生成向量
                String combined = title + " " + content;
                float[] embedding = vectorStoreService.embed(combined);
                String vectorStr = vectorStoreService.toVectorString(embedding);

                // PostgreSQL text[] 格式: {"tag1","tag2"}
                List<String> tagList = e.getTagsAsList();
                String tagsStr = tagList.isEmpty() ? "{}"
                    : "{\"" + String.join("\",\"", tagList.stream().map(t -> t.replace("\\", "\\\\").replace("\"", "\\\"")).toList()) + "\"}";

                // 用 PreparedStatement 直接设参，避免 SQL 注入风险
                final String finalContentType = contentType;
                final String finalTagsStr = tagsStr;
                final String finalTitle = title;
                final String finalContent = content;
                final String finalSource = source;
                final Long finalUserId = userId;
                final String finalVector = vectorStr;

                jdbcTemplate.update(conn -> {
                    PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO knowledge_base (title, content, content_type, tags, source, user_id, is_shared, created_at, updated_at, embedding) VALUES (?, ?, ?, ?::text[], ?, ?, false, NOW(), NOW(), ?::vector)",
                        PreparedStatement.RETURN_GENERATED_KEYS);
                    ps.setString(1, finalTitle);
                    ps.setString(2, finalContent);
                    ps.setString(3, finalContentType);
                    ps.setString(4, finalTagsStr);
                    ps.setString(5, finalSource);
                    ps.setLong(6, finalUserId);
                    ps.setString(7, finalVector);
                    return ps;
                });

                // 取刚插入的 id
                Long id = jdbcTemplate.queryForObject("SELECT lastval()", Long.class);
                Map<String, Object> r2 = new LinkedHashMap<>();
                r2.put("id", id);
                r2.put("title", title);
                r2.put("status", "success");
                results.add(r2);
                success++;
            } catch (Exception ex) {
                Map<String, Object> r2 = new LinkedHashMap<>();
                r2.put("title", e.getTitle());
                r2.put("status", "failed");
                r2.put("error", ex.getMessage());
                results.add(r2);
                failed++;
            }
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", req.getEntries().size());
        summary.put("success", success);
        summary.put("failed", failed);
        summary.put("results", results);
        return summary;
    }

    @Transactional
    public Map<String, Object> rebuildEmbedding(Long id, Long userId) {
        try {
            KnowledgeEntryEntity e = findById(id, userId);
            String combined = e.getTitle() + " " + e.getContent();
            float[] embedding = vectorStoreService.embed(combined);
            String vectorStr = vectorStoreService.toVectorString(embedding);
            updateEmbedding(id, vectorStr);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", id);
            r.put("embeddingDimension", embedding.length);
            r.put("rebuilt", true);
            return r;
        } catch (Exception ex) {
            throw new BusinessException("重建向量失败: " + ex.getMessage());
        }
    }

    public StatsResp getStats(Long userId) {
        StatsResp stats = new StatsResp();
        stats.setTotalEntries(knowledgeEntryMapper.countByUserId(userId));
        StringBuilder ctSb = new StringBuilder();
        for (var ct : knowledgeEntryMapper.countGroupByContentType(userId)) {
            if (ctSb.length() > 0) ctSb.append(", ");
            ctSb.append(ct.getContentType()).append(":").append(ct.getCount());
        }
        stats.setContentTypes(ctSb.toString());
        // Top tags - simplified
        stats.setTopTags("");
        return stats;
    }

    @lombok.Data
    public static class SearchHit {
        private Long id;
        private String title;
        private String content;
        private String contentType;
        private List<String> tags;
        private String source;
        private String createdAt;
        private Long userId;
        private Boolean isShared;
    }
}
