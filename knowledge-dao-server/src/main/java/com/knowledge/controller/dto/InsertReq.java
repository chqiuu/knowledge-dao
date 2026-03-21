package com.knowledge.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "插入知识请求")
public class InsertReq {

    @NotBlank(message = "title不能为空")
    private String title;

    @NotBlank(message = "content不能为空")
    private String content;

    @NotNull(message = "userId不能为空")
    private Long userId;

    private List<String> tags;
    private String source;
    private String contentType = "article";
}
