<template>
  <div class="sessions-page">
    <el-row :gutter="16" class="users-section">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <div class="toolbar">
              <span>用户列表</span>
              <el-input v-model="userKeyword" placeholder="搜索用户ID" style="width:200px" size="small" clearable @keyup.enter="loadUsers" />
            </div>
          </template>
          <el-table :data="users" v-loading="usersLoading" size="small" stripe @row-click="handleUserRowClick">
            <el-table-column prop="userId" label="用户ID" width="120" />
            <el-table-column prop="entryCount" label="知识条目" width="120" />
            <el-table-column prop="sessionCount" label="会话数" width="120" />
            <el-table-column prop="lastActive" label="最后活跃时间" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="sessions-section">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <div class="toolbar">
              <span>会话列表</span>
              <el-select v-model="filterUserId" placeholder="按用户筛选" style="width:180px" size="small" clearable @change="loadSessions">
                <el-option v-for="u in users" :key="u.userId" :label="String(u.userId)" :value="String(u.userId)" />
              </el-select>
              <el-button size="small" @click="loadSessions">刷新</el-button>
            </div>
          </template>

          <el-table :data="sessions" v-loading="sessionsLoading" stripe @row-click="handleSessionRowClick">
            <el-table-column prop="sessionKey" label="会话Key" min-width="200" show-overflow-tooltip />
            <el-table-column prop="userId" label="用户ID" width="120" />
            <el-table-column prop="messageCount" label="消息数" width="100" />
            <el-table-column prop="firstMessage" label="首条消息" min-width="200" show-overflow-tooltip />
            <el-table-column prop="lastMessage" label="末条消息" min-width="200" show-overflow-tooltip />
          </el-table>

          <el-pagination
            v-model:current-page="sessionsPage"
            v-model:page-size="sessionsPageSize"
            :total="sessionsTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            style="margin-top: 16px; justify-content: flex-end"
            @size-change="loadSessions"
            @current-change="loadSessions"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 会话详情抽屉 -->
    <el-drawer v-model="chatDrawerVisible" title="会话详情" size="800px">
      <div class="chat-container">
        <div class="chat-header">
          <span class="session-key">会话: {{ currentSessionKey }}</span>
        </div>
        <div class="chat-messages" ref="chatMessagesRef">
          <div v-if="messagesLoading" style="text-align:center;padding:20px"><el-icon class="is-loading"><Loading /></el-icon></div>
          <div v-else-if="!messages.length" style="text-align:center;padding:40px;color:#909399">暂无消息</div>
          <div v-else class="message-list">
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="message-item"
              :class="msg.role"
            >
              <div class="message-avatar">
                <el-icon v-if="msg.role === 'user'"><User /></el-icon>
                <el-icon v-else><ChatDotRound /></el-icon>
              </div>
              <div class="message-body">
                <div class="message-role">{{ msg.role === 'user' ? '用户' : 'AI' }}</div>
                <div class="message-content">{{ msg.content }}</div>
                <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const users = ref([])
const usersLoading = ref(false)
const userKeyword = ref('')

const sessions = ref([])
const sessionsLoading = ref(false)
const sessionsPage = ref(1)
const sessionsPageSize = ref(20)
const sessionsTotal = ref(0)
const filterUserId = ref('')

const chatDrawerVisible = ref(false)
const currentSessionKey = ref('')
const messages = ref([])
const messagesLoading = ref(false)
const chatMessagesRef = ref(null)

const formatTime = (t) => t ? t.replace('T', ' ').slice(0, 19) : ''

const loadUsers = async () => {
  usersLoading.value = true
  try {
    const result = await request.get('/users', { params: { page: 1, pageSize: 100 } })
    const list = result?.data || []
    users.value = list.map(u => ({
      userId: u.user_id,
      entryCount: u.entry_count,
      sessionCount: u.session_count,
      lastActive: u.last_active,
    }))
  } catch {
    users.value = []
  } finally {
    usersLoading.value = false
  }
}

const loadSessions = async () => {
  sessionsLoading.value = true
  try {
    const params = { page: sessionsPage.value, pageSize: sessionsPageSize.value }
    if (filterUserId.value) params.userId = filterUserId.value
    const result = await request.get('/sessions', { params })
    const list = result?.data || []
    // API 返回 snake_case，转为 camelCase
    sessions.value = list.map(s => ({
      sessionKey: s.session_key,
      userId: s.user_id,
      messageCount: s.message_count,
      firstMessage: s.first_message || s.firstMessage || '',
      lastMessage: s.last_message || s.lastMessage || '',
    }))
    sessionsTotal.value = result?.total || 0
    sessionsPage.value = result?.page || 1
  } catch {
    sessions.value = []
    sessionsTotal.value = 0
  } finally {
    sessionsLoading.value = false
  }
}

const handleUserRowClick = (row) => {
  filterUserId.value = String(row.userId)
  sessionsPage.value = 1
  loadSessions()
}

const handleSessionRowClick = async (row) => {
  currentSessionKey.value = row.sessionKey
  chatDrawerVisible.value = true
  await loadMessages(row.sessionKey)
}

const loadMessages = async (sessionKey) => {
  messagesLoading.value = true
  messages.value = []
  try {
    const result = await request.get(`/sessions/${encodeURIComponent(sessionKey)}/messages`)
    const list = Array.isArray(result) ? result : (result?.data || [])
    // API 返回 snake_case，转为 camelCase
    messages.value = list.map(m => ({
      id: m.id,
      sessionKey: m.session_key,
      role: m.role,
      content: m.content,
      createdAt: m.created_at || m.createdAt,
    }))
    await nextTick()
    if (chatMessagesRef.value) {
      chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight
    }
  } catch {
    messages.value = []
  } finally {
    messagesLoading.value = false
  }
}

onMounted(() => {
  loadUsers()
  loadSessions()
})
</script>

<style lang="scss" scoped>
.toolbar { display: flex; align-items: center; gap: 12px; }
.users-section { margin-bottom: 16px; }
.sessions-section { }

.chat-container { display: flex; flex-direction: column; height: 100%; }
.chat-header { padding-bottom: 12px; border-bottom: 1px solid #eee; margin-bottom: 12px; .session-key { font-size: 13px; color: #606266; font-family: monospace; } }
.chat-messages { flex: 1; overflow-y: auto; max-height: calc(100vh - 200px); padding: 0 4px; }
.message-list { display: flex; flex-direction: column; gap: 16px; }
.message-item {
  display: flex; gap: 10px; align-items: flex-start;
  &.user { flex-direction: row-reverse; .message-body { align-items: flex-end; } }
  &.assistant { flex-direction: row; .message-body { align-items: flex-start; } }
  &.system { flex-direction: row; opacity: 0.6; }
}
.message-avatar {
  width: 36px; height: 36px; border-radius: 50%;
  background: #409eff; display: flex; align-items: center; justify-content: center; color: #fff; flex-shrink: 0;
  .el-icon { font-size: 18px; }
}
.message-body { max-width: 70%; display: flex; flex-direction: column; gap: 4px; }
.message-role { font-size: 11px; color: #909399; }
.message-content { background: #f4f4f5; border-radius: 8px; padding: 10px 14px; font-size: 14px; line-height: 1.6; color: #303133; white-space: pre-wrap; word-break: break-all; }
.message-time { font-size: 11px; color: #c0c4cc; }
</style>
