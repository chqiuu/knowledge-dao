package com.knowledge.controller;

import com.knowledge.common.result.Result;
import com.knowledge.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Dashboard API", description = "仪表盘 API")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final AdminService adminService;

    @GetMapping("/stats")
    @Operation(summary = "仪表盘统计")
    public Result<?> stats() {
        return Result.ok(adminService.getDashboardStats());
    }

    @GetMapping("/vector-status")
    @Operation(summary = "向量状态")
    public Result<?> vectorStatus() {
        return Result.ok(adminService.getVectorStatus());
    }

    @GetMapping("/activities")
    @Operation(summary = "最近活动")
    public Result<?> activities() {
        return Result.ok(adminService.getRecentActivities());
    }

    @GetMapping("/alerts")
    @Operation(summary = "告警信息")
    public Result<?> alerts() {
        Map<String, Object> status = adminService.getVectorStatus();
        List<Map<String, Object>> alerts;
        Object generating = status.getOrDefault("generating", 0L);
        long genCount = generating instanceof Number ? ((Number) generating).longValue() : 0L;
        if (genCount > 0) {
            alerts = List.of(Map.of(
                "level", "warning",
                "message", generating + " entries missing vector embeddings"
            ));
        } else {
            alerts = List.of();
        }
        return Result.ok(alerts);
    }
}
