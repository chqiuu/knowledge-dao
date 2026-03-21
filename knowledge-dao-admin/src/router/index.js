import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard/index.vue'),
    meta: { title: '驾驶舱看板' }
  },
  {
    path: '/knowledge',
    name: 'Knowledge',
    component: () => import('@/views/Knowledge/index.vue'),
    meta: { title: '知识管理' }
  },
  {
    path: '/retrieval',
    name: 'Retrieval',
    component: () => import('@/views/Retrieval/index.vue'),
    meta: { title: '检索中心' }
  },
  {
    path: '/sessions',
    name: 'Sessions',
    component: () => import('@/views/Sessions/index.vue'),
    meta: { title: '用户会话' }
  },
  {
    path: '/monitor',
    name: 'Monitor',
    component: () => import('@/views/Monitor/index.vue'),
    meta: { title: '系统监控' }
  },
  {
    path: '/config',
    name: 'Config',
    component: () => import('@/views/Config/index.vue'),
    meta: { title: '配置中心' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
