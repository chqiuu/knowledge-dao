<template>
  <div class="knowledge-page">
    <el-card shadow="hover">
      <template #header>
        <div class="toolbar">
          <div class="toolbar-left">
            <el-input v-model="searchKeyword" placeholder="标题/内容关键词搜索" style="width: 200px" clearable @keyup.enter="loadData" />
            <el-select v-model="filterType" placeholder="类型筛选" style="width: 130px; margin-left: 8px" clearable>
              <el-option label="全部" value="" />
              <el-option label="article" value="article" />
              <el-option label="document" value="document" />
              <el-option label="note" value="note" />
              <el-option label="book" value="book" />
            </el-select>
            <el-button type="primary" style="margin-left: 8px" @click="loadData">搜索</el-button>
          </div>
          <div class="toolbar-right">
            <el-button type="primary" @click="openAddDialog">新增</el-button>
            <el-button @click="openImportDialog">批量导入</el-button>
            <el-button :disabled="!selectedRows.length" type="danger" plain @click="handleBatchDelete">批量删除</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" @selection-change="handleSelectionChange" stripe>
        <el-table-column type="selection" width="40" />
        <el-table-column prop="id" label="ID" width="80" show-overflow-tooltip />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="contentType" label="类型" width="100">
          <template #default="{ row }"><el-tag size="small">{{ row.contentType || 'article' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="tags" label="标签" width="180">
          <template #default="{ row }">
            <el-tag v-for="tag in (row.tags || [])" :key="tag" size="small" style="margin-right:4px">{{ tag }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="100" show-overflow-tooltip />
        <el-table-column prop="isShared" label="共享" width="70">
          <template #default="{ row }"><el-tag size="small" :type="row.isShared ? 'success' : 'info'">{{ row.isShared ? '是' : '否' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="handleRebuild(row)">重建向量</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="条目详情" size="520px">
      <div v-if="currentRow" class="detail-content">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="ID">{{ currentRow.id }}</el-descriptions-item>
          <el-descriptions-item label="标题">{{ currentRow.title }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ currentRow.contentType }}</el-descriptions-item>
          <el-descriptions-item label="来源">{{ currentRow.source }}</el-descriptions-item>
          <el-descriptions-item label="标签">
            <el-tag v-for="tag in (currentRow.tags || [])" :key="tag" size="small" style="margin-right:4px">{{ tag }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="共享">
            <el-tag :type="currentRow.isShared ? 'success' : 'info'" size="small">{{ currentRow.isShared ? '是' : '否' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="内容" label-class-name="content-label">
            <div class="content-preview">{{ currentRow.content }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(currentRow.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatTime(currentRow.updatedAt) }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'add' ? '新增条目' : '编辑条目'" width="600px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="类型" prop="contentType">
          <el-select v-model="form.contentType" style="width:100%">
            <el-option label="article" value="article" />
            <el-option label="document" value="document" />
            <el-option label="note" value="note" />
            <el-option label="book" value="book" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签" prop="tags">
          <el-input v-model="form.tagsInput" placeholder="多个标签用逗号分隔" />
        </el-form-item>
        <el-form-item label="来源" prop="source">
          <el-input v-model="form.source" placeholder="如: admin_api" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入对话框 -->
    <el-dialog v-model="importVisible" title="批量导入" width="600px">
      <el-alert type="info" :closable="false" style="margin-bottom: 16px">
        请准备 JSON 格式的批量导入文件，每条记录包含 title、content、tags（数组）、contentType、userId 字段。
      </el-alert>
      <el-input v-model="importText" type="textarea" :rows="10" placeholder='[{"title": "标题", "content": "内容", "tags": ["tag1"], "contentType": "article", "userId": 1}]' />
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="handleImport">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getKnowledgeList, createKnowledge, updateKnowledge, deleteKnowledge, rebuildVector, batchImport } from '@/api/knowledge'

const searchKeyword = ref('')
const filterType = ref('')
const tableData = ref([])
const loading = ref(false)
const selectedRows = ref([])
const pagination = reactive({ page: 1, pageSize: 20, total: 0 })

const detailVisible = ref(false)
const currentRow = ref(null)

const dialogVisible = ref(false)
const dialogMode = ref('add')
const formRef = ref(null)
const submitting = ref(false)
const form = reactive({ id: null, title: '', content: '', contentType: 'article', tagsInput: '', source: 'admin_api' })
const formRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

const importVisible = ref(false)
const importing = ref(false)
const importText = ref('')

const formatTime = (t) => t ? t.replace('T', ' ').slice(0, 19) : ''

const buildFormData = () => {
  const tags = form.tagsInput
    ? form.tagsInput.split(',').map(t => t.trim()).filter(Boolean)
    : []
  return {
    title: form.title,
    content: form.content,
    contentType: form.contentType || 'article',
    tags,
    source: form.source || 'admin_api',
    userId: 1
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const result = await getKnowledgeList({
      page: pagination.page,
      pageSize: pagination.pageSize,
      keyword: searchKeyword.value || undefined,
      type: filterType.value || undefined
    })
    // 响应结构: { total, page, pageSize, data: [...] }
    tableData.value = result?.data || []
    pagination.total = result?.total || 0
    pagination.page = result?.page || 1
    pagination.pageSize = result?.pageSize || 20
  } catch {
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const handleSelectionChange = (rows) => { selectedRows.value = rows }

const openDetail = (row) => { currentRow.value = row; detailVisible.value = true }

const openAddDialog = () => {
  dialogMode.value = 'add'
  Object.assign(form, { id: null, title: '', content: '', contentType: 'article', tagsInput: '', source: 'admin_api' })
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  dialogMode.value = 'edit'
  Object.assign(form, {
    id: row.id,
    title: row.title,
    content: row.content,
    contentType: row.contentType || 'article',
    tagsInput: (row.tags || []).join(', '),
    source: row.source || 'admin_api'
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    const data = buildFormData()
    if (dialogMode.value === 'add') {
      await createKnowledge(data)
      ElMessage.success('创建成功')
    } else {
      await updateKnowledge(form.id, data)
      ElMessage.success('更新成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除条目「${row.title}」？`, '提示', { type: 'warning' })
  try {
    await deleteKnowledge(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

const handleBatchDelete = async () => {
  const ids = selectedRows.value.map(r => r.id)
  const names = selectedRows.value.map(r => r.title).join(', ')
  await ElMessageBox.confirm(`确认删除 ${ids.length} 条选中的条目？`, '提示', { type: 'warning' })
  try {
    await Promise.all(ids.map(id => deleteKnowledge(id)))
    ElMessage.success('批量删除成功')
    loadData()
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

const handleRebuild = async (row) => {
  try {
    await rebuildVector(row.id)
    ElMessage.success('向量重建任务已提交')
  } catch (e) {
    ElMessage.error(e.message || '重建失败')
  }
}

const openImportDialog = () => { importText.value = ''; importVisible.value = true }

const handleImport = async () => {
  if (!importText.value.trim()) { ElMessage.warning('请输入导入数据'); return }
  let entries
  try {
    entries = JSON.parse(importText.value)
  } catch {
    ElMessage.error('JSON 格式错误'); return
  }
  if (!Array.isArray(entries)) { ElMessage.error('数据必须是数组格式'); return }
  importing.value = true
  try {
    const result = await batchImport({ entries })
    const summary = result || {}
    ElMessage.success(`导入完成：成功 ${summary.success || entries.length} 条，失败 ${summary.failed || 0} 条`)
    importVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error(e.message || '导入失败')
  } finally {
    importing.value = false
  }
}

onMounted(() => { loadData() })
</script>

<style lang="scss" scoped>
.toolbar { display: flex; justify-content: space-between; flex-wrap: wrap; gap: 8px; }
.toolbar-left, .toolbar-right { display: flex; align-items: center; flex-wrap: wrap; }
.detail-content { padding: 0 8px; }
.content-label { max-width: 120px; }
.content-preview { max-height: 200px; overflow-y: auto; white-space: pre-wrap; font-size: 13px; color: #606266; }
</style>
