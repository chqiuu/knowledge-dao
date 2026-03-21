package com.knowledge.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "聊天响应")
public class ChatResp {
    private List<KnowledgeHit> hits;
    private Integer hitCount;
    private String context;
    private String answer;

    @Data
    public static class KnowledgeHit {
        private Long id;
        private String title;
        private String content;
        private List<String> tags;
        private String source;
        private Double score;
    }
}
