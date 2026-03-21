package com.knowledge.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.common.vo.HotQueryVO;
import com.knowledge.controller.dto.*;
import com.knowledge.entity.AdminOperationLogEntity;
import com.knowledge.entity.ChatMessageEntity;
import com.knowledge.entity.KnowledgeEntryEntity;
import com.knowledge.mapper.AdminOperationLogMapper;
import com.knowledge.mapper.ChatMessageMapper;
import com.knowledge.mapper.KnowledgeEntryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Admin Service - System management, stats, analytics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final KnowledgeEntryMapper knowledgeEntryMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final AdminOperationLogMapper operationLogMapper;
    private final JdbcTemplate jdbcTemplate;
    private final KnowledgeService knowledgeService;

    // ── System Stats ───────────────────────────────────────────────────

    public AdminStatsResp getStats() {
        AdminStatsResp stats = new AdminStatsResp();
        stats.setTotalEntries(count("knowledge_base", null));
        stats.setTotalUsers(countDistinct("knowledge_base", "user_id"));
        stats.setTotalSessions(countDistinct("chat_messages", "session_key"));
        stats.setTotalSearches(count("chat_messages", "role = 'user'"));
        stats.setStorageBytes(stats.getTotalEntries() * 4096);
        stats.setEntriesByType(countGroupBy("knowledge_base", "content_type"));
        stats.setTopTags(topTags());
        stats.setSharedEntries(count("knowledge_base", "is_shared = true"));
        return stats;
    }

    public AdminMetricsResp getMetrics() {
        AdminMetricsResp r = new AdminMetricsResp();
        r.setTotalRequests(0L);
        r.setSuccessRequests(0L);
        r.setErrorRequests(0L);
        r.setEndpoints(new LinkedHashMap<>());
        return r;
    }

    // ── Dashboard ──────────────────────────────────────────────────────

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalEntries", count("knowledge_base", null));
        data.put("todayNew", count("knowledge_base", "created_at >= CURRENT_DATE"));
        data.put("totalSearches", count("chat_messages", "role = 'user'"));
        data.put("totalChats", countDistinct("chat_messages", "session_key"));
        return data;
    }

    public Map<String, Object> getVectorStatus() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("generating", count("knowledge_base", "embedding IS NULL"));
        data.put("completed", count("knowledge_base", "embedding IS NOT NULL"));
        data.put("failed", 0L);
        data.put("queue", 0L);
        return data;
    }

    public List<Map<String, Object>> getRecentActivities() {
        String sql = "SELECT id, title, content_type, source, created_at, user_id FROM knowledge_base ORDER BY created_at DESC LIMIT 20";
        return jdbcTemplate.queryForList(sql).stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", row.get("id"));
            m.put("title", row.get("title"));
            m.put("type", row.get("content_type"));
            m.put("source", row.get("source"));
            m.put("createdAt", row.get("created_at").toString());
            m.put("userId", Long.parseLong(row.get("user_id").toString()));
            return m;
        }).toList();
    }

    // ── Knowledge CRUD ────────────────────────────────────────────────

    public Map<String, Object> listEntries(String tag, String keyword, String type, Long userId, int page, int pageSize) {
        return knowledgeService.list(tag, keyword, type, userId, page, pageSize);
    }

    public KnowledgeEntryEntity getEntry(Long id) {
        QueryWrapper<KnowledgeEntryEntity> q = new QueryWrapper<>();
        q.eq("id", id);
        KnowledgeEntryEntity e = knowledgeEntryMapper.selectOne(q);
        if (e == null) throw new BusinessException("条目不存在");
        return e;
    }

    @Transactional
    public KnowledgeEntryEntity createEntry(InsertReq req) {
        return knowledgeService.insert(req);
    }

    @Transactional
    public boolean updateEntry(Long id, InsertReq req) {
        Long uid = req.getUserId() != null ? req.getUserId() : 1L;
        return knowledgeService.update(id, uid, req);
    }

    @Transactional
    public boolean deleteEntry(Long id, Long userId) {
        return knowledgeService.delete(id, userId);
    }

    @Transactional
    public Map<String, Object> batchImport(BatchImportReq req) {
        return knowledgeService.batchImport(req);
    }

    public Map<String, Object> rebuildEmbedding(Long id, Long userId) {
        return knowledgeService.rebuildEmbedding(id, userId);
    }

    // ── Retrieval Analytics ───────────────────────────────────────────

    public List<Map<String, Object>> getSearchHistory(Long userId, int limit) {
        List<ChatMessageEntity> messages = userId != null
            ? chatMessageMapper.findUserQueries(userId, limit)
            : chatMessageMapper.findAllQueries(limit);
        return messages.stream().map(m -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", m.getId());
            row.put("content", m.getContent());
            row.put("role", m.getRole());
            row.put("sessionKey", m.getSessionKey());
            row.put("createdAt", m.getCreatedAt() != null ? m.getCreatedAt().toString() : null);
            row.put("userId", m.getUserId());
            return row;
        }).toList();
    }

    public List<Map<String, Object>> getHotQueries() {
        List<HotQueryVO> hot = chatMessageMapper.findHotQueries(20);
        return hot.stream().map(h -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("query", h.getQuery());
            row.put("count", h.getCnt());
            return row;
        }).toList();
    }

    public Map<String, Object> getSearchStats() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalSearches", count("chat_messages", "role = 'user'"));
        data.put("searchesToday", count("chat_messages", "role = 'user' AND created_at >= CURRENT_DATE"));
        data.put("hotQueries", getHotQueries());
        return data;
    }

    // ── Users & Sessions ──────────────────────────────────────────────

    public Map<String, Object> listUsers(int page, int pageSize) {
        String cntSql = "SELECT COUNT(DISTINCT user_id) FROM knowledge_base";
        long total = jdbcTemplate.queryForObject(cntSql, Long.class);
        String sql = """
            SELECT kb.user_id, COUNT(DISTINCT kb.id) AS entry_count,
                   COUNT(DISTINCT cm.session_key) AS session_count,
                   MAX(kb.created_at) AS last_active
            FROM knowledge_base kb
            LEFT JOIN chat_messages cm ON cm.user_id = kb.user_id
            GROUP BY kb.user_id
            ORDER BY last_active DESC NULLS LAST
            LIMIT ? OFFSET ?
            """;
        int offset = (page - 1) * pageSize;
        List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, pageSize, offset);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("data", users);
        return result;
    }

    public Map<String, Object> listSessions(Long userId, int page, int pageSize) {
        Object[] params;
        String whereSql;
        if (userId != null) {
            whereSql = "WHERE cm.user_id = ?";
            params = new Object[]{userId.toString()};
        } else {
            whereSql = "WHERE 1=1";
            params = new Object[]{};
        }
        String cntSql = "SELECT COUNT(DISTINCT cm.session_key) FROM chat_messages cm " + whereSql;
        Long totalObj = jdbcTemplate.queryForObject(cntSql, Long.class, params);
        long total = totalObj != null ? totalObj : 0L;

        String sql = "SELECT cm.session_key, cm.user_id, COUNT(cm.id) AS message_count, "
                   + "MIN(cm.content) AS first_message, MAX(cm.content) AS last_message "
                   + "FROM chat_messages cm " + whereSql
                   + " GROUP BY cm.session_key, cm.user_id"
                   + " ORDER BY MAX(cm.created_at) DESC LIMIT ? OFFSET ?";
        Object[] allParams = new Object[params.length + 2];
        System.arraycopy(params, 0, allParams, 0, params.length);
        allParams[params.length] = pageSize;
        allParams[params.length + 1] = (page - 1) * pageSize;
        List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sql, allParams);
        sessions.forEach(s -> {
            String fm = (String) s.get("first_message");
            s.put("firstMessage", fm != null && fm.length() > 100 ? fm.substring(0, 100) + "..." : fm);
            String lm = (String) s.get("last_message");
            s.put("lastMessage", lm != null && lm.length() > 100 ? lm.substring(0, 100) + "..." : lm);
        });
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("data", sessions);
        return result;
    }

    // ── Monitor ──────────────────────────────────────────────────────

    public Map<String, Object> getApiMetrics() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalCalls", count("chat_messages", "role = 'user'"));
        data.put("callsToday", count("chat_messages", "created_at >= NOW() - INTERVAL '1 day'"));
        data.put("responseTimes", Collections.emptyList());
        data.put("callCounts", Collections.emptyList());
        return data;
    }

    public Map<String, Object> getOllamaStatus() {
        Map<String, Object> data = new LinkedHashMap<>();
        try {
            data.put("status", "running");
            data.put("model", "bge-m3");
        } catch (Exception e) {
            data.put("status", "error");
            data.put("message", e.getMessage());
        }
        return data;
    }

    public Map<String, Object> getDatabaseStatus() {
        Map<String, Object> data = new LinkedHashMap<>();
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            data.put("status", "connected");
            data.put("valid", true);
            data.put("database", "PostgreSQL");
        } catch (Exception e) {
            data.put("status", "error");
            data.put("message", e.getMessage());
        }
        return data;
    }

    // ── Config ───────────────────────────────────────────────────────

    public Map<String, Object> getVectorConfig() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("model", "bge-m3");
        data.put("dimension", 1024);
        data.put("batchSize", 10);
        return data;
    }

    public Map<String, Object> getRagConfig() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("topK", 5);
        data.put("maxContextLength", 4096);
        return data;
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private long count(String table, String where) {
        String sql = "SELECT COUNT(*) FROM " + table + (where != null ? " WHERE " + where : "");
        try {
            return jdbcTemplate.queryForObject(sql, Long.class);
        } catch (Exception e) {
            return 0L;
        }
    }

    private long countDistinct(String table, String column) {
        String sql = "SELECT COUNT(DISTINCT " + column + ") FROM " + table;
        try {
            return jdbcTemplate.queryForObject(sql, Long.class);
        } catch (Exception e) {
            return 0L;
        }
    }

    private Map<String, Long> countGroupBy(String table, String column) {
        Map<String, Long> m = new LinkedHashMap<>();
        String sql = "SELECT " + column + ", COUNT(*) FROM " + table + " GROUP BY " + column + " ORDER BY COUNT(*) DESC";
        try {
            jdbcTemplate.queryForList(sql).forEach(row -> {
                Object key = row.values().iterator().next();
                Object val = row.values().stream().skip(1).findFirst().orElse(0L);
                m.put(key.toString(), ((Number) val).longValue());
            });
        } catch (Exception ignored) {}
        return m;
    }

    private Map<String, Long> topTags() {
        Map<String, Long> m = new LinkedHashMap<>();
        String sql = "SELECT tag, COUNT(*) FROM knowledge_base, LATERAL unnest(tags) AS tag GROUP BY tag ORDER BY COUNT(*) DESC LIMIT 10";
        try {
            jdbcTemplate.queryForList(sql).forEach(row -> {
                Object key = row.values().iterator().next();
                Object val = row.values().stream().skip(1).findFirst().orElse(0L);
                m.put(key.toString(), ((Number) val).longValue());
            });
        } catch (Exception ignored) {}
        return m;
    }

    // Simple QueryWrapper alias to avoid import conflict
    private static class QueryWrapper<T> extends com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<T> {}
}
