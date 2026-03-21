package com.knowledge.controller;

import com.knowledge.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Health", description = "健康检查")
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/health")
    @Operation(summary = "健康检查")
    public Result<?> health() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Result.ok("OK", "Knowledge DAO API is running, DB connected");
        } catch (Exception e) {
            return Result.fail(503, "Database connection failed: " + e.getMessage());
        }
    }
}
