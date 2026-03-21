<template>
  <div class="retrieval-page">
    <el-row :gutter="16">
      <!-- 左侧：检索区 -->
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header><span>检索测试</span></template>
          <div class="search-area">
            <el-input
              v-model="query"
              placeholder="输入检索 query..."
              size="large"
              @keyup.enter="handleSearch"
            >
              <template #append>
                <el-button :loading="searching" @click="handleSearch">检索</el-button>
              </template>
            </el-input>
            <el-row :gutter="16" style="margin-top: 16px">
              <el-col :span="12">
                <div class="param-item">
                  <span class="param-label">topK:</span>
                  <el-input-number v-model="searchParams.topK" :min="1" :max="100" size="small" />
                </div>
              </el-col>
              <el-col :span="12">
                <div class="param-item">
                  <span class="param-label">用户ID:</span>
                  <el-input-number v-model="searchParams.userId" :min="1" :max="99999" size="small" />
                </div>
              </el-col>
            </el-row>
          </div>

          <div v-if="searchResults.length" class="results-area">
            <div class="results-header">召回结果 ({{ searchResults.length }})</div>
            <el-card v-for="(item, idx) in searchResults" :key="idx" shadow="hover" class="result-card">
              <div class="result-header">
                <span class="result-title">{{ item.title }}</span>
                <el-tag size="small" :type="scoreType(item.score)" effect="dark">
                  相似度 {{ (item.score * 100).toFixed(1) }}%
                </el-tag>
              </div>
              <div class="result-content">{{ item.content }}</div>
              <div class="result-meta">
                <el-tag v-for="tag in (item.tags || [])" :key="tag" size="small" style="margin-right:4px">{{ tag }}</el-tag>
                <span class="result-source">{{ item.source || 'knowledge' }}</span>
              </div>
            </el-card>
          </div>
          <el-empty v-else-if="searched" description="未找到相关结果" />
        </el-card>
      </el-col>

      <!-- 右侧：历史 & 热门 -->
      <el-col :span="8">
        <el-card shadow="hover" style="margin-bottom: 16px">
          <template #header><div class="card-header"><span>热门 Query Top20</span></div></template>
          <div v-if="hotQueries.length" class="hot-list">
            <div v-for="(item, idx) in hotQueries" :key="idx" class="hot-item">
              <span class="hot-rank" :class="{ top: idx < 3 }">{{ idx + 1 }}</span>
              <span class="hot-query">{{ item[0] }}</span>
              <span class="hot-count">{{ item[1] }} 次</span>
            </div>
          </div>
          <el-empty v-else description="暂无数据" :image-size="60" />
        </el-card>

        <el-card shadow="hover">
          <template #header><div class="card-header"><span>搜索统计</span></div></template>
          <div v-if="searchStats" class="stats-list">
            <div class="stats-item"><span>总检索次数</span><span class="value">{{ searchStats.totalSearches || 0 }}</span></div>
            <div class="stats-item"><span>今日检索</span><span class="value">{{ searchStats.searchesToday || 0 }}</span></div>
            <div class="stats-item"><span>平均命中</span><span class="value">{{ (searchStats.avgHitCount || 0).toFixed(1) }}</span></div>
          </div>
          <el-empty v-else description="暂无数据" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { search, normalizeSearchResults } from '@/api/retrieval'
import { getSearchStats } from '@/api/dashboard'

const query = ref('')
const searching = ref(false)
const searched = ref(false)
const searchResults = ref([])
const searchParams = reactive({ topK: 10, userId: 1 })
const hotQueries = ref([])
const searchStats = ref(null)

const scoreType = (score) => {
  if (score >= 0.8) return 'success'
  if (score >= 0.6) return 'warning'
  return 'danger'
}

const handleSearch = async () => {
  if (!query.value.trim()) return
  searching.value = true
  searched.value = true
  try {
    const rawResults = await search({ query: query.value, ...searchParams })
    searchResults.value = normalizeSearchResults(rawResults || [])
  } catch (e) {
    searchResults.value = []
  } finally {
    searching.value = false
  }
}

onMounted(async () => {
  try {
    const [statsRes] = await Promise.allSettled([getSearchStats()])
    if (statsRes.status === 'fulfilled' && statsRes.value) {
      searchStats.value = statsRes.value
      const topQueries = statsRes.value.topQueries || {}
      hotQueries.value = Object.entries(topQueries).slice(0, 20)
    }
  } catch {
    // Use mock data
  }
})
</script>

<style lang="scss" scoped>
.search-area { }
.param-item { display: flex; align-items: center; gap: 8px; .param-label { width: 70px; font-size: 13px; color: #606266; } }
.results-area { margin-top: 20px; border-top: 1px solid #eee; padding-top: 16px; .results-header { font-size: 14px; color: #606266; margin-bottom: 12px; } }
.result-card { margin-bottom: 12px; }
.result-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; .result-title { font-weight: bold; color: #303133; font-size: 15px; } }
.result-content { font-size: 13px; color: #606266; margin-bottom: 8px; line-height: 1.6; max-height: 120px; overflow: hidden; }
.result-meta { display: flex; align-items: center; gap: 4px; flex-wrap: wrap; .result-source { color: #c0c4cc; font-size: 12px; margin-left: auto; } }
.hot-list { .hot-item { display: flex; align-items: center; padding: 8px 0; border-bottom: 1px solid #f5f5f5; &:last-child { border-bottom: none; } .hot-rank { width: 20px; height: 20px; border-radius: 50%; background: #e4e7ed; color: #909399; display: inline-flex; align-items: center; justify-content: center; font-size: 11px; margin-right: 8px; flex-shrink: 0; &.top { background: #409eff; color: #fff; } } .hot-query { flex: 1; font-size: 13px; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; } .hot-count { color: #c0c4cc; font-size: 12px; margin-left: 8px; flex-shrink: 0; } } }
.stats-list { .stats-item { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #f5f5f5; &:last-child { border-bottom: none; } .value { color: #409eff; font-weight: 600; font-size: 15px; } } }
.card-header { font-weight: 600; }
</style>
