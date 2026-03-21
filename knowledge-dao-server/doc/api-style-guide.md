# API 风格指南

> 基于 kin-tree-cloud-server 规范，为 knowledge-dao 定义的 REST API 设计标准
> 版本：1.0.0
> 参考：kin-tree-cloud-server v1.0

---

## 1. 统一响应格式

### 1.1 响应体结构

所有 API 必须使用统一的 `Result<T>` 包装结构：

```json
{
  "code": 1,
  "message": "success",
  "data": { ... },
  "time": 1742544000000
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 状态码：`1` = 成功，`0` = 失败；业务错误码见错误码表 |
| message | string | 状态描述文字，成功默认为 `"success"` |
| data | T | 响应数据，失败时为 `null` |
| time | long | 服务器时间戳（毫秒） |

**与 kin-tree-cloud 保持一致的设计理由：**
- `code: 1` 表示成功（非 200 HTTP 状态码），与微信小程序等前端框架 axios 拦截器兼容
- 错误信息通过 `message` 字段传递，不依赖 HTTP 状态码
- 时间戳用于客户端防重放校验

### 1.2 错误码体系（ResultEnum）

```java
// 成功
SUCCESS(1, "success")

// 失败
FAILED(0, "failed")

// 用户端错误（1040x）
USER_NOT_FOUND(10401, "用户不存在")
USER_LOGIN_EXPIRED(10402, "登录已过期，请重新登录")
USER_NOT_LOGIN(10403, "请先登录")

// 参数错误（1041x）
PARAM_EMPTY(10410, "必填参数为空")
PARAM_INVALID(10411, "参数格式错误")
PARAM_OUT_OF_RANGE(10412, "参数超出范围")
UPLOAD_FAILED(10414, "文件上传失败")

// 权限错误（1030x）
PERMISSION_DENIED(10301, "无权限访问")

// 资源错误（1040x）
NOT_FOUND(10404, "资源不存在")
DUPLICATE(10405, "数据重复")

// 系统错误（5xxxx）
SYSTEM_ERROR(50000, "系统繁忙，请稍后再试")
```

**knowledge-dao 业务错误码扩展：**

```java
// RAG 专用（1050x）
RAG_NO_RESULTS(10501, "未找到相关知识")
RAG_CONTEXT_BUILD_FAILED(10502, "上下文构建失败")
EMBEDDING_FAILED(10503, "向量生成失败")

// 知识库（1051x）
ENTRY_NOT_FOUND(10511, "知识条目不存在")
ENTRY_DUPLICATE_TITLE(10512, "同名知识条目已存在")

// 配置（1052x）
CONFIG_NOT_FOUND(10521, "配置项不存在")
CONFIG_INVALID(10522, "配置值无效")
```

### 1.3 HTTP 状态码映射

| HTTP 状态码 | 使用场景 |
|-------------|---------|
| 200 | 查询/更新成功（业务上的成功，即使 data 为空） |
| 201 | 创建资源成功 |
| 400 | 参数缺失、类型错误、校验失败 |
| 401 | 未认证（无 token 或 token 无效） |
| 403 | 无权限访问 |
| 404 | 资源不存在（知识条目、用户等） |
| 405 | 不支持的 HTTP 方法 |
| 500 | 服务器内部异常（已记录日志，不暴露堆栈） |

---

## 2. URL 路径规范

### 2.1 路径结构

```
/api/{module}/{resource}
/api/{module}/{resource}/{id}
/api/{module}/{resource}/{id}/{sub-resource}
```

**示例（kin-tree-cloud 风格）：**

```
POST   /api/miniapp/family-trees                    # 创建家谱
GET    /api/miniapp/family-trees                   # 列表
GET    /api/miniapp/family-trees/{treeId}          # 详情
POST   /api/miniapp/family-trees/{treeId}/delete   # 删除（用 POST 而非 DELETE，避免 body 限制）
GET    /api/miniapp/family-trees/{treeId}/branches # 子资源
```

### 2.2 knowledge-dao URL 规范（对齐后）

| 模块前缀 | 含义 |
|---------|------|
| `/api/rag` | RAG 核心功能（检索、聊天） |
| `/api/admin` | 管理功能（知识管理、用户、系统） |
| `/api/dashboard` | 仪表盘统计 |
| `/api/knowledge` | 知识库 CRUD（用户级） |
| `/api/retrieval` | 检索分析（搜索历史、热词） |
| `/api/monitor` | 系统监控 |
| `/api/config` | 配置管理 |

### 2.3 路径命名规则

- **全部小写**，用连字符 `-` 分隔语义（例：`/api/knowledge/batch-import`）
- **避免动词**：动作通过 HTTP 方法表达，不在路径中出现 `create`、`update`、`delete`
- **例外**：具有副作用的操作使用 `POST + /action` 形式，如 `/api/knowledge/batch-recalc-vector`
- **ID 参数**：使用路径参数 `{id}`，如 `/api/knowledge/{id}`

---

## 3. HTTP 方法规范

| 方法 | 语义 | 使用场景 |
|------|------|---------|
| GET | 查询 | 获取资源列表、详情、统计数据 |
| POST | 创建 | 新增资源、触发操作（如 rebuild、batch-import） |
| PUT | 更新 | 完整更新资源（替换） |
| DELETE | 删除 | 删除资源（需校验 ownership） |

**特殊规则：**
- 删除操作使用 `POST /{resource}/{id}/delete` 而非 `DELETE`，避免浏览器/网关对 body 的限制
- 批量操作使用 `POST /{resource}/batch` 或 `POST /{resource}/{id}/action`

---

## 4. 请求与响应规范

### 4.1 分页参数

| 参数 | 类型 | 默认值 | 最大值 | 说明 |
|------|------|--------|--------|------|
| page | int | 1 | - | 页码（从 1 开始） |
| pageSize | int | 20 | 100 | 每页条数 |
| sort | string | created_at | - | 排序字段 |
| order | string | desc | - | 排序方向：asc / desc |

**分页响应结构：**

```json
{
  "code": 1,
  "message": "success",
  "data": {
    "total": 38420,
    "page": 1,
    "pageSize": 20,
    "data": [ ... ]
  },
  "time": 1742544000000
}
```

### 4.2 过滤与搜索参数

| 参数 | 类型 | 说明 |
|------|------|------|
| keyword | string | 关键词搜索（匹配 title、content） |
| tag | string | 按标签筛选（精确匹配） |
| type / contentType | string | 按内容类型筛选 |
| userId | long | 按用户筛选 |
| startDate / endDate | string | 按日期范围筛选（ISO 8601） |

### 4.3 时间格式

- **请求/响应中**：ISO 8601 字符串，如 `"2026-03-22T10:00:00+08:00"`
- **时间戳**：毫秒级 Unix 时间戳（`time` 字段）
- **数据库**：PostgreSQL `TIMESTAMP WITH TIME ZONE`

---

## 5. DTO 命名规范

### 5.1 命名约定（对齐 kin-tree-cloud）

| 类型 | 命名格式 | 示例 |
|------|---------|------|
| 请求 DTO | `{Action}DTO` / `{Action}Request` | `InsertDTO`、`SearchRequest` |
| 响应 VO | `{Resource}VO` / `{Resource}Response` | `StatsVO`、`LoginVO` |
| 列表项 | `{Resource}Item` | `EntryItem` |
| 分页响应 | `{Resource}PageResponse` | `EntryPageResponse` |
| 错误 DTO | `ErrorResponse` | `ErrorResponse` |

### 5.2 DTO 字段规范

```java
public class InsertRequest {
    // 基础类型 + Lombok @Getter/@Setter
    // 每个字段必须有 Javadoc 说明
    // 使用包装类型（Long 而非 long）表示可空
    // 必须校验的字段加 @NotNull、@NotBlank 等 Jakarta 校验注解
}
```

---

## 6. 控制器规范

### 6.1 注解规范（Spring Boot + springdoc-openapi）

```java
@RestController
@RequestMapping("/api/rag")
@Tag(name = "RAG-检索", description = "知识库语义检索与 RAG 问答")
@RequiredArgsConstructor
public class RagController {

    @Operation(
        summary = "语义搜索",
        description = "基于向量检索的语义搜索，返回最相关的知识条目"
    )
    @ApiResponse(
        description = "搜索结果",
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = SearchResponse.class))
    )
    @PostMapping("/search")
    public Result<List<SearchResultVO>> search(
            @RequestBody @Valid SearchRequest request
    ) {
        // ...
    }
}
```

### 6.2 @Tag 分组（与 kin-tree-cloud 对齐）

| Tag 名称 | 描述 |
|---------|------|
| `RAG-检索` | /api/rag 核心检索 |
| `管理后台-知识管理` | /api/admin/entries |
| `管理后台-系统` | /api/admin/stats, health, metrics |
| `仪表盘` | /api/dashboard |
| `知识库-用户` | /api/knowledge |
| `检索分析` | /api/retrieval |
| `系统监控` | /api/monitor |
| `配置中心` | /api/config |

---

## 7. 错误处理规范

### 7.1 全局异常处理

使用统一异常拦截，返回 `Result.fail()` 结构：

```java
// 业务异常（已知）
throw new UserException(ResultEnum.PARAM_INVALID, "用户名不能为空");

// 系统异常（未知）
throw new RuntimeException("数据库连接失败");  // 被全局异常处理器捕获，转换为 SYSTEM_ERROR
```

### 7.2 不暴露内部细节

- 错误 `message` 对外显示友好提示
- 详细堆栈日志记录到服务端，不返回客户端
- 500 错误统一返回：`"系统繁忙，请稍后再试"`

### 7.3 校验异常

```java
// 使用 Jakarta Validation
public class InsertRequest {
    @NotBlank(message = "title 不能为空")
    private String title;

    @NotNull(message = "userId 不能为空")
    @Positive(message = "userId 必须为正整数")
    private Long userId;
}
```

---

## 8. OpenAPI 文档规范

### 8.1 文档访问

- Swagger UI：`GET /swagger-ui.html`
- OpenAPI JSON：`GET /v3/api-docs`
- OpenAPI YAML：`GET /v3/api-docs.yaml`

### 8.2 必需注解

每个端点必须标注：
- `@Tag` — 归属模块（类级别）
- `@Operation` — 端点说明、摘要（方法级别）
- `@Parameter` / `@Schema` — 参数和响应体说明
- `@ApiResponse` — 响应码和响应体结构

### 8.3 示例响应

```java
@Operation(summary = "知识条目详情")
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "成功",
        content = @Content(
            schema = @Schema(implementation = EntryDetailVO.class),
            example = "{\"code\":1,\"message\":\"success\",\"data\":{...}}"
        )
    ),
    @ApiResponse(responseCode = "404", description = "条目不存在")
})
@GetMapping("/{id}")
public Result<EntryDetailVO> getById(@PathVariable Long id) { ... }
```

---

## 9. 版本控制

当前版本号：`v1`（体现在 OpenAPI 文档的 `info.version`）

URL 中不体现版本号（如 `/api/rag/search`），通过 OpenAPI 文档管理多版本。

未来如需 Breaking Change：
1. 在 OpenAPI 文档中新增 `info.version = "v2"`
2. 新增 `@Tag(name = "RAG-检索-v2")`
3. 旧版本标记 `@Deprecated`

---

## 10. 与 kin-tree-cloud 的差异说明

| 项目 | kin-tree-cloud | knowledge-dao |
|------|---------------|---------------|
| 框架 | Spring Boot | JDK HttpServer（轻量自研） |
| 认证 | JWT + 微信 OAuth | 预留 JWT（当前免认证） |
| 路径前缀 | `/api/miniapp/`、`/api/admin/` | `/api/rag/`、`/api/admin/` |
| 分页响应 | `{data: [], total, page, pageSize}` | 同左（已对齐） |
| 错误码 | ResultEnum 集中管理 | 同左（已对齐） |
| 向量检索 | — | pgvector + Ollama（bge-m3） |

**对齐项：**
- ✅ 响应包装结构 `Result< T>`（code/message/data/time）
- ✅ 错误码体系（ResultEnum）
- ✅ URL 路径语义（模块前缀 + 资源 + REST 动作）
- ✅ OpenAPI 注解风格
- ✅ DTO 命名规范
