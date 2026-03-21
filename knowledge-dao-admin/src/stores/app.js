import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const activeMenu = ref('dashboard')

  const toggleSidebar = () => {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  const setActiveMenu = (menu) => {
    activeMenu.value = menu
  }

  return { sidebarCollapsed, activeMenu, toggleSidebar, setActiveMenu }
})
