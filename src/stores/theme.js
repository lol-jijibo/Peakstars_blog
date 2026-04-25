import { computed, reactive, readonly } from 'vue'

const STORAGE_KEY = 'peakstars_blog_theme'

const state = reactive({
  theme: 'light',
  initialized: false
})

function resolvePreferredTheme() {
  const storedTheme = localStorage.getItem(STORAGE_KEY)

  if (storedTheme === 'light' || storedTheme === 'dark') {
    return storedTheme
  }

  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function applyTheme(theme) {
  state.theme = theme
  document.documentElement.dataset.theme = theme
  document.body.dataset.theme = theme
  document.documentElement.style.colorScheme = theme
  localStorage.setItem(STORAGE_KEY, theme)
}

export function initTheme() {
  if (state.initialized) {
    applyTheme(state.theme)
    return
  }

  state.initialized = true
  applyTheme(resolvePreferredTheme())
}

export function useThemeStore() {
  return {
    state: readonly(state),
    isDark: computed(() => state.theme === 'dark'),
    toggleTheme() {
      applyTheme(state.theme === 'dark' ? 'light' : 'dark')
    }
  }
}
