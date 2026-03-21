package com.knowledge.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Map;

@Data
@Schema(description = "管理后台指标响应")
public class AdminMetricsResp {
    private Long totalRequests;
    private Long successRequests;
    private Long errorRequests;
    private Double avgResponseTimeMs;
    private Double p99ResponseTimeMs;
    private Map<String, EndpointMetrics> endpoints;

    @Data
    public static class EndpointMetrics {
        private Long calls;
        private Long errors;
        private Double avgMs;
        private Double p99Ms;
    }
}
