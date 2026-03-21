import request from '@/utils/request'

// API 指标
export const getApiMetrics = () => request.get('/metrics')

// 健康检查
export const getHealthStatus = () => request.get('/health')

// 搜索统计
export const getSearchStats = () => request.get('/searches/stats')
