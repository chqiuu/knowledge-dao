package com.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "管理操作日志")
@TableName("admin_operation_logs")
public class AdminOperationLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private String userId;

    private String action;

    private String resource;

    @TableField("resource_id")
    private String resourceId;

    private String details;

    @TableField("ip_address")
    private String ipAddress;

    @TableField("status_code")
    private Integer statusCode;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
