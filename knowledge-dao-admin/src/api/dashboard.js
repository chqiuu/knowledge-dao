import request from '@/utils/request'

// 系统概览统计
export const getDashboardStats = () => request.get('/stats')

// 健康检查
export const getHealthStatus = () => request.get('/health')

// API 指标（用于 Dashboard 图表）
export const getApiMetrics = () => request.get('/metrics')

// 搜索统计
export const getSearchStats = () => request.get('/searches/stats')

// 最近活动时间线（对应 DashboardController /activities）
export const getRecentActivities = () => request.get('/activities')

// 操作日志（备用）
export const getOperationLogs = (params) => request.get('/activities', { params })

// 健康检查（用于 Dashboard 向量状态）
export const getVectorStatus = () => request.get('/health')

// 获取告警信息（从 metrics 中提取错误率）
export const getAlerts = async () => {
  try {
    const metrics = await getApiMetrics()
    const alerts = []
    if (metrics.errorRequests > 0) {
      const errorRate = ((metrics.errorRequests / metrics.totalRequests) * 100).toFixed(2)
      if (parseFloat(errorRate) > 1) {
        alerts.push({ time: new Date().toLocaleString('zh-CN'), level: 'Warning', message: `API 错误率异常: ${errorRate}%` })
      }
    }
    return alerts
  } catch {
    return []
  }
}
