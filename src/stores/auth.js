import { computed, reactive, readonly } from 'vue'

// 认证服务基础地址。开发环境默认走 Vite 代理的 /auth-api。
const AUTH_BASE_URL = trimTrailingSlash(import.meta.env.VITE_AUTH_API_BASE_URL || '/auth-api')
// 给认证请求兜一个超时，避免后端卡住时前端一直处于 loading 状态。
const REQUEST_TIMEOUT_MS = 15000

// 统一管理本地存储 key，避免在多个页面里散落硬编码字符串。
const STORAGE_KEYS = {
  accessToken: 'interview_demo_access_token',
  currentUser: 'interview_demo_current_user',
  rememberedAccount: 'interview_demo_remembered_account'
}

// 安全读取 localStorage 中的 JSON，解析失败时回退到默认值。
function readJSON(key, fallback) {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : fallback
  } catch {
    return fallback
  }
}

function writeJSON(key, value) {
  localStorage.setItem(key, JSON.stringify(value))
}

// 登录/注册接口里统一把邮箱规范化，避免大小写和空格影响匹配。
function normalizeEmail(email) {
  return email.trim().toLowerCase()
}

// 去掉接口基础地址末尾的 /，方便后续和路径拼接。
function trimTrailingSlash(url) {
  return url.endsWith('/') ? url.slice(0, -1) : url
}

// 前端只保留页面展示和鉴权所需的用户字段。
function sanitizeUser(user) {
  if (!user) return null
  return {
    id: user.id,
    username: user.username,
    email: user.email,
    joinedAt: user.joinedAt
  }
}

// 把浏览器层面的异常转成更容易排查的提示语。
function toAuthRequestError(error) {
  if (error?.name === 'AbortError') {
    return new Error('认证服务请求超时，请检查后端、Redis 或 SMTP 配置')
  }

  if (error instanceof TypeError) {
    return new Error(`认证服务连接失败，请确认 auth 服务已启动: ${AUTH_BASE_URL}`)
  }

  return error
}

// 认证服务的通用请求方法。
// 这里统一处理：
// 1. 超时中断
// 2. code/message/data 结构
// 3. 浏览器层面的网络异常
async function requestAuth(url, options = {}) {
  const controller = new AbortController()
  const timeoutId = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS)

  try {
    const response = await fetch(`${AUTH_BASE_URL}${url}`, {
      headers: {
        'Content-Type': 'application/json',
        ...(options.headers || {})
      },
      signal: controller.signal,
      ...options
    })

    const payload = await response.json()
    if (!response.ok || payload.code !== 0) {
      throw new Error(payload.message || '认证请求失败')
    }

    return payload
  } catch (error) {
    throw toAuthRequestError(error)
  } finally {
    window.clearTimeout(timeoutId)
  }
}

// 整个项目共享的认证状态。
const state = reactive({
  accessToken: localStorage.getItem(STORAGE_KEYS.accessToken) || '',
  user: readJSON(STORAGE_KEYS.currentUser, null),
  rememberedAccount: localStorage.getItem(STORAGE_KEYS.rememberedAccount) || ''
})

// 持久化当前用户信息，供刷新后恢复登录态使用。
function persistCurrentUser() {
  if (state.user) {
    writeJSON(STORAGE_KEYS.currentUser, state.user)
  } else {
    localStorage.removeItem(STORAGE_KEYS.currentUser)
  }
}

// 持久化 accessToken，路由守卫和后续请求都会依赖它。
function persistAccessToken() {
  if (state.accessToken) {
    localStorage.setItem(STORAGE_KEYS.accessToken, state.accessToken)
  } else {
    localStorage.removeItem(STORAGE_KEYS.accessToken)
  }
}

// 处理“记住我”功能对应的账号缓存。
function setRememberedAccount(account, remember) {
  if (remember && account.trim()) {
    state.rememberedAccount = account.trim()
    localStorage.setItem(STORAGE_KEYS.rememberedAccount, state.rememberedAccount)
    return
  }

  state.rememberedAccount = ''
  localStorage.removeItem(STORAGE_KEYS.rememberedAccount)
}

// 发送注册验证码。
// 成功后返回冷却时间和验证码有效期，供注册页倒计时使用。
async function sendRegisterEmailCode(email) {
  try {
    const response = await requestAuth('/api/auth/register/email-code/send', {
      method: 'POST',
      body: JSON.stringify({
        email: normalizeEmail(email)
      })
    })

    return {
      success: true,
      message: response.message,
      expiresInSeconds: response.data?.expiresInSeconds ?? 300,
      cooldownSeconds: response.data?.cooldownSeconds ?? 60
    }
  } catch (error) {
    return {
      success: false,
      message: error.message
    }
  }
}

// 注册账号。这里会把邮箱验证码一起提交给后端校验。
async function register(params) {
  try {
    const response = await requestAuth('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify({
        email: normalizeEmail(params.email),
        password: params.password,
        passwordConfirm: params.passwordConfirm,
        emailCode: params.emailCode,
        username: params.username || ''
      })
    })

    return {
      success: true,
      message: response.message,
      user: sanitizeUser(response.data?.user || null)
    }
  } catch (error) {
    return { success: false, message: error.message }
  }
}

// 登录账号。成功后同步更新 token、当前用户和“记住我”信息。
async function login(params) {
  try {
    const response = await requestAuth('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({
        account: params.account.trim(),
        password: params.password,
        remember: Boolean(params.remember)
      })
    })

    state.accessToken = response.data?.token || ''
    state.user = sanitizeUser(response.data?.user || null)
    persistAccessToken()
    persistCurrentUser()
    setRememberedAccount(params.account, params.remember)

    return {
      success: true,
      message: response.message,
      token: state.accessToken,
      user: state.user
    }
  } catch (error) {
    return { success: false, message: error.message }
  }
}

// 退出登录时清空本地登录态。
function logout() {
  state.accessToken = ''
  state.user = null
  persistAccessToken()
  persistCurrentUser()
}

// 登录页初始化时读取“记住我”的账号。
function getRemembered() {
  return state.rememberedAccount
}

// 忘记密码入口，当前用于登录页弹层调用。
async function sendResetEmail(email) {
  try {
    const response = await requestAuth('/api/auth/forgot-password', {
      method: 'POST',
      body: JSON.stringify({
        email: normalizeEmail(email)
      })
    })

    return {
      success: true,
      message: response.message
    }
  } catch (error) {
    return {
      success: false,
      message: error.message
    }
  }
}

// 给路由守卫使用的轻量鉴权判断。
export function isAuthenticated() {
  return Boolean(state.accessToken && state.user)
}

// 暴露给页面使用的 auth store。
export function useAuthStore() {
  return {
    state: readonly(state),
    currentUser: computed(() => state.user),
    accessToken: computed(() => state.accessToken),
    rememberedAccount: computed(() => state.rememberedAccount),
    isLoggedIn: computed(() => Boolean(state.accessToken && state.user)),
    getRemembered,
    sendResetEmail,
    sendRegisterEmailCode,
    register,
    login,
    logout
  }
}
