<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #409eff"><el-icon><Document /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalEntries || 0 }}</div>
            <div class="stat-label">知识条目总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #67c23a"><el-icon><ChatDotRound /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalSessions || 0 }}</div>
            <div class="stat-label">会话总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #e6a23c"><el-icon><Search /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalSearches || 0 }}</div>
            <div class="stat-label">检索次数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #f56c6c"><el-icon><User /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalUsers || 0 }}</div>
            <div class="stat-label">用户总数</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 健康状态 & 向量状态 -->
    <el-row :gutter="16" class="health-section">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>系统状态</span>
              <el-tag :type="healthOk ? 'success' : 'danger'" size="small">{{ healthOk ? '正常' : '异常' }}</el-tag>
            </div>
          </template>
          <div class="health-items">
            <div class="health-item"><span>数据库</span><el-tag size="small" :type="healthOk ? 'success' : 'danger'">{{ healthOk ? '已连接' : '未连接' }}</el-tag></div>
            <div class="health-item"><span>共享条目</span><span class="value">{{ stats.sharedEntries || 0 }} 条</span></div>
            <div class="health-item"><span>存储估算</span><span class="value">{{ formatBytes(stats.storageBytes) }}</span></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><div class="card-header"><span>条目类型分布</span></div></template>
          <div class="type-list">
            <div v-for="(count, type) in stats.entriesByType" :key="type" class="type-item">
              <span class="type-name">{{ type }}</span>
              <el-progress :percentage="calcPercentage(count)" :color="typeColor(type)" style="flex:1;margin:0 12px" />
              <span class="type-count">{{ count }}</span>
            </div>
            <el-empty v-if="!hasTypes" description="暂无数据" :image-size="60" />
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><div class="card-header"><span>热门标签 Top10</span></div></template>
          <div class="tag-list">
            <div v-for="(count, tag) in stats.topTags" :key="tag" class="tag-item">
              <el-tag size="small" style="margin-right:8px">{{ tag }}</el-tag>
              <span class="tag-count">{{ count }}</span>
            </div>
            <el-empty v-if="!hasTags" description="暂无标签" :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="16" class="chart-section">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><div class="card-header"><span>API 响应时间</span></div></template>
          <div ref="responseTimeChartRef" style="height: 260px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><div class="card-header"><span>API 调用统计</span></div></template>
          <div class="metrics-summary">
            <div class="metric-row">
              <span class="metric-label">总请求数</span>
              <span class="metric-value primary">{{ metrics.totalRequests || 0 }}</span>
            </div>
            <div class="metric-row">
              <span class="metric-label">成功请求</span>
              <span class="metric-value success">{{ metrics.successRequests || 0 }}</span>
            </div>
            <div class="metric-row">
              <span class="metric-label">错误请求</span>
              <span class="metric-value danger">{{ metrics.errorRequests || 0 }}</span>
            </div>
            <div class="metric-row">
              <span class="metric-label">平均响应时间</span>
              <span class="metric-value">{{ (metrics.avgResponseTimeMs || 0).toFixed(1) }} ms</span>
            </div>
            <div class="metric-row">
              <span class="metric-label">P99 响应时间</span>
              <span class="metric-value">{{ (metrics.p99ResponseTimeMs || 0).toFixed(1) }} ms</span>
            </div>
          </div>
          <div ref="callCountChartRef" style="height: 180px; margin-top: 16px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 活动时间线 -->
    <el-row :gutter="16" class="timeline-section">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><div class="card-header"><span>最近操作日志</span></div></template>
          <el-timeline v-if="logs.length">
            <el-timeline-item v-for="log in logs" :key="log.id" :timestamp="formatTime(log.createdAt)" placement="top">
              <p class="log-action">{{ log.action }} {{ log.resource }}</p>
              <p class="log-detail">{{ log.details }}</p>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无操作记录" :image-size="60" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><div class="card-header"><span>告警信息</span></div></template>
          <el-table v-if="alerts.length" :data="alerts" size="small" style="width:100%">
            <el-table-column prop="time" label="时间" width="160" />
            <el-table-column prop="level" label="级别" width="80">
              <template #default="{ row }"><el-tag size="small" :type="levelType(row.level)">{{ row.level }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="message" label="信息" />
          </el-table>
          <el-empty v-else description="暂无告警" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getDashboardStats, getHealthStatus, getApiMetrics, getRecentActivities, getAlerts } from '@/api/dashboard'

const stats = ref({})
const healthOk = ref(false)
const metrics = ref({})
const logs = ref([])
const alerts = ref([])
const responseTimeChartRef = ref(null)
const callCountChartRef = ref(null)
let responseTimeChart = null
let callCountChart = null

const hasTypes = computed(() => stats.value.entriesByType && Object.keys(stats.value.entriesByType).length > 0)
const hasTags = computed(() => stats.value.topTags && Object.keys(stats.value.topTags).length > 0)
const totalEntries = computed(() => stats.value.totalEntries || 0)

const calcPercentage = (count) => {
  if (!totalEntries.value) return 0
  return Math.round((count / totalEntries.value) * 100)
}

const typeColor = (type) => {
  const colors = { article: '#409eff', document: '#67c23a', note: '#e6a23c', book: '#f56c6c' }
  return colors[type] || '#909399'
}

const formatBytes = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

const formatTime = (t) => t ? t.replace('T', ' ').slice(0, 19) : ''

const levelType = (level) => ({ Critical: 'danger', Warning: 'warning', Info: 'info' }[level] || 'info')

const initCharts = () => {
  responseTimeChart = echarts.init(responseTimeChartRef.value)
  callCountChart = echarts.init(callCountChartRef.value)
  updateCharts()
}

const updateCharts = () => {
  const endpoints = metrics.value.endpoints || {}
  const paths = Object.keys(endpoints).slice(0, 8)

  responseTimeChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['平均响应', 'P99响应'], bottom: 0 },
    xAxis: { type: 'category', data: paths.map(p => p.split(' ')[1] || p), axisLabel: { rotate: 30, fontSize: 10 } },
    yAxis: { type: 'value', name: 'ms' },
    series: [
      { name: '平均响应', type: 'bar', data: paths.map(p => parseFloat((endpoints[p].avgMs || 0).toFixed(1))) },
      { name: 'P99响应', type: 'bar', data: paths.map(p => parseFloat((endpoints[p].p99Ms || 0).toFixed(1))) }
    ]
  })

  callCountChart.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data: [
        { name: '成功', value: metrics.value.successRequests || 0, itemStyle: { color: '#67c23a' } },
        { name: '失败', value: metrics.value.errorRequests || 0, itemStyle: { color: '#f56c6c' } }
      ]
    }]
  })
}

onMounted(async () => {
  try {
    const [statsRes, healthRes, metricsRes, logsRes, alertsRes] = await Promise.allSettled([
      getDashboardStats(),
      getHealthStatus(),
      getApiMetrics(),
      getRecentActivities(),
      getAlerts()
    ])

    if (statsRes.status === 'fulfilled') Object.assign(stats.value, statsRes.value)
    if (healthRes.status === 'fulfilled') healthOk.value = true
    if (metricsRes.status === 'fulfilled') Object.assign(metrics.value, metricsRes.value)
    if (logsRes.status === 'fulfilled') logs.value = (logsRes.value?.data || logsRes.value || []).slice(0, 10)
    if (alertsRes.status === 'fulfilled') alerts.value = alertsRes.value
  } catch {
    // API not available, use mock data
  }

  await nextTick()
  initCharts()

  window.addEventListener('resize', () => {
    responseTimeChart?.resize()
    callCountChart?.resize()
  })
})

onUnmounted(() => {
  responseTimeChart?.dispose()
  callCountChart?.dispose()
})
</script>

<style lang="scss" scoped>
.dashboard {
  .stat-cards { margin-bottom: 16px; }
  .stat-card { display: flex; align-items: center; padding: 8px; }
  .stat-icon {
    width: 56px; height: 56px; border-radius: 8px;
    display: flex; align-items: center; justify-content: center; margin-right: 16px;
    .el-icon { font-size: 28px; color: #fff; }
  }
  .stat-info {
    .stat-value { font-size: 28px; font-weight: bold; color: #303133; }
    .stat-label { font-size: 13px; color: #909399; margin-top: 4px; }
  }
  .health-section { margin-bottom: 16px; }
  .chart-section { margin-bottom: 16px; }
  .timeline-section { margin-bottom: 16px; }
  .card-header { font-weight: 600; display: flex; align-items: center; justify-content: space-between; }
  .health-items {
    .health-item { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid #f0f0f0; &:last-child { border-bottom: none; } .value { color: #409eff; font-weight: 500; } }
  }
  .type-list { .type-item { display: flex; align-items: center; padding: 8px 0; .type-name { width: 80px; font-size: 13px; color: #606266; } .type-count { font-size: 13px; color: #909399; min-width: 40px; text-align: right; } } }
  .tag-list { .tag-item { display: flex; align-items: center; justify-content: space-between; padding: 6px 0; .tag-count { color: #909399; font-size: 13px; } } }
  .metrics-summary { .metric-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #f5f5f5; &:last-child { border-bottom: none; } .metric-label { color: #606266; font-size: 13px; } .metric-value { font-weight: 600; font-size: 14px; color: #303133; &.primary { color: #409eff; } &.success { color: #67c23a; } &.danger { color: #f56c6c; } } } }
  .log-action { font-weight: 600; color: #303133; margin: 0; font-size: 13px; }
  .log-detail { font-size: 12px; color: #909399; margin: 2px 0 0; }
}
</style>
