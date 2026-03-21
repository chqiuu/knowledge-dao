-- ============================================================
-- Admin Backend DDL for knowledge-dao
-- Target: PostgreSQL 15+ with pgvector extension
-- Version: 2.0（完整版，覆盖5大模块）
-- Description: 后台管理所需扩展表、索引及现有表扩展
-- 模块：知识管理 | 用户会话 | 检索日志 | 系统监控 | 配置中心
-- ============================================================

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================================
-- Part 1: 现有表扩展
-- ============================================================

-- 1.1 knowledge_base 扩展字段
ALTER TABLE knowledge_base ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'active';
ALTER TABLE knowledge_base ADD COLUMN IF NOT EXISTS summary TEXT;
ALTER TABLE knowledge_base ADD COLUMN IF NOT EXISTS category_id BIGINT;
ALTER TABLE knowledge_base ADD COLUMN IF NOT EXISTS view_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE knowledge_base ADD COLUMN IF NOT EXISTS like_count INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN knowledge_base.status IS '知识状态: active(正常), inactive(禁用), deleted(已删除)';
COMMENT ON COLUMN knowledge_base.summary IS 'AI生成的摘要/概要';
COMMENT ON COLUMN knowledge_base.category_id IS '关联分类ID(kb_categories)';
COMMENT ON COLUMN knowledge_base.view_count IS '浏览次数';
COMMENT ON COLUMN knowledge_base.like_count IS '点赞次数';

-- 扩展索引：覆盖 status 过滤条件
CREATE INDEX IF NOT EXISTS idx_knowledge_base_user_status_created
    ON knowledge_base(user_id, status, created_at DESC)
    WHERE status != 'deleted';

CREATE INDEX IF NOT EXISTS idx_knowledge_base_status_created
    ON knowledge_base(status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_knowledge_base_category
    ON knowledge_base(category_id, status, created_at DESC)
    WHERE category_id IS NOT NULL;

-- 1.2 chat_messages 扩展字段
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'active';
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS summary TEXT;
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS sources JSONB;
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS feedback SMALLINT;

COMMENT ON COLUMN chat_messages.status IS '消息状态: active(正常), deleted(已删除)';
COMMENT ON COLUMN chat_messages.summary IS '消息摘要(如bot回复摘要)';
COMMENT ON COLUMN chat_messages.sources IS '检索来源 [{id, title, score}]';
COMMENT ON COLUMN chat_messages.feedback IS '用户反馈: 1=满意, -1=不满意, NULL=未反馈';

-- 扩展索引：支持软删除 + 时间范围查询
CREATE INDEX IF NOT EXISTS idx_chat_messages_session_created
    ON chat_messages(session_key, user_id, created_at DESC)
    WHERE status != 'deleted';

CREATE INDEX IF NOT EXISTS idx_chat_messages_user_created_status
    ON chat_messages(user_id, created_at DESC)
    WHERE status != 'deleted';

-- 1.3 shared_knowledge 扩展字段
ALTER TABLE shared_knowledge ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'active';
ALTER TABLE shared_knowledge ADD COLUMN IF NOT EXISTS summary TEXT;

COMMENT ON COLUMN shared_knowledge.status IS '共享知识状态: active(正常), deleted(已删除)';
COMMENT ON COLUMN shared_knowledge.summary IS 'AI生成的摘要';

CREATE INDEX IF NOT EXISTS idx_shared_knowledge_status_created
    ON shared_knowledge(status, created_at DESC)
    WHERE status != 'deleted';

-- ============================================================
-- Part 2: 知识管理模块
-- ============================================================

-- --------------------------------------------------
-- 2.1 kb_categories（知识分类）
--    树形层级分类，支持多级
-- --------------------------------------------------

CREATE TABLE IF NOT EXISTS kb_categories (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    parent_id       BIGINT,
    sort            INTEGER NOT NULL DEFAULT 0,
    level           INTEGER NOT NULL DEFAULT 1,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    description     VARCHAR(500),
    icon            VARCHAR(100),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_kb_categories_parent
    ON kb_categories(parent_id);

CREATE INDEX IF NOT EXISTS idx_kb_categories_sort
    ON kb_categories(sort, id);

CREATE INDEX IF NOT EXISTS idx_kb_categories_active
    ON kb_categories(is_active, sort)
    WHERE is_active = TRUE;

COMMENT ON TABLE kb_categories IS '知识分类表，支持树形层级结构';

-- --------------------------------------------------
-- 2.2 kb_tags（知识标签）
--    全局标签，支持 knowledge/system 两种类型
-- --------------------------------------------------

CREATE TABLE IF NOT EXISTS kb_tags (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(50) NOT NULL UNIQUE,
    color           VARCHAR(7),
    tag_type        VARCHAR(20) NOT NULL DEFAULT 'knowledge',
    usage_count     INTEGER NOT NULL DEFAULT 0,
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

COMMENT ON TABLE kb_tags IS '全局知识标签表';

-- --------------------------------------------------
-- 2.3 kb_knowledge_entries（知识条目扩展表）
--    继承 knowledge_base，补充后台管理字段
-- --------------------------------------------------

CREATE TABLE IF NOT EXISTS kb_knowledge_entries (
    id                  BIGSERIAL PRIMARY KEY,
    knowledge_id        BIGINT NOT NULL UNIQUE,
    category_id         BIGINT,
    summary             TEXT,
    status              VARCHAR(20) NOT NULL DEFAULT 'active',
    view_count          INTEGER NOT NULL DEFAULT 0,
    like_count          INTEGER NOT NULL DEFAULT 0,
    dislike_count       INTEGER NOT NULL DEFAULT 0,
    source_url          VARCHAR(1000),
    source_domain       VARCHAR(255),
    author              VARCHAR(255),
    language            VARCHAR(10) DEFAULT 'zh',
    word_count          INTEGER,
    reading_time_minutes INTEGER,
    published_at        TIMESTAMP,
    archived_at         TIMESTAMP,
    archive_reason      VARCHAR(100),
    expires_at          TIMESTAMP,
    metadata            JSONB,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_kb_entries_knowledge
        FOREIGN KEY (knowledge_id) REFERENCES knowledge_base(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_kb_entries_category
        FOREIGN KEY (category_id) REFERENCES kb_categories(id)
        ON DELETE SET NULL
);

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

COMMENT ON TABLE kb_knowledge_entries IS '知识条目扩展表，关联knowledge_base补充后台管理字段';

-- --------------------------------------------------
-- 2.4 kb_knowledge_tags（知识-标签多对多关联）
-- --------------------------------------------------

CREATE TABLE IF NOT EXISTS kb_knowledge_tags (
    id              BIGSERIAL PRIMARY KEY,
    knowledge_id    BIGINT NOT NULL,
    tag_id          BIGINT NOT NULL,
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

CREATE INDEX IF NOT EXISTS idx_kt_tag
    ON kb_knowledge_tags(tag_id, knowledge_id);

CREATE INDEX IF NOT EXISTS idx_kt_knowledge
    ON kb_knowledge_tags(knowledge_id, tag_id);

COMMENT ON TABLE kb_knowledge_tags IS '知识-标签多对多关联表';

-- ============================================================
-- Part 3: 检索日志模块
-- ============================================================

-- --------------------------------------------------
-- 3.1 search_retrieval_logs（检索日志）
--    记录每次RAG检索的完整上下文
-- --------------------------------------------------

CREATE TABLE IF NOT EXISTS search_retrieval_logs (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    session_key         VARCHAR(255),
    query_text          TEXT NOT NULL,
    query_embedding     VECTOR(1024),
    top_k               INTEGER NOT NULL DEFAULT 5,
    recall_count        INTEGER NOT NULL DEFAULT 0,
    retrieval_results   JSONB,
    avg_relevance_score REAL,
    min_relevance_score REAL,
    rerank_enabled      BOOLEAN NOT NULL DEFAULT FALSE,
    rerank_model        VARCHAR(100),
    filter_conditions   JSONB,
    total_time_ms       INTEGER NOT NULL DEFAULT 0,
    embed_time_ms       INTEGER,
    search_time_ms      INTEGER,
    rerank_time_ms      INTEGER,
    api_version         VARCHAR(20),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

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

-- ============================================================
-- Part 4: 系统监控模块
-- ============================================================

-- --------------------------------------------------
-- 4.1 operation_logs（操作日志）
--    记录所有针对知识库的写操作
-- --------------------------------------------------

CREATE TABLE IF NOT EXISTS operation_logs (
    id              BIGSERIAL PRIMARY KEY,
    action          VARCHAR(50) NOT NULL,
    target_type     VARCHAR(50) NOT NULL,
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

COMMENT ON TABLE operation_logs IS '后台管理操作日志，记录所有写操作';

-- --------------------------------------------------
-- 4.2 api_metrics（API调用指标）
-- --------------------------------------------------

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

COMMENT ON TABLE api_metrics IS 'API性能指标，用于监控和慢查询分析';

-- ============================================================
-- Part 5: 配置中心
-- ============================================================

-- --------------------------------------------------
-- 5.1 system_config（系统配置）
-- --------------------------------------------------

CREATE TABLE IF NOT EXISTS system_config (
    id              BIGSERIAL PRIMARY KEY,
    config_key      VARCHAR(100) NOT NULL UNIQUE,
    config_value    TEXT NOT NULL,
    config_type     VARCHAR(20) NOT NULL DEFAULT 'string',
    default_value   TEXT,
    description     VARCHAR(500),
    valid_values    JSONB,
    is_secret       BOOLEAN NOT NULL DEFAULT FALSE,
    is_system       BOOLEAN NOT NULL DEFAULT TRUE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    updated_by      BIGINT,
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

COMMENT ON TABLE system_config IS '系统配置中心，存储系统级配置项';

-- 初始配置数据（幂等：ON CONFLICT DO NOTHING）
INSERT INTO system_config (config_key, config_value, config_type, default_value, description, is_system) VALUES
    ('embedding_model', '"bge-m3"', 'string', '"bge-m3"', 'Embedding模型名称', TRUE),
    ('vector_dimension', '1024', 'number', '1024', '向量维度', TRUE),
    ('top_k', '5', 'number', '5', '默认TopK检索数量', FALSE),
    ('similarity_threshold', '0.5', 'number', '0.5', '相似度阈值（低于此值不返回）', FALSE),
    ('max_token_per_doc', '2000', 'number', '2000', '每个文档最大token数', FALSE),
    ('rerank_enabled', 'false', 'boolean', 'false', '是否启用rerank', FALSE),
    ('rerank_model', '"bge-reranker-base"', 'string', '"bge-reranker-base"', 'Rerank模型', FALSE),
    ('chunk_size', '500', 'number', '500', '文档分块大小（字符数）', FALSE),
    ('chunk_overlap', '50', 'number', '50', '分块重叠字符数', FALSE),
    ('api_rate_limit', '100', 'number', '100', 'API每分钟调用上限', FALSE),
    ('log_retention_days', '90', 'number', '90', '操作日志保留天数', FALSE),
    ('metrics_retention_days', '30', 'number', '30', 'API指标保留天数', FALSE)
ON CONFLICT (config_key) DO NOTHING;

-- ============================================================
-- Part 6: 管理员账户
-- ============================================================

-- --------------------------------------------------
-- 6.1 admin_users（后台管理员账户）
--    独立的管理员认证体系，与业务user_id分离
-- --------------------------------------------------

CREATE TABLE IF NOT EXISTS admin_users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(100) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(50) NOT NULL DEFAULT 'viewer',
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

COMMENT ON TABLE admin_users IS '后台管理员账户';

-- --------------------------------------------------
-- 6.2 admin_operation_log（管理员自身操作审计）
-- --------------------------------------------------

CREATE TABLE IF NOT EXISTS admin_operation_log (
    id              BIGSERIAL PRIMARY KEY,
    admin_id        BIGINT NOT NULL,
    action          VARCHAR(100) NOT NULL,
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

COMMENT ON TABLE admin_operation_log IS '管理员操作审计日志';

-- ============================================================
-- Part 7: user_profile 扩展索引
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_user_profile_key_updated
    ON user_profile(key, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_user_profile_updated
    ON user_profile(updated_at DESC);

-- ============================================================
-- Part 8: 辅助函数
-- ============================================================

-- 清理 N 天前的操作日志
CREATE OR REPLACE FUNCTION cleanup_old_logs(keep_days INTEGER DEFAULT 90)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
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
DECLARE
    deleted_count INTEGER;
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
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM search_retrieval_logs
    WHERE created_at < NOW() - (keep_days || ' days')::INTERVAL;
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;
