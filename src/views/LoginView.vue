<template>
  <div class="login-page">
    <div class="bg-particles">
      <span
        v-for="dot in particleDots"
        :key="dot.id"
        class="particle"
        :style="dot.style"
      ></span>
    </div>
    <canvas ref="canvasRef" class="particle-canvas"></canvas>
    <div class="orb orb-1"></div>
    <div class="orb orb-2"></div>

    <transition name="toast">
      <div v-if="toast.visible" class="toast" :class="toast.type">{{ toast.message }}</div>
    </transition>

    <transition name="modal">
      <div v-if="showForgot" class="modal-overlay" @click.self="closeForgot">
        <div class="modal">
          <h3>重置密码</h3>
          <p>请输入注册邮箱，我们会将重置链接发送到你的邮箱。</p>

          <label class="field">
            <span>邮箱</span>
            <div class="input-wrap" :class="{ invalid: forgotError }">
              <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="2" y="4" width="20" height="16" rx="3" />
                <path d="m2 7 10 7 10-7" />
              </svg>
              <input v-model.trim="forgotEmail" type="email" placeholder="请输入邮箱" />
            </div>
            <small class="field-msg err">{{ forgotError }}</small>
          </label>

          <div class="modal-actions">
            <button class="btn-ghost" type="button" @click="closeForgot">取消</button>
            <button class="btn-primary" type="button" :disabled="forgotLoading" @click="sendForgot">
              {{ forgotLoading ? '发送中...' : '发送链接' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <main class="card">
      <div class="brand">
        <div class="brand-icon">
          <img class="brand-icon-image" src="/peakstars-blog-icon.jpg" alt="PeakStars_blog" />
        </div>
        <div class="brand-name">PeakStars_blog</div>
        <div class="brand-sub">欢迎回来，请登录您的账号</div>
      </div>

      <div class="tabs">
        <button class="tab-btn" :class="{ active: activeTab === 'login' }" type="button" @click="switchTab('login')">登录</button>
        <button class="tab-btn" :class="{ active: activeTab === 'register' }" type="button" @click="switchTab('register')">注册</button>
      </div>

      <transition name="panel" mode="out-in">
        <form v-if="activeTab === 'login'" key="login" @submit.prevent="submitLogin">
          <label class="field">
            <span>邮箱</span>
            <div class="input-wrap" :class="{ invalid: loginErrors.email }">
              <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="2" y="4" width="20" height="16" rx="3" />
                <path d="m2 7 10 7 10-7" />
              </svg>
              <input
                v-model.trim="loginForm.email"
                type="email"
                placeholder="请输入邮箱"
                autocomplete="email"
                @blur="validateLoginEmail"
              />
            </div>
            <small class="field-msg err">{{ loginErrors.email }}</small>
          </label>

          <label class="field">
            <span>密码</span>
            <div class="input-wrap" :class="{ invalid: loginErrors.password }">
              <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" />
                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
              </svg>
              <input
                v-model="loginForm.password"
                :type="showLoginPwd ? 'text' : 'password'"
                placeholder="请输入密码"
                autocomplete="current-password"
                @blur="validateLoginPassword"
              />
              <button type="button" class="eye-toggle" @click="showLoginPwd = !showLoginPwd">
                <svg v-if="!showLoginPwd" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                  <circle cx="12" cy="12" r="3" />
                </svg>
                <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
                  <line x1="1" y1="1" x2="23" y2="23" />
                </svg>
              </button>
            </div>
            <small class="field-msg err">{{ loginErrors.password }}</small>
          </label>

          <div class="options-row">
            <label class="checkbox-label">
              <input v-model="loginForm.remember" type="checkbox" />
              <span class="custom-check">
                <svg viewBox="0 0 12 10" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="1 5 4 9 11 1" />
                </svg>
              </span>
              记住我
            </label>
            <a href="#" class="forgot-link" @click.prevent="showForgot = true">忘记密码？</a>
          </div>

          <button class="btn-submit" type="submit" :disabled="loginLoading">
            {{ loginLoading ? '登录中...' : '登录' }}
          </button>

          <div class="divider">或通过以下方式登录</div>

          <div class="social-row">
            <button class="social-btn" type="button" @click="socialHint('GitHub')">
              <span class="social-icon">
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2A10 10 0 0 0 2 12c0 4.42 2.87 8.17 6.84 9.5.5.08.66-.23.66-.5v-1.69c-2.77.6-3.36-1.34-3.36-1.34-.46-1.16-1.11-1.47-1.11-1.47-.91-.62.07-.6.07-.6 1 .07 1.53 1.03 1.53 1.03.87 1.52 2.34 1.07 2.91.83.09-.65.35-1.09.63-1.34-2.22-.25-4.55-1.11-4.55-4.92 0-1.11.38-2 1.03-2.71-.1-.25-.45-1.29.1-2.64 0 0 .84-.27 2.75 1.02.79-.22 1.65-.33 2.5-.33.85 0 1.71.11 2.5.33 1.91-1.29 2.75-1.02 2.75-1.02.55 1.35.2 2.39.1 2.64.65.71 1.03 1.6 1.03 2.71 0 3.82-2.34 4.66-4.57 4.91.36.31.69.92.69 1.85V21c0 .27.16.59.67.5C19.14 20.16 22 16.42 22 12A10 10 0 0 0 12 2z" />
                </svg>
              </span>
              GitHub
            </button>
            <button class="social-btn" type="button" @click="socialHint('Google')">
              <span class="social-icon">
                <svg viewBox="0 0 24 24">
                  <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                  <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                  <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
                  <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
                </svg>
              </span>
              Google
            </button>
            <button class="social-btn" type="button" @click="socialHint('微信')">
              <span class="social-icon">
                <svg viewBox="0 0 24 24" fill="#07C160">
                  <path d="M8.69 5C4.9 5 2 7.74 2 11.2c0 1.97 1 3.73 2.62 4.9l-.6 2 2.16-1.12c.69.2 1.43.3 2.19.3.22 0 .44-.01.66-.03A5.34 5.34 0 0 1 9 16.5c0-3.04 2.7-5.5 6-5.5.2 0 .4.01.6.03C15.06 8.4 12.19 5 8.69 5zm-1.97 3.6a.98.98 0 1 1 0 1.96.98.98 0 0 1 0-1.96zm3.95 0a.98.98 0 1 1 0 1.96.98.98 0 0 1 0-1.96zM15 12c-3.08 0-5 1.9-5 4.5S11.92 21 15 21c.65 0 1.27-.09 1.85-.26l1.74.9-.48-1.6C19.36 19.3 20 18.02 20 16.5 20 13.9 18.08 12 15 12zm-1.5 2.25a.75.75 0 1 1 0 1.5.75.75 0 0 1 0-1.5zm3 0a.75.75 0 1 1 0 1.5.75.75 0 0 1 0-1.5z" />
                </svg>
              </span>
              微信
            </button>
          </div>

          <div class="card-footer">
            还没有账号？
            <a href="#" @click.prevent="switchTab('register')">立即注册</a>
          </div>
        </form>

        <form v-else key="register" @submit.prevent="submitRegister">
          <label class="field">
            <span>邮箱</span>
            <div class="input-wrap" :class="{ invalid: registerErrors.email }">
              <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="2" y="4" width="20" height="16" rx="3" />
                <path d="m2 7 10 7 10-7" />
              </svg>
              <input
                v-model.trim="registerForm.email"
                type="email"
                placeholder="请输入邮箱"
                autocomplete="email"
                @blur="validateRegisterEmail"
              />
            </div>
            <small class="field-msg err">{{ registerErrors.email }}</small>
          </label>

          <div class="qq-verify-box" :class="{ ready: qqVerification.sent }">
            <div class="qq-verify-head">
              <div>
                <div class="qq-verify-title">QQ 邮箱验证</div>
                <div class="qq-verify-subtitle">邮箱格式正确后，发送验证码并前往 QQ 邮箱查收。</div>
              </div>
              <span v-if="qqVerification.sent" class="qq-verify-badge">已发送</span>
            </div>

            <div class="qq-verify-row">
              <div class="qq-verify-input input-wrap" :class="{ invalid: registerErrors.qqCode }">
                <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="5" width="18" height="14" rx="3" />
                  <path d="M7 9h10" />
                  <path d="M7 13h6" />
                </svg>
                <input
                  v-model.trim="qqVerification.code"
                  type="text"
                  maxlength="6"
                  inputmode="numeric"
                  placeholder="请输入 6 位验证码"
                  @blur="validateRegisterQqCode"
                />
              </div>

              <button
                class="qq-verify-btn"
                type="button"
                :disabled="qqVerification.loading || qqVerification.cooldown > 0"
                @click="sendQqVerificationCode"
              >
                {{ qqVerificationButtonText }}
              </button>
            </div>

            <small class="field-msg err">{{ registerErrors.qqCode }}</small>
          </div>

          <label class="field">
            <span>密码</span>
            <div class="input-wrap" :class="{ invalid: registerErrors.password }">
              <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" />
                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
              </svg>
              <input
                v-model="registerForm.password"
                :type="showRegisterPwd ? 'text' : 'password'"
                placeholder="创建密码"
                autocomplete="new-password"
                @blur="validateRegisterPassword"
              />
              <button type="button" class="eye-toggle" @click="showRegisterPwd = !showRegisterPwd">
                <svg v-if="!showRegisterPwd" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                  <circle cx="12" cy="12" r="3" />
                </svg>
                <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
                  <line x1="1" y1="1" x2="23" y2="23" />
                </svg>
              </button>
            </div>
            <div class="strength-bar">
              <span v-for="index in 4" :key="index" class="strength-seg" :style="{ background: index <= passwordScore ? strengthColor : 'rgba(255,255,255,.12)' }"></span>
            </div>
            <div class="strength-label" :style="{ color: strengthColor }">{{ strengthLabel }}</div>
            <small class="field-msg err">{{ registerErrors.password }}</small>
          </label>

          <label class="field">
            <span>确认密码</span>
            <div class="input-wrap" :class="{ invalid: registerErrors.confirmPassword }">
              <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" />
                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
              </svg>
              <input
                v-model="registerForm.confirmPassword"
                :type="showConfirmPwd ? 'text' : 'password'"
                placeholder="再次输入密码"
                autocomplete="new-password"
                @blur="validateRegisterConfirm"
              />
              <button type="button" class="eye-toggle" @click="showConfirmPwd = !showConfirmPwd">
                <svg v-if="!showConfirmPwd" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                  <circle cx="12" cy="12" r="3" />
                </svg>
                <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
                  <line x1="1" y1="1" x2="23" y2="23" />
                </svg>
              </button>
            </div>
            <small class="field-msg err">{{ registerErrors.confirmPassword }}</small>
          </label>

          <button class="btn-submit" type="submit" :disabled="registerLoading">
            {{ registerLoading ? '注册中...' : '注册' }}
          </button>

          <div class="card-footer register-footer">
            已有账号？
            <a href="#" @click.prevent="switchTab('login')">立即登录</a>
          </div>
        </form>
      </transition>
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  startEntryTransitionLoader,
  stopEntryTransitionLoader
} from '@/stores/entryLoader'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const remembered = authStore.getRemembered()
const activeTab = ref('login')
const showLoginPwd = ref(false)
const showRegisterPwd = ref(false)
const showConfirmPwd = ref(false)
const showForgot = ref(false)
const forgotLoading = ref(false)
const loginLoading = ref(false)
const registerLoading = ref(false)
const forgotEmail = ref('')
const forgotError = ref('')
const canvasRef = ref(null)

const loginForm = reactive({
  email: remembered,
  password: '',
  remember: Boolean(remembered)
})

const registerForm = reactive({
  email: '',
  password: '',
  confirmPassword: ''
})

const qqVerification = reactive({
  code: '',
  sent: false,
  loading: false,
  cooldown: 0,
  sentTo: ''
})

const loginErrors = reactive({
  email: '',
  password: ''
})

const registerErrors = reactive({
  email: '',
  password: '',
  confirmPassword: '',
  qqCode: ''
})

const toast = reactive({
  visible: false,
  message: '',
  type: 'ok'
})

const particleDots = Array.from({ length: 36 }, (_, index) => {
  const size = Math.random() * 4 + 2
  const opacity = Math.random() * 0.5 + 0.25
  const colors = ['rgba(108,99,255,', 'rgba(90,200,250,', 'rgba(167,139,250,', 'rgba(255,255,255,']
  return {
    id: index,
    style: {
      width: `${size}px`,
      height: `${size}px`,
      left: `${Math.random() * 100}%`,
      animationDuration: `${Math.random() * 10 + 10}s`,
      animationDelay: `${Math.random() * 8}s`,
      background: `${colors[Math.floor(Math.random() * colors.length)]}${opacity})`
    }
  }
})

const passwordScore = computed(() => {
  let score = 0
  if (registerForm.password.length >= 6) score += 1
  if (/[A-Za-z]/.test(registerForm.password) && /\d/.test(registerForm.password)) score += 1
  if (/[A-Z]/.test(registerForm.password) && /[a-z]/.test(registerForm.password)) score += 1
  if (/[^A-Za-z0-9]/.test(registerForm.password)) score += 1
  return score
})

const strengthColor = computed(() => ['', '#ff6b6b', '#faad14', '#5bfcb4', '#6c63ff'][passwordScore.value] || '')
const strengthLabel = computed(() => ['', '密码较弱', '密码一般', '密码较强', '密码很强'][passwordScore.value] || '')
const qqVerificationButtonText = computed(() => {
  if (qqVerification.loading) return '发送中...'
  if (qqVerification.cooldown > 0) return `${qqVerification.cooldown}s 后重发`
  return qqVerification.sent ? '重新发送' : '发送验证码'
})

let toastTimer = null
let frameId = 0
let resizeHandler = null
let qqCooldownTimer = 0
let points = []
const bodyOverflow = { html: '', body: '' }

function validateEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)
}

function normalizeEmail(value) {
  return value.trim().toLowerCase()
}

function resolveRedirectPath() {
  return typeof route.query.redirect === 'string' ? route.query.redirect : '/home'
}

function isQqEmail(value) {
  return /@qq\.com$/i.test(value.trim())
}

function showToast(message, type = 'ok') {
  toast.visible = true
  toast.message = message
  toast.type = type
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toast.visible = false
  }, 2600)
}

function clearQqCooldownTimer() {
  if (qqCooldownTimer) {
    window.clearInterval(qqCooldownTimer)
    qqCooldownTimer = 0
  }
}

function startQqCooldown(seconds) {
  clearQqCooldownTimer()
  qqVerification.cooldown = Math.max(0, Number(seconds) || 0)

  if (qqVerification.cooldown <= 0) return

  qqCooldownTimer = window.setInterval(() => {
    if (qqVerification.cooldown <= 1) {
      qqVerification.cooldown = 0
      clearQqCooldownTimer()
      return
    }

    qqVerification.cooldown -= 1
  }, 1000)
}

function resetQqVerificationState() {
  qqVerification.code = ''
  qqVerification.sent = false
  qqVerification.loading = false
  qqVerification.sentTo = ''
  startQqCooldown(0)
}

function switchTab(tab) {
  activeTab.value = tab
  loginErrors.email = ''
  loginErrors.password = ''
  registerErrors.email = ''
  registerErrors.password = ''
  registerErrors.confirmPassword = ''
  registerErrors.qqCode = ''
  resetQqVerificationState()
}

function validateLoginEmail() {
  loginErrors.email = validateEmail(loginForm.email) ? '' : '请输入正确的邮箱地址'
  return !loginErrors.email
}

function validateLoginPassword() {
  loginErrors.password = loginForm.password.length >= 6 ? '' : '请输入不少于 6 位的密码'
  return !loginErrors.password
}

function validateRegisterEmail() {
  registerErrors.email = validateEmail(registerForm.email) ? '' : '请输入正确的邮箱地址'
  return !registerErrors.email
}

function validateRegisterQqCode() {
  if (!qqVerification.code) {
    registerErrors.qqCode = ''
    return true
  }

  registerErrors.qqCode = /^\d{6}$/.test(qqVerification.code) ? '' : '请输入 6 位验证码'
  return !registerErrors.qqCode
}

function validateRegisterQqCodeRequired() {
  if (!qqVerification.code) {
    registerErrors.qqCode = '请输入邮箱验证码'
    return false
  }

  if (qqVerification.sentTo !== normalizeEmail(registerForm.email)) {
    registerErrors.qqCode = '邮箱已变更，请重新发送验证码'
    return false
  }

  return validateRegisterQqCode()
}

function validateRegisterPassword() {
  if (registerForm.password.length < 6) {
    registerErrors.password = '密码不能少于 6 位'
    return false
  }
  if (passwordScore.value < 2) {
    registerErrors.password = '密码强度不足，请加入字母和数字'
    return false
  }
  registerErrors.password = ''
  return true
}

function validateRegisterConfirm() {
  registerErrors.confirmPassword = registerForm.password === registerForm.confirmPassword ? '' : '两次输入的密码不一致'
  return !registerErrors.confirmPassword
}

async function sendQqVerificationCode() {
  if (!validateRegisterEmail()) {
    showToast('请先输入正确的邮箱地址', 'err')
    return
  }

  if (!isQqEmail(registerForm.email)) {
    registerErrors.email = '当前仅支持 QQ 邮箱接收验证码'
    showToast('请输入 QQ 邮箱地址', 'err')
    return
  }

  registerErrors.email = ''
  registerErrors.qqCode = ''
  qqVerification.loading = true

  const result = await authStore.sendRegisterEmailCode(registerForm.email)
  qqVerification.loading = false

  if (!result.success) {
    showToast(result.message, 'err')
    return
  }

  qqVerification.sent = true
  qqVerification.sentTo = normalizeEmail(registerForm.email)
  startQqCooldown(result.cooldownSeconds)
  showToast(result.message, 'ok')
}

async function submitLogin() {
  const emailValid = validateLoginEmail()
  const passwordValid = validateLoginPassword()
  if (!emailValid || !passwordValid) return

  loginLoading.value = true
  const result = await authStore.login({
    account: loginForm.email,
    password: loginForm.password,
    remember: loginForm.remember
  })
  loginLoading.value = false

  if (!result.success) {
    loginErrors.password = result.message
    showToast(result.message, 'err')
    return
  }

  showToast(result.message, 'ok')
  const redirect = resolveRedirectPath()
  setTimeout(async () => {
    try {
      
        startEntryTransitionLoader({
          title: '正在进入 PeakStars_blog',
          subtitle: '登录成功，系统正在同步你的页面内容与登录状态。'
        })
      

      await router.replace(redirect)
    } catch {
      stopEntryTransitionLoader()
    }
  }, 320)
}

async function submitRegister() {
  const emailValid = validateRegisterEmail()
  const codeValid = validateRegisterQqCodeRequired()
  const passwordValid = validateRegisterPassword()
  const confirmValid = validateRegisterConfirm()
  if (!emailValid || !codeValid || !passwordValid || !confirmValid) return

  registerLoading.value = true
  const result = await authStore.register({
    email: registerForm.email,
    password: registerForm.password,
    passwordConfirm: registerForm.confirmPassword,
    emailCode: qqVerification.code
  })
  registerLoading.value = false

  if (!result.success) {
    if (result.message.includes('验证码')) {
      registerErrors.qqCode = result.message
    }
    showToast(result.message, 'err')
    return
  }

  loginForm.email = registerForm.email
  loginForm.password = registerForm.password
  loginForm.remember = true
  registerForm.email = ''
  registerForm.password = ''
  registerForm.confirmPassword = ''
  resetQqVerificationState()
  showToast(result.message, 'ok')
  switchTab('login')
}

function socialHint(name) {
  showToast(`${name} 登录暂未接入`, 'ok')
}

function closeForgot() {
  showForgot.value = false
  forgotEmail.value = ''
  forgotError.value = ''
}

function sendForgot() {
  forgotError.value = validateEmail(forgotEmail.value) ? '' : '请输入正确的邮箱地址'
  if (forgotError.value) return

  forgotLoading.value = true
  authStore.sendResetEmail(forgotEmail.value).then((result) => {
    forgotLoading.value = false
    closeForgot()
    showToast(result.message, result.success ? 'ok' : 'err')
  })
}

function initCanvas() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const createPoints = () => {
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight
    points = Array.from({ length: 34 }, () => ({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      vx: (Math.random() - 0.5) * 0.55,
      vy: (Math.random() - 0.5) * 0.55
    }))
  }

  const draw = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    points.forEach((point) => {
      point.x += point.vx
      point.y += point.vy
      if (point.x <= 0 || point.x >= canvas.width) point.vx *= -1
      if (point.y <= 0 || point.y >= canvas.height) point.vy *= -1

      ctx.beginPath()
      ctx.arc(point.x, point.y, 1.5, 0, Math.PI * 2)
      ctx.fillStyle = 'rgba(255,255,255,0.55)'
      ctx.fill()
    })

    for (let i = 0; i < points.length; i += 1) {
      for (let j = i + 1; j < points.length; j += 1) {
        const dx = points[i].x - points[j].x
        const dy = points[i].y - points[j].y
        const distance = Math.sqrt(dx * dx + dy * dy)
        if (distance < 150) {
          ctx.beginPath()
          ctx.moveTo(points[i].x, points[i].y)
          ctx.lineTo(points[j].x, points[j].y)
          ctx.strokeStyle = `rgba(108,99,255,${(1 - distance / 150) * 0.18})`
          ctx.lineWidth = 1
          ctx.stroke()
        }
      }
    }

    frameId = window.requestAnimationFrame(draw)
  }

  createPoints()
  draw()

  resizeHandler = () => {
    window.cancelAnimationFrame(frameId)
    createPoints()
    draw()
  }
  window.addEventListener('resize', resizeHandler)
}

watch(() => registerForm.email, (nextEmail) => {
  const normalizedNextEmail = normalizeEmail(nextEmail || '')
  if (!normalizedNextEmail) {
    resetQqVerificationState()
    return
  }

  if (qqVerification.sentTo && qqVerification.sentTo !== normalizedNextEmail) {
    resetQqVerificationState()
  }
})

onMounted(() => {
  bodyOverflow.html = document.documentElement.style.overflow
  bodyOverflow.body = document.body.style.overflow
  document.documentElement.style.overflow = 'hidden'
  document.body.style.overflow = 'hidden'
  initCanvas()
})

onBeforeUnmount(() => {
  clearTimeout(toastTimer)
  clearQqCooldownTimer()
  window.cancelAnimationFrame(frameId)
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
  document.documentElement.style.overflow = bodyOverflow.html
  document.body.style.overflow = bodyOverflow.body
})
</script>

<style src="../styles/views/LoginView.css"></style>
