package com.knowledge.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "知识条目列表响应")
public class KnowledgeListResp {
    private Long total;
    private Integer page;
    private Integer pageSize;
    private List<KnowledgeItem> data;

    @Data
    public static class KnowledgeItem {
        private Long id;
        private String title;
        private String content;
        private String contentType;
        private String source;
        private String createdAt;
        private String updatedAt;
        private Long userId;
        private Boolean isShared;
        private List<String> tags;
    }
}
