import request from '@/utils/request'

export const getVectorConfig = () => request.get('/config/vector')
export const updateVectorConfig = (data) => request.put('/config/vector', data)
export const getRagConfig = () => request.get('/config/rag')
export const updateRagConfig = (data) => request.put('/config/rag', data)
