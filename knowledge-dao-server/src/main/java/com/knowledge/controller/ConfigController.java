package com.knowledge.controller;

import com.knowledge.common.result.Result;
import com.knowledge.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Config API", description = "配置管理 API")
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final AdminService adminService;

    @GetMapping("/vector")
    @Operation(summary = "获取向量配置")
    public Result<?> getVectorConfig() {
        return Result.ok(adminService.getVectorConfig());
    }

    @PutMapping("/vector")
    @Operation(summary = "更新向量配置")
    public Result<?> putVectorConfig(@RequestBody Map<String, Object> body) {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("model", body.getOrDefault("model", "bge-m3"));
        data.put("dimension", body.getOrDefault("dimension", 1024));
        data.put("batchSize", body.getOrDefault("batchSize", 10));
        return Result.ok(data);
    }

    @GetMapping("/rag")
    @Operation(summary = "获取 RAG 配置")
    public Result<?> getRagConfig() {
        return Result.ok(adminService.getRagConfig());
    }

    @PutMapping("/rag")
    @Operation(summary = "更新 RAG 配置")
    public Result<?> putRagConfig(@RequestBody Map<String, Object> body) {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("topK", body.getOrDefault("topK", 5));
        data.put("maxContextLength", body.getOrDefault("maxContextLength", 4096));
        return Result.ok(data);
    }
}
