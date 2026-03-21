<template>
  <div class="config-page">
    <el-alert type="info" :closable="false" style="margin-bottom: 16px">
      以下配置为系统运行时参数，部分参数需要重启服务生效。配置变更请联系管理员修改 config.yaml 文件。
    </el-alert>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header"><span>Embedding 配置</span></div>
          </template>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="模型名称">{{ config.ollama?.embeddingModel || 'bge-m3' }}</el-descriptions-item>
            <el-descriptions-item label="向量维度">{{ config.app?.embeddingDimension || 1024 }}</el-descriptions-item>
            <el-descriptions-item label="Ollama 地址">{{ config.ollama?.baseUrl || 'http://localhost:11434' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header"><span>数据库配置</span></div>
          </template>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="数据库类型">PostgreSQL + pgvector</el-descriptions-item>
            <el-descriptions-item label="主机地址">{{ config.db?.host || 'localhost' }}</el-descriptions-item>
            <el-descriptions-item label="端口">{{ config.db?.port || 5432 }}</el-descriptions-item>
            <el-descriptions-item label="数据库名">{{ config.db?.database || 'knowledge' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <!-- UI 配置 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header"><span>界面偏好设置</span><el-button type="primary" size="small" style="margin-left:auto" @click="savePrefs">保存偏好</el-button></div>
          </template>
          <el-form label-width="120px" label-position="left" size="default">
            <el-form-item label="每页条目数">
              <el-input-number v-model="prefs.pageSize" :min="10" :max="100" :step="10" />
            </el-form-item>
            <el-form-item label="默认检索 topK">
              <el-input-number v-model="prefs.defaultTopK" :min="1" :max="100" />
            </el-form-item>
            <el-form-item label="默认相似度阈值">
              <el-slider v-model="prefs.similarityThreshold" :min="0" :max="1" :step="0.05" :format-tooltip="(v) => v.toFixed(2)" style="flex:1" />
              <span style="margin-left:12px;color:#909399">{{ prefs.similarityThreshold.toFixed(2) }}</span>
            </el-form-item>
            <el-form-item label="时间显示格式">
              <el-select v-model="prefs.timeFormat" style="width:100%">
                <el-option label="YYYY-MM-DD HH:mm:ss" value="full" />
                <el-option label="MM-DD HH:mm" value="short" />
                <el-option label="相对时间" value="relative" />
              </el-select>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header"><span>系统信息</span></div>
          </template>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="Admin API">http://localhost:8081</el-descriptions-item>
            <el-descriptions-item label="RAG API">http://localhost:8080</el-descriptions-item>
            <el-descriptions-item label="向量模型">bge-m3</el-descriptions-item>
            <el-descriptions-item label="向量维度">1024</el-descriptions-item>
            <el-descriptions-item label="前端端口">5173</el-descriptions-item>
            <el-descriptions-item label="版本">knowledge-dao v1.0.0</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const config = ref({
  ollama: { embeddingModel: 'bge-m3', baseUrl: 'http://localhost:11434' },
  app: { embeddingDimension: 1024 },
  db: { host: 'localhost', port: 5432, database: 'knowledge' }
})

const prefs = reactive({
  pageSize: 20,
  defaultTopK: 10,
  similarityThreshold: 0.65,
  timeFormat: 'full'
})

const savePrefs = () => {
  localStorage.setItem('kd-admin-prefs', JSON.stringify(prefs))
  ElMessage.success('偏好设置已保存')
}

onMounted(() => {
  const saved = localStorage.getItem('kd-admin-prefs')
  if (saved) {
    try {
      Object.assign(prefs, JSON.parse(saved))
    } catch {}
  }
})
</script>

<style lang="scss" scoped>
.config-page { }
.card-header { font-weight: 600; display: flex; align-items: center; }
</style>
