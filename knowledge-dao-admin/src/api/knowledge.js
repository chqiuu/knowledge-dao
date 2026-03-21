import request from '@/utils/request'

// 知识条目列表（分页）
export const getKnowledgeList = (params) => request.get('/entries', { params })

// 知识条目详情（含向量信息）
export const getKnowledgeDetail = (id) => request.get(`/entries/${id}`)

// 创建知识条目
export const createKnowledge = (data) => request.post('/entries', data)

// 更新知识条目
export const updateKnowledge = (id, data) => request.put(`/entries/${id}`, data)

// 删除知识条目
export const deleteKnowledge = (id) => request.delete(`/entries/${id}`)

// 重建向量
export const rebuildVector = (id, userId = 1) => request.post(`/entries/${id}/rebuild?userId=${userId}`)

// 批量重算向量
export const batchRecalcVector = async (ids) => {
  const promises = ids.map(id => rebuildVector(id))
  return Promise.allSettled(promises)
}

// 批量导入
export const batchImport = (data) => request.post('/entries/batch', data)

// 批量删除
export const batchDelete = (ids) => {
  const promises = ids.map(id => deleteKnowledge(id))
  return Promise.allSettled(promises)
}
