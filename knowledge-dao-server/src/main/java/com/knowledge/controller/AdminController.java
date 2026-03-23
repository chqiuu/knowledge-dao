package com.knowledge.controller;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.common.exception.ResourceNotFoundException;
import com.knowledge.common.result.Result;
import com.knowledge.controller.dto.*;
import com.knowledge.entity.KnowledgeEntryEntity;
import com.knowledge.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Admin API", description = "管理后台 API")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/health")
    @Operation(summary = "健康检查")
    public Result<?> health() {
        return Result.ok("OK", "DB connected");
    }

    @GetMapping("/stats")
    @Operation(summary = "系统统计")
    public Result<?> stats() {
        return Result.ok(adminService.getStats());
    }

    @GetMapping("/metrics")
    @Operation(summary = "API 指标")
    public Result<?> metrics() {
        return Result.ok(adminService.getMetrics());
    }

    // ── Entries CRUD ──────────────────────────────────────────────────

    @GetMapping("/entries")
    @Operation(summary = "知识条目列表")
    public Result<?> listEntries(
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(adminService.listEntries(tag, keyword, type, userId, page, pageSize));
    }

    @GetMapping("/entries/{id}")
    @Operation(summary = "知识条目详情")
    public Result<?> getEntry(@PathVariable Long id) {
        return Result.ok(adminService.getEntry(id));
    }

    @PostMapping("/entries")
    @Operation(summary = "创建知识条目")
    public Result<?> createEntry(@Valid @RequestBody InsertReq req) {
        return Result.ok(adminService.createEntry(req), "创建成功");
    }

    @PutMapping("/entries/{id}")
    @Operation(summary = "更新知识条目")
    public Result<?> updateEntry(@PathVariable Long id, @Valid @RequestBody InsertReq req) {
        adminService.updateEntry(id, req);
        return Result.ok(null, "更新成功");
    }

    @DeleteMapping("/entries/{id}")
    @Operation(summary = "删除知识条目")
    public Result<?> deleteEntry(@PathVariable Long id, @RequestParam(required = false) Long userId) {
        adminService.deleteEntry(id, userId != null ? userId : 1L);
        return Result.ok(null, "删除成功");
    }

    @PostMapping("/entries/batch")
    @Operation(summary = "批量导入")
    public Result<?> batchImport(@Valid @RequestBody BatchImportReq req) {
        return Result.ok(adminService.batchImport(req), "批量导入完成");
    }

    @PostMapping("/entries/{id}/rebuild")
    @Operation(summary = "重建向量")
    public Result<?> rebuildEntry(@PathVariable Long id, @RequestParam(required = false) Long userId) {
        return Result.ok(adminService.rebuildEmbedding(id, userId != null ? userId : 1L), "重建成功");
    }

    // ── Dashboard ────────────────────────────────────────────────────

    @GetMapping("/activities")
    @Operation(summary = "最近活动（时间线）")
    public Result<?> activities() {
        return Result.ok(adminService.getRecentActivities());
    }

    @GetMapping("/vector-status")
    @Operation(summary = "向量状态")
    public Result<?> vectorStatus() {
        return Result.ok(adminService.getVectorStatus());
    }

    @GetMapping("/alerts")
    @Operation(summary = "告警信息")
    public Result<?> alerts() {
        return Result.ok(adminService.getAlerts());
    }

    // ── Search Analytics ──────────────────────────────────────────────

    @GetMapping("/searches")
    @Operation(summary = "搜索历史")
    public Result<?> searches(@RequestParam(required = false) Long userId,
                             @RequestParam(defaultValue = "50") int limit) {
        return Result.ok(adminService.getSearchHistory(userId, limit));
    }

    @GetMapping("/searches/stats")
    @Operation(summary = "搜索统计")
    public Result<?> searchStats() {
        return Result.ok(adminService.getSearchStats());
    }

    // ── Users & Sessions ──────────────────────────────────────────────

    @GetMapping("/users")
    @Operation(summary = "用户列表")
    public Result<?> listUsers(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(adminService.listUsers(page, pageSize));
    }

    @GetMapping("/sessions")
    @Operation(summary = "会话列表")
    public Result<?> listSessions(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        return Result.ok(adminService.listSessions(userId, page, pageSize));
    }

    @GetMapping("/sessions/{sessionKey}/messages")
    @Operation(summary = "会话消息详情")
    public Result<?> getSessionMessages(@PathVariable String sessionKey) {
        return Result.ok(adminService.getSessionMessages(sessionKey));
    }

    // ── Batch Operations ──────────────────────────────────────────────

    @PostMapping("/batch-recalc-vector")
    @Operation(summary = "批量重建向量")
    public Result<?> batchRecalcVector(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.fail(400, "ids不能为空");
        }
        int rebuilt = 0;
        for (Long id : ids) {
            try {
                adminService.rebuildEmbedding(id, 1L);
                rebuilt++;
            } catch (Exception ignored) {}
        }
        return Result.ok(Map.of("rebuilt", rebuilt, "total", ids.size()));
    }
}
