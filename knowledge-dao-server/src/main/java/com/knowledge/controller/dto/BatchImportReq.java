package com.knowledge.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "批量导入请求")
public class BatchImportReq {

    @NotNull(message = "entries不能为空")
    private List<BatchEntry> entries;

    @Data
    public static class BatchEntry {
        private String title;
        private String content;
        private Long userId;
        private List<String> tags;
        private String source;
        private String contentType = "article";
    }
}
