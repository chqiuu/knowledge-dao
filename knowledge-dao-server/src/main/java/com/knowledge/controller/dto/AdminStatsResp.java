package com.knowledge.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "管理后台统计响应")
public class AdminStatsResp {
    private Long totalEntries;
    private Long totalUsers;
    private Long totalSessions;
    private Long totalSearches;
    private Long storageBytes;
    private Map<String, Long> entriesByType;
    private Map<String, Long> topTags;
    private Long sharedEntries;
}
