import axios from 'axios'

// 单独的 axios 实例用于 RAG API（端口 8080）
const ragRequest = axios.create({
  baseURL: '/api/rag',
  timeout: 30000
})

ragRequest.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 0 && res.code !== 200) {
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data ?? res
  },
  (error) => Promise.reject(error)
)

// 执行检索
export const search = (params) => ragRequest.post('/search', params)

// 转换 distance 为相似度分数
const distanceToScore = (distance) => {
  if (distance == null) return 0
  // 余弦距离转相似度
  return 1 / (1 + distance)
}

// 搜索结果标准化（添加 score 字段）
export const normalizeSearchResults = (results) => {
  if (!Array.isArray(results)) return []
  return results.map(r => ({
    id: r.id,
    title: r.title,
    content: r.content,
    tags: r.tags || [],
    source: r.source || 'knowledge',
    score: distanceToScore(r.distance),
    contentType: r.contentType,
    createdAt: r.createdAt
  }))
}
