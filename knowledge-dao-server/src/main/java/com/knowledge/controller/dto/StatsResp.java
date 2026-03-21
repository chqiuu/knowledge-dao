package com.knowledge.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Map;

@Data
@Schema(description = "统计响应")
public class StatsResp {
    private Long totalEntries;
    private String contentTypes;
    private String topTags;
}
