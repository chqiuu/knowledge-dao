# 数据库设计 - 后台管理扩展

> 本文档描述 knowledge-dao 后台管理模块的完整数据库设计。
> 基于 PostgreSQL 15 + pgvector，兼容现有 `schema.sql`。
>
> 参考风格：kin-tree-cloud 数据库设计（UUID 主键 / 软删除 / 审计字段 / 索引覆盖）

---

## 目录

- [1. 扩展概述](#1-扩展概述)
- [2. 现有表扩展](#2-现有表扩展)
- [3. 新增表](#3-新增表)
- [4. 索引设计](#4-索引设计)
- [5. 数据保留策略](#5-数据保留策略)
- [6. DDL 执行指南](#6-ddl-执行指南)

---

## 1. 扩展概述

### 1.1 设计目标

| 目标 | 说明 |
|------|------|
| 可审计 | 所有写操作可追溯到人、时间、IP |
| 可观测 | API 性能、错误率、检索质量可量化监控 |
| 可管理 | 支持分类/标签管理、配置中心、管理员体系 |
| 可扩展 | 新增表结构不影响现有业务 |

### 1.2 五大模块

| 模块 | 说明 |
|------|------|
| 知识管理 | 分类、标签、知识条目（含 summary/status/category） |
| 用户会话 | chat_messages 扩展（summary/sources/feedback） |
| 检索日志 | search_retrieval_logs 记录每次检索详情 |
| 系统监控 | api_metrics + operation_logs 双轨审计 |
| 配置中心 | system_config 存储系统级配置项 |

### 1.3 新增对象一览

| 对象 | 类型 | 用途 |
|------|------|------|
| `kb_categories` | 表 | 知识分类（树形层级） |
| `kb_tags` | 表 | 知识标签（全局复用） |
| `kb_knowledge_entries` | 表 | 知识条目（含 summary/category_id/ref_count） |
| `kb_knowledge_tags` | 表 | 知识-标签多对多关联 |
| `search_retrieval_logs` | 表 | 检索日志（查询/结果/评分） |
| `system_config` | 表 | 系统配置（KV + 变更历史） |
| `operation_logs` | 表 | 知识库写操作审计日志 |
| `api_metrics` | 表 | API 请求性能指标 |
| `admin_users` | 表 | 后台管理员账户 |
| `admin_operation_log` | 表 | 管理员自身操作审计 |
| `cleanup_old_logs()` | 函数 | 清理过期操作日志 |
| `cleanup_old_api_metrics()` | 函数 | 清理过期 API 指标 |

---

## 2. 现有表扩展

### 2.1 knowledge_base 扩展

```sql
ALTER TABLE knowledge_base ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'active';
ALTER TABLE knowledge_base ADD COLUMN IF NOT EXISTS summary TEXT;
ALTER TABLE knowledge_base ADD COLUMN IF NOT EXISTS category_id BIGINT;
ALTER TABLE knowledge_base ADD COLUMN IF NOT EXISTS view_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE knowledge_base ADD COLUMN IF NOT EXISTS like_count INTEGER NOT NULL DEFAULT 0;
```

| 新增字段 | 类型 | 说明 |
|---------|------|------|
| `status` | VARCHAR(20) | 状态：active / inactive / deleted |
| `summary` | TEXT | AI 生成的摘要/概要 |
| `category_id` | BIGINT | 关联分类（外键 kb_categories.id） |
| `view_count` | INTEGER | 浏览次数（后台统计用） |
| `like_count` | INTEGER | 点赞次数 |

### 2.2 chat_messages 扩展

```sql
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'active';
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS summary TEXT;
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS sources JSONB;
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS feedback SMALLINT;
```

| 新增字段 | 类型 | 说明 |
|---------|------|------|
| `status` | VARCHAR(20) | 状态：active / deleted |
| `summary` | TEXT | 消息摘要（如 bot 回复摘要） |
| `sources` | JSONB | 检索来源 `[{id, title, score}]` |
| `feedback` | SMALLINT | 用户反馈：1=满意，-1=不满意，NULL=未反馈 |

### 2.3 shared_knowledge 扩展

```sql
ALTER TABLE shared_knowledge ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'active';
ALTER TABLE shared_knowledge ADD COLUMN IF NOT EXISTS summary TEXT;
```

---

## 3. 新增表

### 3.1 kb_categories（知识分类）

树形分类表，参考 kin-tree-cloud 的 `sys_tag` 设计风格。

```sql
CREATE TABLE IF NOT EXISTS kb_categories (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
        -- 分类名称，如"技术文档"、"读书笔记"
    parent_id       BIGINT,
        -- 上级分类 ID，NULL 表示顶级分类
    sort            INTEGER NOT NULL DEFAULT 0,
        -- 排序权重，数字越小越靠前
    level           INTEGER NOT NULL DEFAULT 1,
        -- 层级深度：1=一级，2=二级，...
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
        -- 是否启用（禁用后该分类不可选，但不影响已有数据）
    description     VARCHAR(500),
        -- 分类描述
    icon            VARCHAR(100),
        -- 分类图标（如 emoji 或 icon class）
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 层级查询索引（支持递归 CTE）
CREATE INDEX IF NOT EXISTS idx_kb_categories_parent
    ON kb_categories(parent_id);

CREATE INDEX IF NOT EXISTS idx_kb_categories_sort
    ON kb_categories(sort, id);

CREATE INDEX IF NOT EXISTS idx_kb_categories_active
    ON kb_categories(is_active, sort)
    WHERE is_active = TRUE;
```

> 使用递归 CTE 查询整棵分类树：
> ```sql
> WITH RECURSIVE cat_tree AS (
>   SELECT * FROM kb_categories WHERE parent_id IS NULL
>   UNION ALL
>   SELECT c.* FROM kb_categories c JOIN cat_tree t ON c.parent_id = t.id
> ) SELECT * FROM cat_tree;
> ```

### 3.2 kb_tags（知识标签）

全局标签表，参考 kin-tree-cloud 的 `sys_tag` 风格。

```sql
CREATE TABLE IF NOT EXISTS kb_tags (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(50) NOT NULL UNIQUE,
        -- 标签名称，唯一
    color           VARCHAR(7),
        -- 标签颜色，如"#FF6B6B"（HEX）
    tag_type        VARCHAR(20) NOT NULL DEFAULT 'knowledge',
        -- 标签类型：knowledge（知识）/ system（系统预置）
    usage_count     INTEGER NOT NULL DEFAULT 0,
        -- 被引用次数，后台维护
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_kb_tags_type_active
    ON kb_tags(tag_type, is_active)
    WHERE is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_kb_tags_usage
    ON kb_tags(usage_count DESC, id)
    WHERE is_active = TRUE;
```

### 3.3 kb_knowledge_entries（知识条目扩展表）

在现有 `knowledge_base` 基础上，补充后台管理所需的扩展字段。
> 采用继承表设计：扩展字段放在此表，通过 `knowledge_base.id = kb_knowledge_entries.id` 关联。

```sql
CREATE TABLE IF NOT EXISTS kb_knowledge_entries (
    id                  BIGSERIAL PRIMARY KEY,
    knowledge_id        BIGINT NOT NULL UNIQUE,
        -- 关联 knowledge_base.id，外键
    category_id         BIGINT,
        -- 关联 kb_categories.id
    summary             TEXT,
        -- AI 生成的摘要
    status              VARCHAR(20) NOT NULL DEFAULT 'active',
        -- 知识状态：active / inactive / deleted（软删除）
    view_count          INTEGER NOT NULL DEFAULT 0,
        -- 浏览次数
    like_count          INTEGER NOT NULL DEFAULT 0,
        -- 点赞次数
    dislike_count       INTEGER NOT NULL DEFAULT 0,
        -- 点踩次数
    source_url          VARCHAR(1000),
        -- 原始来源 URL
    source_domain       VARCHAR(255),
        -- 来源域名（用于过滤）
    author              VARCHAR(255),
        -- 作者/来源
    language            VARCHAR(10) DEFAULT 'zh',
        -- 内容语言：zh / en / ...
    word_count          INTEGER,
        -- 正文字数（不含标题）
    reading_time_minutes INTEGER,
        -- 预估阅读时间（分钟）
    published_at        TIMESTAMP,
        -- 发布时间（内容原始发布时间）
    archived_at         TIMESTAMP,
        -- 归档时间（转入归档库的时间）
    archive_reason      VARCHAR(100),
        -- 归档原因
    expires_at         TIMESTAMP,
        -- 过期时间（NULL=永不过期）
    metadata            JSONB,
        -- 扩展元数据（灵活字段）
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_kb_entries_knowledge
        FOREIGN KEY (knowledge_id) REFERENCES knowledge_base(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_kb_entries_category
        FOREIGN KEY (category_id) REFERENCES kb_categories(id)
        ON DELETE SET NULL
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_kb_entries_category
    ON kb_knowledge_entries(category_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_kb_entries_status_created
    ON kb_knowledge_entries(status, created_at DESC)
    WHERE status != 'deleted';

CREATE INDEX IF NOT EXISTS idx_kb_entries_knowledge
    ON kb_knowledge_entries(knowledge_id);

CREATE INDEX IF NOT EXISTS idx_kb_entries_published
    ON kb_knowledge_entries(published_at DESC)
    WHERE published_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_kb_entries_expires
    ON kb_knowledge_entries(expires_at)
    WHERE expires_at IS NOT NULL AND status != 'deleted';
```

### 3.4 kb_knowledge_tags（知识-标签关联表）

多对多关联，参考 kin-tree-cloud 的 `sys_tag_relation` 设计。

```sql
CREATE TABLE IF NOT EXISTS kb_knowledge_tags (
    id              BIGSERIAL PRIMARY KEY,
    knowledge_id    BIGINT NOT NULL,
        -- 关联 knowledge_base.id
    tag_id          BIGINT NOT NULL,
        -- 关联 kb_tags.id
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_kt_knowledge
        FOREIGN KEY (knowledge_id) REFERENCES knowledge_base(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_kt_tag
        FOREIGN KEY (tag_id) REFERENCES kb_tags(id)
        ON DELETE CASCADE,
    CONSTRAINT uk_kt_knowledge_tag
        UNIQUE (knowledge_id, tag_id)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_kt_tag
    ON kb_knowledge_tags(tag_id, knowledge_id);

CREATE INDEX IF NOT EXISTS idx_kt_knowledge
    ON kb_knowledge_tags(knowledge_id, tag_id);
```

### 3.5 search_retrieval_logs（检索日志）

记录每次 RAG 检索的完整上下文，用于质量分析和优化。

```sql
CREATE TABLE IF NOT EXISTS search_retrieval_logs (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    session_key         VARCHAR(255),
        -- 所属会话（可选，用于关联 chat_messages）
    query_text         TEXT NOT NULL,
        -- 用户查询原文
    query_embedding     VECTOR(1024),
        -- 查询向量（用于分析召回质量）
    top_k               INTEGER NOT NULL DEFAULT 5,
        -- 检索TopK参数
    recall_count        INTEGER NOT NULL DEFAULT 0,
        -- 实际召回文档数
    retrieval_results   JSONB,
        -- 检索结果 `[{knowledge_id, title, score, rank}]`
    avg_relevance_score REAL,
        -- 平均相关性评分（0~1）
    min_relevance_score REAL,
        -- 最低相关性评分（用于判断是否低于阈值）
    rerank_enabled      BOOLEAN NOT NULL DEFAULT FALSE,
        -- 是否启用了 rerank
    rerank_model        VARCHAR(100),
        -- rerank 模型名称
    filter_conditions   JSONB,
        -- 过滤条件（category/tags/status 等）
    total_time_ms       INTEGER NOT NULL DEFAULT 0,
        -- 检索总耗时（毫秒）
    embed_time_ms       INTEGER,
        -- embedding 耗时
    search_time_ms      INTEGER,
        -- 向量检索耗时
    rerank_time_ms      INTEGER,
        -- rerank 耗时（如果启用）
    api_version         VARCHAR(20),
        -- API 版本
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 向量索引：用于分析召回质量（可选，仅在需要按向量分析时启用）
-- CREATE INDEX IF NOT EXISTS idx_srl_query_embedding
--     ON search_retrieval_logs USING hnsw (query_embedding vector_cosine_ops);

-- 查询索引
CREATE INDEX IF NOT EXISTS idx_srl_user_created
    ON search_retrieval_logs(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_srl_session
    ON search_retrieval_logs(session_key, created_at DESC)
    WHERE session_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_srl_created
    ON search_retrieval_logs(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_srl_avg_score
    ON search_retrieval_logs(avg_relevance_score DESC, created_at DESC)
    WHERE avg_relevance_score IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_srl_total_time
    ON search_retrieval_logs(total_time_ms DESC, created_at DESC)
    WHERE total_time_ms > 500;

COMMENT ON TABLE search_retrieval_logs IS
    'RAG检索日志，记录每次检索的查询、召回结果和相关性评分，用于质量监控和优化';
```

### 3.6 system_config（系统配置中心）

存储系统级配置项，支持变更历史审计。

```sql
CREATE TABLE IF NOT EXISTS system_config (
    id              BIGSERIAL PRIMARY KEY,
    config_key      VARCHAR(100) NOT NULL UNIQUE,
        -- 配置键：embedding_model / vector_dimension / top_k / ...
    config_value    TEXT NOT NULL,
        -- 配置值（JSON 字符串，支持复杂类型）
    config_type     VARCHAR(20) NOT NULL DEFAULT 'string',
        -- 值类型：string / number / boolean / json
    default_value   TEXT,
        -- 默认值
    description     VARCHAR(500),
        -- 配置说明
    valid_values    JSONB,
        -- 合法值列表（可选约束：`["bge-m3", "text-embedding-3"]`）
    is_secret       BOOLEAN NOT NULL DEFAULT FALSE,
        -- 是否敏感（密码等，查询时需脱敏）
    is_system       BOOLEAN NOT NULL DEFAULT TRUE,
        -- 是否系统级配置（FALSE=可由管理员修改，TRUE=仅超级管理员）
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
        -- 是否启用（禁用后使用 default_value）
    updated_by      BIGINT,
        -- 最后修改人（admin_users.id）
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sc_key
    ON system_config(config_key);

CREATE INDEX IF NOT EXISTS idx_sc_active
    ON system_config(is_active, config_key)
    WHERE is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_sc_system
    ON system_config(is_system, is_active)
    WHERE is_active = TRUE;
```

**初始配置数据示例：**

```sql
INSERT INTO system_config (config_key, config_value, config_type, default_value, description, is_system) VALUES
    ('embedding_model', '"bge-m3"', 'string', '"bge-m3"', 'Embedding 模型名称', TRUE),
    ('vector_dimension', '1024', 'number', '1024', '向量维度', TRUE),
    ('top_k', '5', 'number', '5', '默认 TopK 检索数量', FALSE),
    ('similarity_threshold', '0.5', 'number', '0.5', '相似度阈值（低于此值不返回）', FALSE),
    ('max_token_per_doc', '2000', 'number', '2000', '每个文档最大 token 数', FALSE),
    ('rerank_enabled', 'false', 'boolean', 'false', '是否启用 rerank', FALSE),
    ('rerank_model', '"bge-reranker-base"', 'string', '"bge-reranker-base"', 'Rerank 模型', FALSE),
    ('chunk_size', '500', 'number', '500', '文档分块大小（字符数）', FALSE),
    ('chunk_overlap', '50', 'number', '50', '分块重叠字符数', FALSE),
    ('api_rate_limit', '100', 'number', '100', 'API 每分钟调用上限', FALSE),
    ('log_retention_days', '90', 'number', '90', '操作日志保留天数', FALSE),
    ('metrics_retention_days', '30', 'number', '30', 'API指标保留天数', FALSE);
```

### 3.7 operation_logs（操作日志）

记录所有针对知识库的写操作。

```sql
CREATE TABLE IF NOT EXISTS operation_logs (
    id              BIGSERIAL PRIMARY KEY,
    action          VARCHAR(50) NOT NULL,
        -- create / update / delete / share / unshare / restore / export / feedback
    target_type     VARCHAR(50) NOT NULL,
        -- knowledge_entry / chat_message / knowledge_tag / category / ...
    target_id       BIGINT,
    target_title    VARCHAR(500),
    operator_id     BIGINT NOT NULL,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    request_method  VARCHAR(10),
    request_path    VARCHAR(500),
    request_params  JSONB,
    response_status SMALLINT,
    error_message   TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_operation_logs_action_created
    ON operation_logs(action, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_operation_logs_target
    ON operation_logs(target_type, target_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_operation_logs_operator
    ON operation_logs(operator_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_operation_logs_created
    ON operation_logs(created_at DESC);
```

### 3.8 api_metrics（API 指标）

记录每个 API 请求的性能数据。

```sql
CREATE TABLE IF NOT EXISTS api_metrics (
    id              BIGSERIAL PRIMARY KEY,
    endpoint        VARCHAR(255) NOT NULL,
    method          VARCHAR(10) NOT NULL,
    status_code     SMALLINT NOT NULL,
    latency_ms      INTEGER NOT NULL,
    user_id         BIGINT,
    ip_address      VARCHAR(45),
    error_type      VARCHAR(50),
    request_size    INTEGER,
    response_size   INTEGER,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_api_metrics_endpoint_created
    ON api_metrics(endpoint, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_api_metrics_user_created
    ON api_metrics(user_id, created_at DESC)
    WHERE user_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_api_metrics_status_created
    ON api_metrics(status_code, created_at DESC)
    WHERE status_code >= 400;

CREATE INDEX IF NOT EXISTS idx_api_metrics_latency
    ON api_metrics(latency_ms DESC, created_at DESC)
    WHERE latency_ms > 1000;

CREATE INDEX IF NOT EXISTS idx_api_metrics_created
    ON api_metrics(created_at DESC);
```

### 3.9 admin_users（管理员账户）

独立的管理员认证体系，与业务 user_id 分离。

```sql
CREATE TABLE IF NOT EXISTS admin_users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(100) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(50) NOT NULL DEFAULT 'viewer',
        -- super_admin / admin / viewer
    nickname        VARCHAR(100),
    email           VARCHAR(255) UNIQUE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMP,
    last_login_ip   VARCHAR(45),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_admin_users_username
    ON admin_users(username);

CREATE INDEX IF NOT EXISTS idx_admin_users_role_active
    ON admin_users(role, is_active);
```

### 3.10 admin_operation_log（管理员审计）

记录管理员自身的管理操作。

```sql
CREATE TABLE IF NOT EXISTS admin_operation_log (
    id              BIGSERIAL PRIMARY KEY,
    admin_id        BIGINT NOT NULL,
    action          VARCHAR(100) NOT NULL,
        -- login / logout / create_knowledge / delete_knowledge / change_config / ...
    target_type     VARCHAR(50),
    target_id       BIGINT,
    detail          JSONB,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_admin_op_admin_created
    ON admin_operation_log(admin_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_admin_op_action_created
    ON admin_operation_log(action, created_at DESC);
```

---

## 4. 索引设计

### 4.1 索引总览

| 表 | 索引名 | 字段 | 类型 | 用途 |
|----|--------|------|------|------|
| `knowledge_base` | `idx_kb_user_status_created` | (user_id, status, created_at DESC) | B-tree | 用户+状态过滤+时间排序 |
| `knowledge_base` | `idx_kb_embedding` | (embedding) | HNSW | 向量检索 |
| `chat_messages` | `idx_cm_session_created` | (session_key, user_id, created_at DESC) | B-tree | 会话历史查询 |
| `chat_messages` | `idx_cm_user_created_status` | (user_id, created_at DESC) | B-tree | 用户消息列表 |
| `kb_categories` | `idx_kb_cat_parent` | (parent_id) | B-tree | 树形查询 |
| `kb_categories` | `idx_kb_cat_sort` | (sort, id) | B-tree | 排序展示 |
| `kb_tags` | `idx_kb_tags_type_active` | (tag_type, is_active) | B-tree | 标签列表 |
| `kb_knowledge_entries` | `idx_kb_entries_category` | (category_id, status, created_at DESC) | B-tree | 分类下知识列表 |
| `kb_knowledge_entries` | `idx_kb_entries_status_created` | (status, created_at DESC) | Partial | 软删除过滤 |
| `kb_knowledge_tags` | `idx_kt_tag` | (tag_id, knowledge_id) | B-tree | 按标签查知识 |
| `search_retrieval_logs` | `idx_srl_user_created` | (user_id, created_at DESC) | B-tree | 用户检索历史 |
| `search_retrieval_logs` | `idx_srl_session` | (session_key, created_at DESC) | Partial | 会话检索历史 |
| `search_retrieval_logs` | `idx_srl_avg_score` | (avg_relevance_score DESC) | Partial | 低质量检索分析 |
| `search_retrieval_logs` | `idx_srl_total_time` | (total_time_ms DESC) | Partial | 慢检索分析 |
| `system_config` | `idx_sc_key` | (config_key) | B-tree | 配置查询 |
| `operation_logs` | `idx_ol_action_created` | (action, created_at DESC) | B-tree | 操作类型统计 |
| `operation_logs` | `idx_ol_target` | (target_type, target_id, created_at DESC) | B-tree | 单条记录操作历史 |
| `operation_logs` | `idx_ol_operator` | (operator_id, created_at DESC) | B-tree | 管理员操作记录 |
| `api_metrics` | `idx_am_endpoint_created` | (endpoint, created_at DESC) | B-tree | 接口性能统计 |
| `api_metrics` | `idx_am_user_created` | (user_id, created_at DESC) | Partial | 用户调用统计 |
| `api_metrics` | `idx_am_status_created` | (status_code, created_at DESC) | Partial | 错误率监控 |
| `api_metrics` | `idx_am_latency` | (latency_ms DESC) | Partial | 慢查询定位 |
| `admin_users` | `idx_au_username` | (username) | B-tree | 登录查询 |
| `admin_operation_log` | `idx_aol_admin_created` | (admin_id, created_at DESC) | B-tree | 管理员操作历史 |

### 4.2 Partial Index（软删除过滤）

仅索引未删除记录，减少索引体积：

```sql
-- 知识库软删除过滤
CREATE INDEX idx_knowledge_base_user_status_created
    ON knowledge_base(user_id, status, created_at DESC)
    WHERE status != 'deleted';

-- 聊天记录软删除过滤
CREATE INDEX idx_chat_messages_session_created
    ON chat_messages(session_key, user_id, created_at DESC)
    WHERE status != 'deleted';

-- 知识条目软删除过滤
CREATE INDEX idx_kb_entries_status_created
    ON kb_knowledge_entries(status, created_at DESC)
    WHERE status != 'deleted';

-- API 错误请求
CREATE INDEX idx_api_metrics_status_created
    ON api_metrics(status_code, created_at DESC)
    WHERE status_code >= 400;

-- API 慢查询（>1s）
CREATE INDEX idx_api_metrics_latency
    ON api_metrics(latency_ms DESC, created_at DESC)
    WHERE latency_ms > 1000;

-- 检索慢查询（>500ms）
CREATE INDEX idx_srl_total_time
    ON search_retrieval_logs(total_time_ms DESC, created_at DESC)
    WHERE total_time_ms > 500;
```

---

## 5. 数据保留策略

### 5.1 保留周期

| 数据类型 | 默认保留 | 说明 |
|---------|---------|------|
| `search_retrieval_logs` | 90 天 | 检索日志分析 |
| `operation_logs` | 90 天 | 操作审计 |
| `api_metrics` | 30 天 | 性能监控 |
| `admin_operation_log` | 永久 | 管理员操作需长期保留 |
| `chat_messages` | 用户可配置 | 默认永久 |
| `knowledge_base` | 永久 | 物理删除需管理员操作 |
| `system_config` | 永久 | 配置变更需历史可查 |

### 5.2 清理函数

```sql
-- 清理 N 天前的操作日志
CREATE OR REPLACE FUNCTION cleanup_old_logs(keep_days INTEGER DEFAULT 90)
RETURNS INTEGER AS $$
DECLARE deleted_count INTEGER;
BEGIN
    DELETE FROM operation_logs
    WHERE created_at < NOW() - (keep_days || ' days')::INTERVAL;
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- 清理 N 天前的 API 指标
CREATE OR REPLACE FUNCTION cleanup_old_api_metrics(keep_days INTEGER DEFAULT 30)
RETURNS INTEGER AS $$
DECLARE deleted_count INTEGER;
BEGIN
    DELETE FROM api_metrics
    WHERE created_at < NOW() - (keep_days || ' days')::INTERVAL;
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- 清理 N 天前的检索日志
CREATE OR REPLACE FUNCTION cleanup_old_retrieval_logs(keep_days INTEGER DEFAULT 90)
RETURNS INTEGER AS $$
DECLARE deleted_count INTEGER;
BEGIN
    DELETE FROM search_retrieval_logs
    WHERE created_at < NOW() - (keep_days || ' days')::INTERVAL;
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;
```

---

## 6. DDL 执行指南

完整 DDL 脚本位于：`doc/admin-ddl.sql`

**执行顺序（幂等，可重复执行）：**

```
1. CREATE EXTENSION IF NOT EXISTS vector;
2. ALTER TABLE knowledge_base (扩展字段)
3. ALTER TABLE chat_messages (扩展字段)
4. ALTER TABLE shared_knowledge (扩展字段)
5. CREATE TABLE kb_categories
6. CREATE TABLE kb_tags
7. CREATE TABLE kb_knowledge_entries
8. CREATE TABLE kb_knowledge_tags
9. CREATE TABLE search_retrieval_logs
10. CREATE TABLE system_config
11. CREATE TABLE operation_logs
12. CREATE TABLE api_metrics
13. CREATE TABLE admin_users
14. CREATE TABLE admin_operation_log
15. CREATE INDEX（所有索引）
16. CREATE FUNCTION（清理函数）
17. INSERT 初始配置数据
```

> ⚠️ 首次执行前请先备份数据库。
> ⚠️ 所有 `CREATE TABLE IF NOT EXISTS` 和 `CREATE INDEX IF NOT EXISTS` 均为幂等设计，可安全重复执行。
