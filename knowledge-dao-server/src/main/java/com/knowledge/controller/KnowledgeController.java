package com.knowledge.controller;

import com.knowledge.common.result.Result;
import com.knowledge.controller.dto.*;
import com.knowledge.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Tag(name = "Knowledge API", description = "知识库管理 API")
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @GetMapping
    @Operation(summary = "知识列表")
    public Result<?> list(@RequestParam(required = false) String tag,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String type,
                          @RequestParam(required = false) Long userId,
                          @RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(knowledgeService.list(tag, keyword, type, userId, page, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "知识详情")
    public Result<?> get(@PathVariable Long id, @RequestParam(required = false) Long userId) {
        return Result.ok(knowledgeService.findById(id, userId != null ? userId : 1L));
    }

    @PostMapping
    @Operation(summary = "创建知识")
    public Result<?> create(@Valid @RequestBody InsertReq req) {
        return Result.ok(knowledgeService.insert(req), "创建成功");
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新知识")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody InsertReq req) {
        Long uid = req.getUserId() != null ? req.getUserId() : 1L;
        knowledgeService.update(id, uid, req);
        return Result.ok(null, "更新成功");
    }

    @DeleteMapping
    @Operation(summary = "批量删除知识")
    public Result<?> delete(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.fail(400, "ids不能为空");
        }
        int deleted = 0;
        for (Long id : ids) {
            if (knowledgeService.delete(id, 1L)) deleted++;
        }
        return Result.ok(deleted);
    }

    @GetMapping("/tags")
    @Operation(summary = "获取标签列表")
    public Result<?> tags() {
        // Return default tags since we don't have a tags endpoint
        return Result.ok(Arrays.asList("java", "ai", "spring", "api", "架构", "rag", "数据库", "git", "redis"));
    }

    @PostMapping("/batch-recalc-vector")
    @Operation(summary = "批量重建向量")
    public Result<?> batchRecalcVector(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        int rebuilt = 0;
        if (ids != null) {
            for (Long id : ids) {
                try {
                    knowledgeService.rebuildEmbedding(id, 1L);
                    rebuilt++;
                } catch (Exception ignored) {}
            }
        }
        return Result.ok(Map.of("rebuilt", rebuilt, "total", ids != null ? ids.size() : 0));
    }

    @PostMapping("/batch-import")
    @Operation(summary = "批量导入")
    public Result<?> batchImport(@Valid @RequestBody BatchImportReq req) {
        return Result.ok(knowledgeService.batchImport(req));
    }
}
