-- ============================================================
-- Knowledge Base Schema for PostgreSQL + pgvector
-- Run this to initialize the database
-- ============================================================

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- chat_messages table
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGSERIAL PRIMARY KEY,
    session_key VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    user_id BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_session ON chat_messages(session_key, user_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_user_created ON chat_messages(user_id, created_at);

-- knowledge_base table
CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    content_type VARCHAR(100),
    tags JSONB,
    embedding VECTOR(1024),
    source VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    user_id BIGINT NOT NULL,
    is_shared BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_knowledge_base_user ON knowledge_base(user_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_base_user_created ON knowledge_base(user_id, created_at DESC);
-- HNSW index for efficient vector similarity search
CREATE INDEX IF NOT EXISTS idx_knowledge_base_embedding ON knowledge_base USING hnsw (embedding vector_cosine_ops);

-- user_profile table
CREATE TABLE IF NOT EXISTS user_profile (
    key VARCHAR(255) NOT NULL,
    value JSONB NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    user_id BIGINT NOT NULL,
    PRIMARY KEY (key, user_id)
);

CREATE INDEX IF NOT EXISTS idx_user_profile_user ON user_profile(user_id);

-- shared_knowledge table
CREATE TABLE IF NOT EXISTS shared_knowledge (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    content_type VARCHAR(100),
    tags JSONB,
    embedding VECTOR(1024),
    source VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_shared_knowledge_owner ON shared_knowledge(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_shared_knowledge_embedding ON shared_knowledge USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE chat_messages IS 'Stores conversation history per session';
COMMENT ON TABLE knowledge_base IS 'Main knowledge base with vector embeddings for RAG';
COMMENT ON TABLE user_profile IS 'User preferences and profile data in JSONB';
COMMENT ON TABLE shared_knowledge IS 'Shared knowledge entries across users';
