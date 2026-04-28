import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  batchSaveAdminContent,
  createAdminContent,
  deleteAdminContent,
  getAdminContentList,
  getAdminDashboard,
  sendAdminHeartbeat,
  updateAdminContent
} from '@/modules/admin/api/admin'

// 业务目的：集中管理后台管理台的仪表盘、内容列表、轮询状态和编辑动作。
// 业务逻辑：页面只和 Pinia 仓库交互，避免把轮询、缓存和错误处理散落到多个组件里。
export const useAdminConsoleStore = defineStore('adminConsole', () => {
  const dashboard = ref(null)
  const contentMap = ref({
    tech: [],
    world: [],
    ai: []
  })
  const loadingMap = ref({
    dashboard: false,
    tech: false,
    world: false,
    ai: false
  })
  const saving = ref(false)
  const errorMessage = ref('')
  const heartbeatTimer = ref(null)
  const dashboardTimer = ref(null)
  const clientId = ref(readOrCreateClientId())

  const currentSummary = computed(() => dashboard.value?.summary || null)
  const recentEdits = computed(() => dashboard.value?.recentEdits || [])
  const trendPoints = computed(() => dashboard.value?.trendPoints || [])
  const moduleStats = computed(() => dashboard.value?.moduleStats || [])
  const hotContents = computed(() => dashboard.value?.hotContents || [])
  const commentRecords = computed(() => dashboard.value?.commentRecords || [])

  async function loadDashboard() {
    loadingMap.value.dashboard = true
    errorMessage.value = ''
    try {
      dashboard.value = await getAdminDashboard()
    } catch (error) {
      errorMessage.value = error.message
      throw error
    } finally {
      loadingMap.value.dashboard = false
    }
  }

  async function heartbeat() {
    try {
      await sendAdminHeartbeat(clientId.value)
    } catch (error) {
      errorMessage.value = error.message
    }
  }

  async function loadContent(type) {
    loadingMap.value[type] = true
    errorMessage.value = ''
    try {
      contentMap.value[type] = await getAdminContentList(type)
      return contentMap.value[type]
    } catch (error) {
      errorMessage.value = error.message
      throw error
    } finally {
      loadingMap.value[type] = false
    }
  }

  async function saveContent(type, payload) {
    saving.value = true
    errorMessage.value = ''
    try {
      if (payload.id) {
        await updateAdminContent(type, payload.id, payload)
      } else {
        await createAdminContent(type, payload)
      }
      await Promise.all([loadContent(type), loadDashboard()])
    } catch (error) {
      errorMessage.value = error.message
      throw error
    } finally {
      saving.value = false
    }
  }

  async function batchImportContent(type, records) {
    saving.value = true
    errorMessage.value = ''
    try {
      await batchSaveAdminContent(type, records)
      await Promise.all([loadContent(type), loadDashboard()])
    } catch (error) {
      errorMessage.value = error.message
      throw error
    } finally {
      saving.value = false
    }
  }

  async function removeContent(type, contentKey) {
    saving.value = true
    errorMessage.value = ''
    try {
      await deleteAdminContent(type, contentKey)
      await Promise.all([loadContent(type), loadDashboard()])
    } catch (error) {
      errorMessage.value = error.message
      throw error
    } finally {
      saving.value = false
    }
  }

  async function refreshAll(type) {
    await heartbeat()
    if (type) {
      await Promise.all([loadDashboard(), loadContent(type)])
      return
    }
    await loadDashboard()
  }

  function startRealtime(type) {
    stopRealtime()
    heartbeat()
    loadDashboard().catch(() => null)
    if (type) {
      loadContent(type).catch(() => null)
    }
    heartbeatTimer.value = window.setInterval(() => {
      heartbeat()
    }, 15000)
    dashboardTimer.value = window.setInterval(() => {
      loadDashboard().catch(() => null)
      if (type) {
        loadContent(type).catch(() => null)
      }
    }, 20000)
  }

  function stopRealtime() {
    if (heartbeatTimer.value) {
      window.clearInterval(heartbeatTimer.value)
      heartbeatTimer.value = null
    }
    if (dashboardTimer.value) {
      window.clearInterval(dashboardTimer.value)
      dashboardTimer.value = null
    }
  }

  function getContentList(type) {
    return contentMap.value[type] || []
  }

  return {
    dashboard,
    contentMap,
    loadingMap,
    saving,
    errorMessage,
    currentSummary,
    recentEdits,
    trendPoints,
    moduleStats,
    hotContents,
    commentRecords,
    loadDashboard,
    loadContent,
    saveContent,
    batchImportContent,
    removeContent,
    refreshAll,
    getContentList,
    startRealtime,
    stopRealtime
  }
})

// 业务目的：为后台管理台生成稳定的浏览器端心跳标识，避免刷新页面后在线人数重复膨胀。
// 业务逻辑：优先复用本地缓存的 clientId，首次进入时再生成并持久化到 localStorage。
function readOrCreateClientId() {
  const storageKey = 'peakstars_admin_console_client_id'
  const existing = localStorage.getItem(storageKey)
  if (existing) {
    return existing
  }

  const created = typeof crypto !== 'undefined' && crypto.randomUUID
    ? crypto.randomUUID()
    : `admin-${Date.now()}`
  localStorage.setItem(storageKey, created)
  return created
}
