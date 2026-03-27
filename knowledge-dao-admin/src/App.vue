<template>
  <el-container class="app-container">
    <el-aside :width="collapsed ? '64px' : '220px'" class="app-aside">
      <div class="logo">
        <span v-if="!collapsed">Knowledge DAO</span>
        <span v-else>KD</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        :router="true"
        class="app-menu"
        background-color="#1a1a2e"
        text-color="#a0a5b8"
        active-text-color="#409eff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>驾驶舱看板</template>
        </el-menu-item>
        <el-menu-item index="/knowledge">
          <el-icon><Document /></el-icon>
          <template #title>知识管理</template>
        </el-menu-item>
        <el-menu-item index="/retrieval">
          <el-icon><Search /></el-icon>
          <template #title>检索中心</template>
        </el-menu-item>
        <el-menu-item index="/sessions">
          <el-icon><ChatLineSquare /></el-icon>
          <template #title>用户会话</template>
        </el-menu-item>
        <el-menu-item index="/monitor">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>系统监控</template>
        </el-menu-item>
        <el-menu-item index="/config">
          <el-icon><Setting /></el-icon>
          <template #title>配置中心</template>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="app-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="toggleCollapse"><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
        </div>
        <div class="header-right">
          <span class="system-time">{{ currentTime }}</span>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { storeToRefs } from 'pinia'

const router = useRouter()
const appStore = useAppStore()
const { sidebarCollapsed: collapsed } = storeToRefs(appStore)

const currentTime = ref('')
let timer = null

const toggleCollapse = () => appStore.toggleSidebar()

const activeMenu = computed(() => router.currentRoute.value.path)

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', { hour12: false })
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style lang="scss" scoped>
.app-container {
  height: 100vh;
  overflow: hidden;
}

.app-aside {
  background: #1a1a2e;
  transition: width 0.3s;
  overflow-x: hidden;
  overflow-y: auto;

  .logo {
    height: 60px;
    line-height: 60px;
    text-align: center;
    color: #409eff;
    font-size: 18px;
    font-weight: bold;
    border-bottom: 1px solid #2a2a4e;
    letter-spacing: 2px;
  }

  .app-menu {
    border-right: none;
  }
}

.app-header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 16px;

  .collapse-btn {
    font-size: 20px;
    cursor: pointer;
    color: #606266;
  }

  .system-time {
    color: #909399;
    font-size: 13px;
  }
}

.app-main {
  background: #f5f7fa;
  padding: 16px;
  overflow-y: auto;
}
</style>
