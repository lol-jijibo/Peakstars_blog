import { reactive, readonly } from 'vue'

const DEFAULT_COPY = {
  kicker: 'Peakstars Forum',
  title: '正在进入 PeakStars_blog',
  subtitle: '请稍候，系统正在同步页面内容，并展示当前进入进度。'
}

const MIN_VISIBLE_MS = 800
const PRELOAD_INTERVAL_MS = 50
const PRELOAD_TARGET = 85
const COMPLETE_INTERVAL_MS = 30
const COMPLETE_STEP_MIN = 2
const COMPLETE_STEP_MAX = 6
const HIDE_DELAY_MS = 150

const state = reactive({
  visible: false,
  progress: 0,
  statusText: '等待开始',
  kicker: DEFAULT_COPY.kicker,
  title: DEFAULT_COPY.title,
  subtitle: DEFAULT_COPY.subtitle
})

let progressTimer = 0
let completeTimer = 0
let hideTimer = 0
let loaderStartedAt = 0
let isFinishing = false

function clearTimers() {
  if (progressTimer) {
    window.clearInterval(progressTimer)
    progressTimer = 0
  }

  if (completeTimer) {
    window.clearInterval(completeTimer)
    completeTimer = 0
  }

  if (hideTimer) {
    window.clearTimeout(hideTimer)
    hideTimer = 0
  }
}

function resetState() {
  state.visible = false
  state.progress = 0
  state.statusText = '等待开始'
  loaderStartedAt = 0
  isFinishing = false
}

function getStatusText(progress) {
  if (progress <= 0) return '等待开始'
  if (progress < 28) return '正在初始化登录态...'
  if (progress < 62) return '正在同步页面内容与用户信息...'
  if (progress < 90) return '正在准备进入页面...'
  if (progress < 100) return '正在完成最后校验...'
  return '进入完成'
}

function updateProgress(nextProgress) {
  state.progress = Math.max(0, Math.min(100, nextProgress))
  state.statusText = getStatusText(state.progress)
}

function getPreloadStep(progress) {
  if (progress < 12) return 8
  if (progress < 28) return 6
  if (progress < 48) return 4
  if (progress < 68) return 3
  if (progress < 82) return 2
  return 1.5
}

function startCompletionProgress() {
  if (!state.visible) return

  if (completeTimer) {
    window.clearInterval(completeTimer)
    completeTimer = 0
  }

  completeTimer = window.setInterval(() => {
    const remaining = 100 - state.progress
    const step = Math.min(
      COMPLETE_STEP_MAX,
      Math.max(COMPLETE_STEP_MIN, Math.ceil(remaining / 6))
    )
    const nextProgress = Math.min(100, state.progress + step)

    updateProgress(nextProgress)

    if (nextProgress >= 100) {
      window.clearInterval(completeTimer)
      completeTimer = 0

      hideTimer = window.setTimeout(() => {
        resetState()
      }, HIDE_DELAY_MS)
    }
  }, COMPLETE_INTERVAL_MS)
}

export function startEntryTransitionLoader(copy = {}) {
  clearTimers()
  state.kicker = copy.kicker || DEFAULT_COPY.kicker
  state.title = copy.title || DEFAULT_COPY.title
  state.subtitle = copy.subtitle || DEFAULT_COPY.subtitle
  state.visible = true
  loaderStartedAt = Date.now()
  isFinishing = false
  updateProgress(0)

  progressTimer = window.setInterval(() => {
    if (isFinishing) return

    const nextProgress = Math.min(
      PRELOAD_TARGET,
      state.progress + getPreloadStep(state.progress)
    )

    updateProgress(nextProgress)
  }, PRELOAD_INTERVAL_MS)
}

export function finishEntryTransitionLoader() {
  if (!state.visible || isFinishing) return

  isFinishing = true

  if (progressTimer) {
    window.clearInterval(progressTimer)
    progressTimer = 0
  }

  const elapsed = Date.now() - loaderStartedAt
  const waitMs = Math.max(0, MIN_VISIBLE_MS - elapsed)

  hideTimer = window.setTimeout(() => {
    startCompletionProgress()
  }, waitMs)
}

export function stopEntryTransitionLoader() {
  clearTimers()
  resetState()
}

export const entryTransitionLoaderState = readonly(state)
