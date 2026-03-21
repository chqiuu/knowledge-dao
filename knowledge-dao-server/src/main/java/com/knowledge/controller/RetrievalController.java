package com.knowledge.controller;

import com.knowledge.common.result.Result;
import com.knowledge.service.AdminService;
import com.knowledge.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Retrieval API", description = "检索分析 API")
@RestController
@RequestMapping("/api/retrieval")
@RequiredArgsConstructor
public class RetrievalController {

    private final KnowledgeService knowledgeService;
    private final AdminService adminService;

    @PostMapping("/search")
    @Operation(summary = "检索搜索")
    public Result<?> search(@RequestBody Map<String, Object> body) {
        String query = (String) body.get("query");
        if (query == null || query.isBlank()) {
            return Result.fail(400, "query不能为空");
        }
        Long userId = body.get("userId") != null ? ((Number) body.get("userId")).longValue() : 1L;
        int topK = body.get("topK") != null ? ((Number) body.get("topK")).intValue() : 5;

        List<KnowledgeService.SearchHit> results = knowledgeService.search(query, userId, topK);
        List<Map<String, Object>> rows = results.stream().map(r -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("title", r.getTitle());
            m.put("content", r.getContent());
            m.put("score", 1.0);
            m.put("tags", r.getTags());
            m.put("source", r.getSource());
            return m;
        }).toList();
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("results", rows);
        result.put("total", rows.size());
        return Result.ok(result);
    }

    @GetMapping("/history")
    @Operation(summary = "搜索历史")
    public Result<?> history(@RequestParam(required = false) Long userId,
                              @RequestParam(defaultValue = "50") int limit) {
        return Result.ok(adminService.getSearchHistory(userId, limit));
    }

    @GetMapping("/hot-queries")
    @Operation(summary = "热门查询")
    public Result<?> hotQueries() {
        return Result.ok(adminService.getHotQueries());
    }

    @GetMapping("/test")
    @Operation(summary = "检索测试")
    public Result<?> test() {
        // Simple test - return a few entries
        return Result.ok(Map.of("results", List.of(), "total", 0));
    }
}
