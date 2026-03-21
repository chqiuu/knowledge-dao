# knowledge-dao

Agent DAO Layer for PostgreSQL Knowledge Base with RAG Support.

基于 PostgreSQL + pgvector 的 RAG 知识库系统，提供向量检索、对话历史管理、用户配置存储能力。

## 技术栈

- **语言**: Java 17
- **数据库**: PostgreSQL 15+ (with pgvector extension)
- **向量模型**: Ollama (bge-m3, 1024 维)
- **连接池**: HikariCP
- **HTTP 框架**: Javalin 6.3.0
- **构建工具**: Maven

## 项目结构

```
knowledge-dao/
├── src/main/java/com/knowledge/
│   ├── App.java                      # 应用入口 + CLI 测试套件
│   ├── config/Config.java            # YAML 配置加载
│   ├── model/
│   │   ├── KnowledgeEntry.java       # 知识条目实体
│   │   ├── KnowledgeSearchResult.java # 搜索结果 DTO
│   │   ├── ChatMessage.java          # 对话消息实体
│   │   └── UserProfile.java          # 用户配置实体
│   ├── service/
│   │   ├── EmbeddingService.java     # Ollama 向量化服务
│   │   └── RagService.java           # RAG 检索编排服务
│   ├── store/
│   │   ├── VectorStore.java          # 向量存储接口抽象
│   │   └── PgvectorStore.java       # pgvector 实现
│   └── dao/
│       ├── KnowledgeDao.java         # 知识库数据访问
│       ├── ChatMessageDao.java       # 对话历史数据访问
│       └── UserProfileDao.java       # 用户配置数据访问
├── schema.sql                        # 数据库初始化脚本
├── pom.xml
└── src/main/resources/config.yaml    # 配置文件
```

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.8+
- PostgreSQL 15+ with pgvector extension
- Ollama 服务 (支持 bge-m3 模型)

### 2. 数据库初始化

```bash
# 创建数据库
createdb knowledge

# 启用 pgvector
psql -d knowledge -c "CREATE EXTENSION IF NOT EXISTS vector;"

# 执行建表脚本
psql -d knowledge -f schema.sql
```

### 3. 启动 Ollama

```bash
# 拉取 bge-m3 模型
ollama pull bge-m3

# 启动服务
ollama serve
```

### 4. 配置

编辑 `src/main/resources/config.yaml`：

```yaml
db:
  host: localhost
  port: 5432
  database: knowledge
  username: dou
  password: chqiuu2026    # 或通过环境变量 DB_PASSWORD 设置

ollama:
  base-url: http://localhost:11434
  embedding-model: bge-m3

app:
  embedding-dimension: 1024
```

### 5. 构建与运行

```bash
mvn clean package -DskipTests
java -jar target/knowledge-dao-1.0.0.jar
```

## 核心模块

| 模块 | 说明 |
|------|------|
| `EmbeddingService` | 调用 Ollama API 生成文本向量，支持单条和批量 |
| `RagService` | RAG 检索编排：语义搜索、阈值过滤、Context 构建 |
| `KnowledgeDao` | 知识库 CRUD + 向量检索（余弦相似度） |
| `ChatMessageDao` | 对话历史管理（按 Session 分组） |
| `UserProfileDao` | 用户配置 KV 存储（JSONB） |
| `VectorStore` | 向量存储接口抽象，支持 PgvectorStore 等实现 |

## License

MIT
