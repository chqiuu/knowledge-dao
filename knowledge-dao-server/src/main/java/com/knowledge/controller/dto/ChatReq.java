package com.knowledge.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "聊天请求")
public class ChatReq {

    @NotBlank(message = "query不能为空")
    private String query;

    @NotNull(message = "userId不能为空")
    private Long userId;

    private Integer topK = 5;
    private String sessionKey;
}
