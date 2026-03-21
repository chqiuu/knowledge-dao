package com.knowledge.controller;

import com.knowledge.common.result.Result;
import com.knowledge.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Monitor API", description = "系统监控 API")
@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final AdminService adminService;

    @GetMapping("/api-metrics")
    @Operation(summary = "API 指标")
    public Result<?> apiMetrics() {
        return Result.ok(adminService.getApiMetrics());
    }

    @GetMapping("/ollama-status")
    @Operation(summary = "Ollama 状态")
    public Result<?> ollamaStatus() {
        return Result.ok(adminService.getOllamaStatus());
    }

    @GetMapping("/database-status")
    @Operation(summary = "数据库状态")
    public Result<?> databaseStatus() {
        return Result.ok(adminService.getDatabaseStatus());
    }
}
