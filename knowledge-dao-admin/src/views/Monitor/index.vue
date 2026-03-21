<template>
  <div class="monitor-page">
    <!-- 顶部指标卡片 -->
    <el-row :gutter="16" class="metrics-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-value primary">{{ metrics.totalRequests || 0 }}</div>
          <div class="metric-label">总请求数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-value success">{{ metrics.successRequests || 0 }}</div>
          <div class="metric-label">成功请求</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-value danger">{{ metrics.errorRequests || 0 }}</div>
          <div class="metric-label">错误请求</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-value">{{ errorRate }}%</div>
          <div class="metric-label">错误率</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表 -->
    <el-row :gutter="16" class="chart-section">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><div class="card-header"><span>各接口响应时间 (ms)</span></div></template>
          <div ref="responseTimeChartRef" style="height: 280px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><div class="card-header"><span>请求量分布</span></div></template>
          <div ref="callCountChartRef" style="height: 280px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 详情表格 -->
    <el-row :gutter="16" class="table-section">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header><div class="card-header"><span>接口详情</span></div></template>
          <el-table :data="endpointData" stripe size="small">
            <el-table-column prop="path" label="接口路径" min-width="200" show-overflow-tooltip />
            <el-table-column prop="calls" label="调用次数" width="120" sortable />
            <el-table-column prop="errors" label="错误次数" width="100">
              <template #default="{ row }"><span :class="row.errors > 0 ? 'text-danger' : ''">{{ row.errors }}</span></template>
            </el-table-column>
            <el-table-column prop="avgMs" label="平均响应 (ms)" width="130" sortable>
              <template #default="{ row }">{{ (row.avgMs || 0).toFixed(1) }}</template>
            </el-table-column>
            <el-table-column prop="p99Ms" label="P99响应 (ms)" width="130" sortable>
              <template #default="{ row }">{{ (row.p99Ms || 0).toFixed(1) }}</template>
            </el-table-column>
            <el-table-column label="健康度" width="120">
              <template #default="{ row }">
                <el-tag size="small" :type="healthType(row)">{{ healthLabel(row) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 热门查询 & 系统信息 -->
    <el-row :gutter="16" class="info-section">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><div class="card-header"><span>热门查询 Top20</span></div></template>
          <div v-if="hotQueries.length" class="hot-list">
            <div v-for="(item, idx) in hotQueries" :key="idx" class="hot-item">
              <span class="hot-rank" :class="{ top: idx < 3 }">{{ idx + 1 }}</span>
              <span class="hot-query">{{ item[0] }}</span>
              <span class="hot-count">{{ item[1] }} 次</span>
            </div>
          </div>
          <el-empty v-else description="暂无数据" :image-size="60" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><div class="card-header"><span>系统信息</span></div></template>
          <div class="sys-info">
            <div class="sys-row"><span class="sys-label">Admin API</span><el-tag size="small" type="success">localhost:8081</el-tag></div>
            <div class="sys-row"><span class="sys-label">RAG API</span><el-tag size="small" type="success">localhost:8080</el-tag></div>
            <div class="sys-row"><span class="sys-label">向量模型</span><el-tag size="small">bge-m3</el-tag></div>
            <div class="sys-row"><span class="sys-label">向量维度</span><el-tag size="small">1024</el-tag></div>
            <div class="sys-row"><span class="sys-label">平均响应</span><span class="sys-value">{{ (metrics.avgResponseTimeMs || 0).toFixed(1) }} ms</span></div>
            <div class="sys-row"><span class="sys-label">P99响应</span><span class="sys-value">{{ (metrics.p99ResponseTimeMs || 0).toFixed(1) }} ms</span></div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getApiMetrics, getSearchStats } from '@/api/monitor'

const metrics = ref({})
const hotQueries = ref([])
const responseTimeChartRef = ref(null)
const callCountChartRef = ref(null)
let responseTimeChart = null
let callCountChart = null

const errorRate = computed(() => {
  const total = metrics.value.totalRequests || 0
  const errors = metrics.value.errorRequests || 0
  if (!total) return '0.00'
  return ((errors / total) * 100).toFixed(2)
})

const endpointData = computed(() => {
  const endpoints = metrics.value.endpoints || {}
  return Object.entries(endpoints).map(([path, data]) => ({
    path,
    calls: data.calls || 0,
    errors: data.errors || 0,
    avgMs: data.avgMs || 0,
    p99Ms: data.p99Ms || 0
  }))
})

const healthType = (row) => {
  if (row.errors > 0) return 'danger'
  if (row.avgMs > 500) return 'warning'
  return 'success'
}

const healthLabel = (row) => {
  if (row.errors > 0) return '有错误'
  if (row.avgMs > 500) return '响应慢'
  return '正常'
}

const initCharts = () => {
  responseTimeChart = echarts.init(responseTimeChartRef.value)
  callCountChart = echarts.init(callCountChartRef.value)
  updateCharts()
}

const updateCharts = () => {
  const endpoints = metrics.value.endpoints || {}
  const entries = Object.entries(endpoints).slice(0, 10)

  responseTimeChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['平均响应', 'P99响应'], bottom: 0 },
    xAxis: { type: 'category', data: entries.map(([p]) => p.split(' ')[1] || p), axisLabel: { rotate: 30, fontSize: 10 } },
    yAxis: { type: 'value', name: 'ms' },
    series: [
      { name: '平均响应', type: 'bar', data: entries.map(([, d]) => parseFloat((d.avgMs || 0).toFixed(1))) },
      { name: 'P99响应', type: 'bar', data: entries.map(([, d]) => parseFloat((d.p99Ms || 0).toFixed(1))) }
    ]
  })

  callCountChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['35%', '65%'],
      data: [
        { name: '成功', value: metrics.value.successRequests || 0, itemStyle: { color: '#67c23a' } },
        { name: '失败', value: metrics.value.errorRequests || 0, itemStyle: { color: '#f56c6c' } }
      ]
    }]
  })
}

onMounted(async () => {
  try {
    const [metricsRes, statsRes] = await Promise.allSettled([
      getApiMetrics(),
      getSearchStats()
    ])
    if (metricsRes.status === 'fulfilled') Object.assign(metrics.value, metricsRes.value)
    if (statsRes.status === 'fulfilled' && statsRes.value) {
      hotQueries.value = Object.entries(statsRes.value.topQueries || {}).slice(0, 20)
    }
  } catch {
    // Use empty data
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
.monitor-page { }
.metrics-cards { margin-bottom: 16px; }
.metric-card { text-align: center; padding: 8px 0; .metric-value { font-size: 28px; font-weight: bold; &.primary { color: #409eff; } &.success { color: #67c23a; } &.danger { color: #f56c6c; } } .metric-label { font-size: 13px; color: #909399; margin-top: 4px; } }
.chart-section { margin-bottom: 16px; }
.table-section { margin-bottom: 16px; }
.info-section { margin-bottom: 16px; }
.card-header { font-weight: 600; }
.text-danger { color: #f56c6c; }
.hot-list { .hot-item { display: flex; align-items: center; padding: 8px 0; border-bottom: 1px solid #f5f5f5; &:last-child { border-bottom: none; } .hot-rank { width: 20px; height: 20px; border-radius: 50%; background: #e4e7ed; color: #909399; display: inline-flex; align-items: center; justify-content: center; font-size: 11px; margin-right: 8px; flex-shrink: 0; &.top { background: #409eff; color: #fff; } } .hot-query { flex: 1; font-size: 13px; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; } .hot-count { color: #c0c4cc; font-size: 12px; margin-left: 8px; } } }
.sys-info { .sys-row { display: flex; align-items: center; gap: 12px; padding: 10px 0; border-bottom: 1px solid #f5f5f5; &:last-child { border-bottom: none; } .sys-label { width: 120px; color: #606266; font-size: 13px; } .sys-value { color: #409eff; font-weight: 600; } } }
</style>
