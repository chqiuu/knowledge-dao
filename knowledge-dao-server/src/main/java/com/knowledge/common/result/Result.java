package com.knowledge.common.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体
 */
@Data
@Schema(description = "统一响应体")
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "状态码：200成功，其他失败")
    private int code;
    @Schema(description = "消息")
    private String message = "success";
    @Schema(description = "数据")
    private T data;
    @Schema(description = "时间戳")
    private final long timestamp = System.currentTimeMillis();

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        return ok(data, null);
    }

    public static <T> Result<T> ok(T data, String message) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.data = data;
        if (message != null) r.message = message;
        return r;
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }

    public static <T> Result<T> fail(String message) {
        return fail(500, message);
    }

    public static <T> Result<T> fail() {
        return fail(500, "操作失败");
    }
}
