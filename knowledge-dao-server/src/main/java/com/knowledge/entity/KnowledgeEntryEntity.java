package com.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.knowledge.mapper.handler.StringArrayTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "知识条目")
@TableName("knowledge_base")
public class KnowledgeEntryEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String content;

    @TableField("content_type")
    private String contentType;

    @TableField(value = "tags", typeHandler = StringArrayTypeHandler.class, jdbcType = JdbcType.OTHER)
    private List<String> tags;

    /**
     * Embedding stored as pgvector VECTOR(1024).
     * Stored as String "[0.1,0.2,...]" in the entity.
     */
    @TableField("embedding")
    private String embeddingVector;

    private String source;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField("user_id")
    private Long userId;

    @TableField("is_shared")
    private Boolean isShared;
}
