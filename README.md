# knowledge-dao

本地 RAG 知识库系统 — 基于 PostgreSQL + pgvector + Ollama 的私有化知识检索方案。

## 项目结构

```
knowledge-dao/
├── knowledge-dao-admin/    # Vue 3 管理后台
└── knowledge-dao-server/   # Java 后端服务
```

## 功能特性

- **RAG 检索**：支持长文档向量化存储与语义检索
- **知识管理**：知识的增删改查、分类标签管理、批量导入
- **用户会话**：用户提问历史与 AI 回复记录
- **检索日志**：完整的检索链路追踪（embedding 耗时 / 检索耗时 / 重排耗时）
- **系统监控**：API 调用统计、响应时间、错误率监控
- **配置中心**：Embedding 模型、向量维度、TopK 等参数可配置

## 技术栈

### 后端
- Java（Spring Boot）
- PostgreSQL + pgvector（向量数据库）
- Ollama（本地 Embedding 模型）
- MyBatis（持久层）

### 前端
- Vue 3 + Vite
- Element Plus
- Axios

## 快速启动

### 后端

```bash
cd knowledge-dao-server
# 配置数据库连接（src/main/resources/config.yaml）
mvn package -DskipTests
java -cp target/knowledge-dao-2.0.0.jar com.knowledge.KnowledgeDaoApplication
```

服务启动后访问：http://localhost:8081

### 前端

```bash
cd knowledge-dao-admin
npm install
npm run dev
```

前端访问：http://localhost:5173

## API 文档

详细接口规范见 [doc/openapi.yaml](knowledge-dao-server/doc/openapi.yaml)，可在 [Swagger Editor](https://editor.swagger.io/) 中预览。

## 数据库设计

DDL 脚本位于 `knowledge-dao-server/doc/admin-ddl.sql`，包含：

- 10 张新表
- 41 个索引（含 HNSW 向量索引）
- 3 个自动清理函数

## 文档

| 文档 | 说明 |
|------|------|
| `doc/api-style-guide.md` | API 风格规范 |
| `doc/openapi.yaml` | OpenAPI 3.0 完整规范 |
| `doc/database-design-admin.md` | 后台管理数据库设计 |
| `doc/admin-ddl.sql` | 完整 DDL 脚本（幂等） |

## License

MIT
