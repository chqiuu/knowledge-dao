# API 文档

> 当前 API 以 Java 类方法形式提供，计划后续通过 Javalin 暴露 HTTP 接口。

---

## EmbeddingService

调用 Ollama 生成文本向量。

**包**: `com.knowledge.service`
**构造**: `new EmbeddingService(Config config)`

---

### embed

```java
public float[] embed(String text) throws IOException, InterruptedException
```

生成单条文本的向量表示。

**参数**:
| 名称 | 类型 | 必填 | 说明 |
|------|------|------|------|
| text | String | 是 | 待向量化的文本 |

**返回值**: `float[]` — 1024 维浮点向量

**异常**:
- `IOException` — Ollama 服务不可用或响应错误
- `InterruptedException` — 请求超时

**示例**:
```java
float[] vec = embeddingService.embed("Java 编程语言");
System.out.println("维度: " + vec.length); // 1024
```

---

### embedBatch

```java
public List<float[]> embedBatch(List<String> texts)
    throws IOException, InterruptedException
```

批量生成向量。单次 HTTP 请求，提升大量文本的向量化效率。

**参数**:
| 名称 | 类型 | 必填 | 说明 |
|------|------|------|------|
| texts | List\<String\> | 是 | 文本列表 |

**返回值**: `List<float[]>` — 对应顺序的向量列表，空文本返回全零向量

---

### cosineSimilarity

```java
public static double cosineSimilarity(float[] a, float[] b)
```

计算两个向量的余弦相似度。

**返回值**: `double` — 范围 [-1, 1]，0 表示正交

---

### toPgVectorString

```java
public static String toPgVectorString(float[] embedding)
```

将 float[] 转为 pgvector SQL 格式 `[v1,v2,...,vn]`。

---

## RagService

RAG 检索编排服务。

**包**: `com.knowledge.service`
**构造**: `new RagService(KnowledgeDao knowledgeDao, EmbeddingService embeddingService)`

---

### retrieve

```java
public List<KnowledgeEntry> retrieve(String query, Long userId, int topK)
    throws Exception
```

获取最相关的知识条目列表。

**参数**:
| 名称 | 类型 | 必填 | 说明 |
|------|------|------|------|
| query | String | 是 | 搜索查询文本 |
| userId | Long | 是 | 用户 ID（正整数） |
| topK | int | 是 | 返回结果数量（1-100） |

**返回值**: `List<KnowledgeEntry>` — 按余弦相似度降序排列

---

### retrieveWithThreshold

```java
public List<KnowledgeEntry> retrieveWithThreshold(
    String query, Long userId, int topK, double minSimilarity
) throws Exception
```

带相似度阈值过滤的检索。

**额外参数**:
| 名称 | 类型 | 说明 |
|------|------|------|
| minSimilarity | double | 最低相似度阈值 [0, 1] |

---

### buildContext

```java
public String buildContext(String query, Long userId, int topK) throws Exception
```

构建 LLM prompt 可用的上下文字符串。

**返回值格式**:
```
## Relevant Knowledge (Top N results):

### [1] 标题
正文内容...
Tags: tag1, tag2, ...

### [2] 标题
...
```

**无结果时返回**: `"No relevant knowledge found."`

---

### addKnowledge

```java
public KnowledgeEntry addKnowledge(
    String title, String content, String contentType,
    List<String> tags, String source, Long userId
) throws Exception
```

新增知识条目（自动生成向量）。

---

## KnowledgeDao

知识库数据访问对象。

**包**: `com.knowledge.dao`
**构造**: `new KnowledgeDao(Connection connection, VectorStore vectorStore)`

---

### insert

```java
public KnowledgeEntry insert(
    String title, String content, String contentType,
    List<String> tags, String source, Long userId
) throws Exception
```

插入知识条目，自动生成向量。

**参数**:
| 名称 | 类型 | 说明 |
|------|------|------|
| title | String | 标题 |
| content | String | 正文内容 |
| contentType | String | 内容类型（article/book/note 等） |
| tags | List\<String\> | 标签列表 |
| source | String | 来源标识 |
| userId | Long | 用户 ID |

**返回值**: `KnowledgeEntry` — 含 DB 生成 id 和 embedding

---

### search

```java
public List<KnowledgeSearchResult> search(String query, Long userId, int topK)
    throws Exception
```

向量语义搜索，返回轻量 DTO（不含 embedding）。

**返回值**: `List<KnowledgeSearchResult>`

---

### searchWithEntries

```java
public List<KnowledgeEntry> searchWithEntries(String query, Long userId, int topK)
    throws Exception
```

向量语义搜索，返回完整 `KnowledgeEntry` 列表。

---

### findById

```java
public Optional<KnowledgeEntry> findById(Long id, Long userId) throws Exception
```

按 ID 查询单条知识。

---

### findAll

```java
public List<KnowledgeEntry> findAll(Long userId, int limit, int offset)
    throws Exception
```

分页列出用户知识条目（最多 1000 条/次）。

---

### streamAll

```java
public void streamAll(Long userId, int batchSize, KnowledgeEntryConsumer consumer)
    throws Exception
```

流式读取所有知识条目，适用于大数据量场景。

**回调接口**:
```java
@FunctionalInterface
public interface KnowledgeEntryConsumer {
    void accept(KnowledgeEntry entry) throws Exception;
}
```

---

### update

```java
public boolean update(Long id, Long userId, String title, String content,
    String contentType, List<String> tags, String source) throws Exception
```

更新知识条目（自动重新生成向量）。

---

### delete

```java
public boolean delete(Long id, Long userId) throws Exception
```

删除知识条目（按 id + userId 双重校验）。

---

## ChatMessageDao

对话历史数据访问。

**包**: `com.knowledge.dao`
**构造**: `new ChatMessageDao(Connection connection)`

---

### insert

```java
public ChatMessage insert(String sessionKey, String role, String content, Long userId)
    throws SQLException
```

插入对话消息。

**参数**:
| 名称 | 类型 | 说明 |
|------|------|------|
| sessionKey | String | 会话 ID |
| role | String | user / assistant / system |
| content | String | 消息内容 |
| userId | Long | 用户 ID |

---

### findBySession

```java
public List<ChatMessage> findBySession(String sessionKey, Long userId, int limit)
    throws SQLException
```

按会话 Key 读取消息历史（按时间升序）。

---

### findRecent

```java
public List<ChatMessage> findRecent(Long userId, int hours, int limit)
    throws SQLException
```

获取最近 N 小时内消息（按时间降序）。

---

### countBySession

```java
public int countBySession(String sessionKey, Long userId) throws SQLException
```

统计会话消息数量。

---

### deleteOlderThan

```java
public int deleteOlderThan(Long userId, int days) throws SQLException
```

清理 N 天前的消息。返回删除条数。

---

### findDistinctSessions

```java
public List<String> findDistinctSessions(Long userId, int limit) throws SQLException
```

获取用户所有会话 ID（按最近活动时间排序）。

---

## UserProfileDao

用户配置数据访问。

**包**: `com.knowledge.dao`
**构造**: `new UserProfileDao(Connection connection)`

---

### set

```java
public void set(String key, Object value, Long userId) throws SQLException
```

UPSERT 配置项。value 支持任意可 JSON 序列化的对象。

---

### get

```java
public Optional<UserProfile> get(String key, Long userId) throws SQLException
```

读取配置项。

---

### getValue

```java
public <T> Optional<T> getValue(String key, Long userId, Class<T> valueClass)
    throws SQLException
```

读取并反序列化为指定类型。

---

### exists

```java
public boolean exists(String key, Long userId) throws SQLException
```

检查配置项是否存在。

---

### delete

```java
public boolean delete(String key, Long userId) throws SQLException
```

删除配置项。

---

## 模型类

### KnowledgeEntry

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| title | String | 标题 |
| content | String | 正文 |
| contentType | String | 内容类型 |
| tags | List\<String\> | 标签 |
| embedding | float[] | 1024维向量 |
| source | String | 来源 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |
| userId | Long | 所属用户 |
| isShared | Boolean | 是否共享 |

### KnowledgeSearchResult

继承 `KnowledgeEntry` 所有字段，额外增加:

| 字段 | 类型 | 说明 |
|------|------|------|
| distance | double | 余弦距离（越小越相似） |

### ChatMessage

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| sessionKey | String | 会话 ID |
| role | String | user/assistant/system |
| content | String | 消息内容 |
| createdAt | LocalDateTime | 发送时间 |
| userId | Long | 所属用户 |

### UserProfile

| 字段 | 类型 | 说明 |
|------|------|------|
| key | String | 配置键 |
| value | Object | 配置值（JSONB 反序列化） |
| updatedAt | LocalDateTime | 更新时间 |
| userId | Long | 用户 ID |
