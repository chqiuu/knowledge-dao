# Rag-Admin 管理后台 API 规范

> 版本: 1.0.0
> 日期: 2026-03-21
> 基础路径: `/api/admin`
> 技术栈: JDK HttpServer (Java 17+) + PostgreSQL + pgvector

---

## 概述

rag-admin 是 knowledge-dao 的后台管理系统，提供完整的管理功能，包括驾驶舱、知识管理、检索中心、用户会话管理、系统监控和配置管理。

所有 API 遵循统一响应格式，使用 JWT Bearer Token 认证（预留），当前版本先做基础实现。

---

## 统一响应格式

### 成功响应

```json
{
  "success": true,
  "code": 200,
  "message": "操作成功",
  "data": { ... },
  "timestamp": 1742544000000
}
```

### 错误响应

```json
{
  "success": false,
  "code": 400,
  "message": "请求参数错误：title 不能为空",
  "data": null,
  "timestamp": 1742544000000
}
```

### HTTP 状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 201 | 创建成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## API 列表

### 模块一：驾驶舱 (Dashboard)

#### 1.1 获取系统概览

```
GET /api/admin/dashboard/overview
```

**描述**: 返回系统全局统计数据

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "message": "success",
  "data": {
    "totalUsers": 1250,
    "totalKnowledgeEntries": 38420,
    "totalSearches": 156780,
    "totalChatMessages": 892340,
    "vectorStorageSizeMB": 2048.5,
    "activeUsersToday": 342,
    "searchesToday": 4521,
    "avgResponseTimeMs": 127
  },
  "timestamp": 1742544000000
}
```

---

#### 1.2 获取知识趋势

```
GET /api/admin/dashboard/knowledge-trend?days=30
```

**参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| days | int | 否 | 统计天数，默认 30，最大 90 |

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "data": [
    { "date": "2026-02-20", "newEntries": 45, "searches": 1203 },
    { "date": "2026-02-21", "newEntries": 38, "searches": 1156 }
  ]
}
```

---

#### 1.3 获取活跃用户排行

```
GET /api/admin/dashboard/top-users?limit=10
```

**参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| limit | int | 否 | 返回数量，默认 10 |

---

### 模块二：知识管理 (Knowledge)

#### 2.1 知识条目列表

```
GET /api/admin/knowledge?page=1&size=20&userId=&contentType=&tag=
```

**参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页数量，默认 20，最大 100 |
| userId | long | 否 | 按用户筛选 |
| contentType | string | 否 | 内容类型筛选 |
| tag | string | 否 | 标签筛选 |
| keyword | string | 否 | 关键词搜索（标题/内容） |
| sort | string | 否 | 排序：created_at, updated_at, relevance，默认 created_at |
| order | string | 否 | 排序方向：desc, asc，默认 desc |

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "data": {
    "items": [
      {
        "id": 1,
        "title": "Java 核心技术",
        "content": "Java 是一门面向对象...",
        "contentType": "article",
        "tags": ["java", "programming"],
        "source": "blog",
        "userId": 100,
        "isShared": false,
        "createdAt": "2026-03-01T10:00:00Z",
        "updatedAt": "2026-03-15T14:30:00Z"
      }
    ],
    "total": 38420,
    "page": 1,
    "size": 20,
    "pages": 1921
  }
}
```

---

#### 2.2 知识条目详情

```
GET /api/admin/knowledge/{id}
```

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 知识条目 ID |

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "data": {
    "id": 1,
    "title": "Java 核心技术",
    "content": "Java 是一门面向对象编程语言...",
    "contentType": "article",
    "tags": ["java", "programming"],
    "source": "blog",
    "embedding": [0.123, -0.456, ...],
    "userId": 100,
    "isShared": false,
    "createdAt": "2026-03-01T10:00:00Z",
    "updatedAt": "2026-03-15T14:30:00Z"
  }
}
```

---

#### 2.3 创建知识条目

```
POST /api/admin/knowledge
Content-Type: application/json
```

**请求体**:
```json
{
  "title": "Python 入门教程",
  "content": "Python 是一门易学难精的语言...",
  "contentType": "tutorial",
  "tags": ["python", "programming", "beginner"],
  "source": "manual",
  "userId": 100,
  "isShared": false
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 是 | 标题，最大 500 字符 |
| content | string | 是 | 正文内容，最大 100 万字符 |
| contentType | string | 否 | 内容类型，默认 article |
| tags | string[] | 否 | 标签数组，最大 20 个 |
| source | string | 否 | 来源标识，默认 manual |
| userId | long | 是 | 所属用户 ID |
| isShared | boolean | 否 | 是否共享，默认 false |

**响应**: `201 Created`

---

#### 2.4 批量导入知识

```
POST /api/admin/knowledge/batch
Content-Type: application/json
```

**请求体**:
```json
{
  "entries": [
    {
      "title": "标题1",
      "content": "内容1",
      "contentType": "article",
      "tags": ["tag1"],
      "source": "import"
    },
    {
      "title": "标题2",
      "content": "内容2",
      "contentType": "article",
      "tags": ["tag2"],
      "source": "import"
    }
  ],
  "userId": 100
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| entries | object[] | 是 | 知识条目数组，最大 100 条/次 |
| userId | long | 是 | 导入到的用户 ID |

**响应示例**:
```json
{
  "success": true,
  "code": 201,
  "data": {
    "total": 2,
    "successCount": 2,
    "failedCount": 0,
    "errors": []
  }
}
```

---

#### 2.5 更新知识条目

```
PUT /api/admin/knowledge/{id}
Content-Type: application/json
```

**路径参数**: `id` - 知识条目 ID

**请求体**: 同创建，字段均可选

**响应**: `200 OK`

---

#### 2.6 删除知识条目

```
DELETE /api/admin/knowledge/{id}
```

**路径参数**: `id` - 知识条目 ID

**响应**: `200 OK`

---

#### 2.7 知识条目统计

```
GET /api/admin/knowledge/stats
```

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "data": {
    "totalEntries": 38420,
    "totalUsers": 1250,
    "byContentType": {
      "article": 25100,
      "tutorial": 8320,
      "book": 3200,
      "note": 1800
    },
    "byTag": [
      { "tag": "java", "count": 4200 },
      { "tag": "python", "count": 3800 },
      { "tag": "ai", "count": 2900 }
    ],
    "sharedEntries": 3240,
    "avgContentLength": 4521
  }
}
```

---

### 模块三：检索中心 (Search)

#### 3.1 语义搜索

```
POST /api/admin/search
Content-Type: application/json
```

**请求体**:
```json
{
  "query": "Java 并发编程最佳实践",
  "userId": 100,
  "topK": 10,
  "minSimilarity": 0.6
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| query | string | 是 | 搜索查询文本 |
| userId | long | 是 | 用户 ID |
| topK | int | 否 | 返回数量，默认 10，最大 100 |
| minSimilarity | double | 否 | 最低相似度 [0, 1]，默认 0 |

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "data": {
    "query": "Java 并发编程最佳实践",
    "totalHits": 10,
    "searchTimeMs": 45,
    "results": [
      {
        "id": 1542,
        "title": "Java 并发编程实战",
        "content": "本文介绍 Java 并发编程的核心概念...",
        "contentType": "article",
        "tags": ["java", "concurrency", "multithreading"],
        "source": "book",
        "similarity": 0.8923,
        "userId": 100
      }
    ]
  }
}
```

---

#### 3.2 搜索历史

```
GET /api/admin/search/history?userId=&page=1&size=20
```

**参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | long | 否 | 按用户筛选 |
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页数量，默认 20 |
| startDate | string | 否 | 开始日期，ISO 8601 |
| endDate | string | 否 | 结束日期，ISO 8601 |

**说明**: 搜索历史记录来自 `chat_messages` 表中 role=search 的记录

---

#### 3.3 搜索统计

```
GET /api/admin/search/stats
```

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "data": {
    "totalSearches": 156780,
    "searchesToday": 4521,
    "avgLatencyMs": 127,
    "topQueries": [
      { "query": "Java 入门", "count": 3420 },
      { "query": "Python 教程", "count": 2890 },
      { "query": "AI 发展趋势", "count": 2150 }
    ],
    "zeroResultQueries": 3240,
    "p95LatencyMs": 380,
    "p99LatencyMs": 890
  }
}
```

---

#### 3.4 RAG 对话

```
POST /api/admin/rag/chat
Content-Type: application/json
```

**请求体**:
```json
{
  "query": "Java 多线程有哪些实现方式？",
  "userId": 100,
  "topK": 5,
  "sessionKey": "sess-20260321-001"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| query | string | 是 | 用户查询 |
| userId | long | 是 | 用户 ID |
| topK | int | 否 | 上下文知识条数，默认 5 |
| sessionKey | string | 否 | 会话标识，用于关联对话历史 |

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "data": {
    "query": "Java 多线程有哪些实现方式？",
    "answer": "Java 多线程主要有以下几种实现方式：...",
    "context": "## Relevant Knowledge (Top 5):\n\n### [1] Java 并发编程实战\nJava 提供了多种并发机制...\n\n### [2] Thread 类使用\n...",
    "hits": [
      {
        "id": 1542,
        "title": "Java 并发编程实战",
        "content": "Java 线程实现方式包括继承 Thread...",
        "similarity": 0.8923
      }
    ],
    "hitCount": 5,
    "sessionKey": "sess-20260321-001"
  }
}
```

---

### 模块四：用户与会话 (User & Session)

#### 4.1 用户列表

```
GET /api/admin/users?page=1&size=20
```

**参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页数量，默认 20 |
| keyword | string | 否 | 关键词搜索（用户ID） |

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "data": {
    "items": [
      {
        "userId": 100,
        "totalEntries": 450,
        "totalSearches": 2300,
        "totalMessages": 8900,
        "lastActiveAt": "2026-03-21T10:30:00Z",
        "createdAt": "2026-01-15T08:00:00Z"
      }
    ],
    "total": 1250,
    "page": 1,
    "size": 20
  }
}
```

---

#### 4.2 用户详情

```
GET /api/admin/users/{userId}
```

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "data": {
    "userId": 100,
    "totalEntries": 450,
    "totalSearches": 2300,
    "totalMessages": 8900,
    "lastActiveAt": "2026-03-21T10:30:00Z",
    "createdAt": "2026-01-15T08:00:00Z",
    "contentTypeDistribution": {
      "article": 320,
      "tutorial": 80,
      "book": 50
    },
    "topTags": ["java", "python", "ai"]
  }
}
```

---

#### 4.3 用户会话列表

```
GET /api/admin/users/{userId}/sessions?page=1&size=20
```

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "data": {
    "items": [
      {
        "sessionKey": "sess-20260321-001",
        "messageCount": 24,
        "lastMessageAt": "2026-03-21T10:30:00Z",
        "createdAt": "2026-03-21T09:00:00Z"
      }
    ],
    "total": 45
  }
}
```

---

#### 4.4 会话消息历史

```
GET /api/admin/sessions/{sessionKey}/messages?userId=&page=1&size=50
```

**参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | long | 是 | 用户 ID（安全校验） |
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页数量，默认 50 |

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "data": {
    "items": [
      {
        "id": 1001,
        "role": "user",
        "content": "Java 多线程有哪些实现方式？",
        "createdAt": "2026-03-21T10:00:00Z"
      },
      {
        "id": 1002,
        "role": "assistant",
        "content": "Java 多线程主要有以下几种实现方式...",
        "createdAt": "2026-03-21T10:00:05Z"
      }
    ],
    "total": 24
  }
}
```

---

#### 4.5 删除会话

```
DELETE /api/admin/sessions/{sessionKey}?userId=
```

**参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | long | 是 | 用户 ID（安全校验） |

---

#### 4.6 清理用户历史消息

```
DELETE /api/admin/users/{userId}/messages?days=30
```

**参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| days | int | 否 | 清理多少天前的消息，默认 30 |

---

### 模块五：系统监控 (Monitoring)

#### 5.1 健康检查

```
GET /api/admin/monitor/health
```

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "data": {
    "status": "healthy",
    "version": "1.0.0",
    "uptimeSeconds": 864000,
    "components": {
      "database": "healthy",
      "vectorStore": "healthy",
      "embeddingService": "healthy"
    }
  }
}
```

---

#### 5.2 系统指标

```
GET /api/admin/monitor/metrics
```

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "data": {
    "jvm": {
      "memoryUsedMB": 512,
      "memoryMaxMB": 2048,
      "heapUsedPercent": 25,
      "threads": 42,
      "gcCount": 156
    },
    "database": {
      "activeConnections": 8,
      "idleConnections": 2,
      "maxConnections": 20,
      "queryCountToday": 156780,
      "avgQueryTimeMs": 12.5
    },
    "vectorStore": {
      "totalVectors": 38420000,
      "indexType": "hnsw",
      "embeddingDimension": 1024
    },
    "application": {
      "requestsTotal": 892340,
      "requestsToday": 4521,
      "errorRate": 0.002,
      "avgResponseTimeMs": 127
    }
  }
}
```

---

#### 5.3 数据库状态

```
GET /api/admin/monitor/database
```

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "data": {
    "database": "knowledge-dao",
    "version": "PostgreSQL 16.2",
    "pgvectorVersion": "0.7.0",
    "totalTables": 4,
    "totalIndexes": 9,
    "totalSizeMB": 2048.5,
    "indexesSizeMB": 512.3,
    "tableStats": [
      { "table": "knowledge_base", "rows": 38420, "sizeMB": 1536.2 },
      { "table": "chat_messages", "rows": 892340, "sizeMB": 420.5 },
      { "table": "user_profile", "rows": 12500, "sizeMB": 12.8 },
      { "table": "shared_knowledge", "rows": 3240, "sizeMB": 79.0 }
    ]
  }
}
```

---

#### 5.4 Ollama 状态

```
GET /api/admin/monitor/ollama
```

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "data": {
    "status": "healthy",
    "baseUrl": "http://localhost:11434",
    "embeddingModel": "bge-m3",
    "embeddingDimension": 1024,
    "totalEmbeddingRequests": 156780,
    "avgEmbeddingTimeMs": 85,
    "embeddingFailures": 12
  }
}
```

---

### 模块六：配置中心 (Config)

#### 6.1 获取系统配置

```
GET /api/admin/config
```

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "data": {
    "app": {
      "name": "knowledge-dao",
      "version": "1.0.0",
      "port": 8080
    },
    "database": {
      "host": "localhost",
      "port": 5432,
      "name": "knowledge-dao",
      "poolSize": 10
    },
    "embedding": {
      "provider": "ollama",
      "model": "bge-m3",
      "baseUrl": "http://localhost:11434",
      "dimension": 1024,
      "timeoutSeconds": 120
    },
    "rag": {
      "defaultTopK": 5,
      "maxTopK": 100,
      "minSimilarity": 0.0
    }
  }
}
```

---

#### 6.2 更新系统配置

```
PUT /api/admin/config
Content-Type: application/json
```

**请求体**:
```json
{
  "rag.defaultTopK": 10,
  "rag.minSimilarity": 0.5,
  "embedding.timeoutSeconds": 180
}
```

**说明**: 只支持更新指定字段，不支持更新数据库连接等敏感配置

---

#### 6.3 获取用户配置

```
GET /api/admin/config/user/{userId}
```

---

#### 6.4 设置用户配置

```
PUT /api/admin/config/user/{userId}
Content-Type: application/json
```

**请求体**:
```json
{
  "preferences": {
    "theme": "dark",
    "language": "zh"
  },
  "defaultSearchLimit": 10
}
```

---

#### 6.5 删除用户配置

```
DELETE /api/admin/config/user/{userId}?key=preferences
```

**参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| key | string | 是 | 配置键名 |

---

## 数据模型

### KnowledgeEntry (知识条目)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | long | 主键 |
| title | string | 标题，最大 500 字符 |
| content | string | 正文内容 |
| contentType | string | 内容类型 |
| tags | string[] | 标签数组，JSONB |
| embedding | float[] | 1024 维向量（仅详情接口返回） |
| source | string | 来源标识 |
| userId | long | 所属用户 ID |
| isShared | boolean | 是否共享 |
| createdAt | datetime | 创建时间 |
| updatedAt | datetime | 更新时间 |

### ChatMessage (对话消息)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | long | 主键 |
| sessionKey | string | 会话标识 |
| role | string | 角色：user/assistant/system/search |
| content | string | 消息内容 |
| userId | long | 所属用户 ID |
| createdAt | datetime | 创建时间 |

### UserProfile (用户配置)

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | long | 用户 ID（复合主键） |
| key | string | 配置键（复合主键） |
| value | object | 配置值，JSONB |
| updatedAt | datetime | 更新时间 |

---

## 错误码规范

| 错误码 | 说明 |
|--------|------|
| 1001 | 资源不存在 |
| 1002 | 参数校验失败 |
| 1003 | 认证失败 |
| 1004 | 权限不足 |
| 2001 | 数据库错误 |
| 2002 | 向量服务错误 |
| 2003 | Ollama 服务不可用 |
| 3001 | 知识条目已存在 |
| 3002 | 知识条目不存在 |
| 3003 | 用户不存在 |

---

## 附录：PostgreSQL 表结构（Admin 扩展）

以下为 Admin API 所需的额外表/字段扩展：

```sql
-- 用于记录搜索历史的扩展表（如需独立存储搜索记录）
CREATE TABLE IF NOT EXISTS search_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    query TEXT NOT NULL,
    result_count INT NOT NULL DEFAULT 0,
    latency_ms INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_search_history_user ON search_history(user_id);
CREATE INDEX idx_search_history_created ON search_history(created_at DESC);

-- 用于记录系统指标的扩展表
CREATE TABLE IF NOT EXISTS system_metrics (
    id BIGSERIAL PRIMARY KEY,
    metric_name VARCHAR(100) NOT NULL,
    metric_value JSONB NOT NULL,
    recorded_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_system_metrics_name_recorded ON system_metrics(metric_name, recorded_at DESC);
```

---

## 变更日志

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0.0 | 2026-03-21 | 初始版本，包含驾驶舱、知识管理、检索中心、用户会话、系统监控、配置管理六大模块 |
