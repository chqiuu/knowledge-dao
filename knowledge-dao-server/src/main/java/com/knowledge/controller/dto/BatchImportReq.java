package com.knowledge.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

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
        // 支持 List<String>（JSON数组）或 String（逗号分隔）
        private Object tags;
        private String source;
        private String contentType = "article";

        public List<String> getTagsAsList() {
            if (tags == null) return List.of();
            if (tags instanceof List) {
                return ((List<?>) tags).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
            }
            if (tags instanceof String) {
                String s = (String) tags;
                if (s.isEmpty()) return List.of();
                return Arrays.stream(s.split(","))
                    .map(String::trim)
                    .filter(x -> !x.isEmpty())
                    .collect(Collectors.toList());
            }
            return List.of();
        }
    }
}
