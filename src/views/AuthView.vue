<template>
  <div class="auth-page">
    <div class="auth-shell">
      <section class="hero-panel">
        <p class="eyebrow">Interview Demo</p>
        <h1>先注册并登录，再进入 PeakStars_blog</h1>
        <p class="hero-copy">
          你的浏览入口现在被登录态接管了。完成注册或登录后，我们会自动带你进入原本想访问的面经页面。
        </p>
        <div class="hero-points">
          <span>本地账号保存</span>
          <span>支持邮箱或用户名登录</span>
          <span>进入面经前统一校验</span>
        </div>
      </section>

      <section class="auth-card">
        <div class="card-head">
          <div class="brand">PeakStars_blog</div>
          <p class="card-subtitle">登录功能已经从 `stars-blog` 的思路迁进来了，这里先完成身份校验。</p>
        </div>

        <div class="tab-row">
          <button
            type="button"
            class="tab-btn"
            :class="{ active: activeTab === 'login' }"
            @click="switchTab('login')"
          >
            登录
          </button>
          <button
            type="button"
            class="tab-btn"
            :class="{ active: activeTab === 'register' }"
            @click="switchTab('register')"
          >
            注册
          </button>
        </div>

        <div v-if="notice.message" class="notice" :class="notice.type">
          {{ notice.message }}
        </div>

        <form v-if="activeTab === 'login'" class="form" @submit.prevent="handleLogin">
          <label class="field">
            <span>邮箱或用户名</span>
            <input
              v-model.trim="loginForm.account"
              type="text"
              placeholder="输入注册时填写的邮箱或用户名"
            />
            <small v-if="loginErrors.account">{{ loginErrors.account }}</small>
          </label>

          <label class="field">
            <span>密码</span>
            <input
              v-model="loginForm.password"
              :type="showLoginPassword ? 'text' : 'password'"
              placeholder="至少 6 位"
            />
            <small v-if="loginErrors.password">{{ loginErrors.password }}</small>
          </label>

          <div class="form-row">
            <label class="checkbox">
              <input v-model="loginForm.remember" type="checkbox" />
              <span>记住账号</span>
            </label>
            <button type="button" class="link-btn" @click="showLoginPassword = !showLoginPassword">
              {{ showLoginPassword ? '隐藏密码' : '显示密码' }}
            </button>
          </div>

          <button class="submit-btn" type="submit" :disabled="submitting">
            {{ submitting ? '登录中...' : '登录并进入 PeakStars_blog' }}
          </button>
        </form>

        <form v-else class="form" @submit.prevent="handleRegister">
          <label class="field">
            <span>用户名</span>
            <input
              v-model.trim="registerForm.username"
              type="text"
              placeholder="2-16 位，建议用你的昵称"
            />
            <small v-if="registerErrors.username">{{ registerErrors.username }}</small>
          </label>

          <label class="field">
            <span>邮箱</span>
            <input
              v-model.trim="registerForm.email"
              type="email"
              placeholder="name@example.com"
            />
            <small v-if="registerErrors.email">{{ registerErrors.email }}</small>
          </label>

          <label class="field">
            <span>密码</span>
            <input
              v-model="registerForm.password"
              :type="showRegisterPassword ? 'text' : 'password'"
              placeholder="至少 8 位，建议包含大写字母和数字"
            />
            <small v-if="registerErrors.password">{{ registerErrors.password }}</small>
          </label>

          <div class="strength-block">
            <div class="strength-bar">
              <span
                v-for="segment in 4"
                :key="segment"
                :class="{ active: segment <= passwordStrength }"
                :style="{ backgroundColor: segment <= passwordStrength ? strengthColor : '#e5e7eb' }"
              />
            </div>
            <p class="strength-text" :style="{ color: strengthColor }">{{ strengthText }}</p>
          </div>

          <label class="field">
            <span>确认密码</span>
            <input
              v-model="registerForm.confirmPassword"
              :type="showRegisterPassword ? 'text' : 'password'"
              placeholder="再输入一次密码"
            />
            <small v-if="registerErrors.confirmPassword">{{ registerErrors.confirmPassword }}</small>
          </label>

          <div class="form-row">
            <label class="checkbox">
              <input v-model="registerForm.agree" type="checkbox" />
              <span>我已阅读并同意使用说明</span>
            </label>
            <button type="button" class="link-btn" @click="showRegisterPassword = !showRegisterPassword">
              {{ showRegisterPassword ? '隐藏密码' : '显示密码' }}
            </button>
          </div>
          <small v-if="registerErrors.agree" class="inline-error">{{ registerErrors.agree }}</small>

          <button class="submit-btn" type="submit" :disabled="submitting">
            {{ submitting ? '注册中...' : '完成注册' }}
          </button>
        </form>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const activeTab = ref('login')
const submitting = ref(false)
const showLoginPassword = ref(false)
const showRegisterPassword = ref(false)

const notice = reactive({
  type: 'success',
  message: ''
})

const loginForm = reactive({
  account: authStore.rememberedAccount.value,
  password: '',
  remember: Boolean(authStore.rememberedAccount.value)
})

const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  agree: false
})

const loginErrors = reactive({
  account: '',
  password: ''
})

const registerErrors = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  agree: ''
})

const redirectPath = computed(() => {
  return typeof route.query.redirect === 'string' ? route.query.redirect : '/interview'
})

const passwordStrength = computed(() => {
  let score = 0
  if (registerForm.password.length >= 8) score += 1
  if (/[A-Z]/.test(registerForm.password)) score += 1
  if (/[0-9]/.test(registerForm.password)) score += 1
  if (/[^A-Za-z0-9]/.test(registerForm.password)) score += 1
  return score
})

const strengthText = computed(() => {
  const map = {
    0: '密码强度还没建立起来',
    1: '偏弱，建议继续增强',
    2: '一般，可以再加一点复杂度',
    3: '不错，已经比较稳了',
    4: '很强，可以放心用了'
  }
  return map[passwordStrength.value]
})

const strengthColor = computed(() => {
  const map = {
    0: '#d6d3d1',
    1: '#ef4444',
    2: '#f59e0b',
    3: '#10b981',
    4: '#2563eb'
  }
  return map[passwordStrength.value]
})

function isEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)
}

function setNotice(type, message) {
  notice.type = type
  notice.message = message
}

function switchTab(tab) {
  activeTab.value = tab
  clearErrors()
}

function clearErrors() {
  loginErrors.account = ''
  loginErrors.password = ''
  registerErrors.username = ''
  registerErrors.email = ''
  registerErrors.password = ''
  registerErrors.confirmPassword = ''
  registerErrors.agree = ''
}

function validateLogin() {
  clearErrors()

  if (!loginForm.account) {
    loginErrors.account = '请输入邮箱或用户名'
  }

  if (!loginForm.password) {
    loginErrors.password = '请输入密码'
  } else if (loginForm.password.length < 6) {
    loginErrors.password = '密码至少需要 6 位'
  }

  return !loginErrors.account && !loginErrors.password
}

function validateRegister() {
  clearErrors()

  if (registerForm.username.length < 2 || registerForm.username.length > 16) {
    registerErrors.username = '用户名长度需要在 2 到 16 位之间'
  }

  if (!isEmail(registerForm.email)) {
    registerErrors.email = '请输入有效的邮箱地址'
  }

  if (registerForm.password.length < 8) {
    registerErrors.password = '密码至少需要 8 位'
  } else if (passwordStrength.value < 2) {
    registerErrors.password = '建议至少包含大写字母或数字，强度会更稳'
  }

  if (registerForm.password !== registerForm.confirmPassword) {
    registerErrors.confirmPassword = '两次输入的密码不一致'
  }

  if (!registerForm.agree) {
    registerErrors.agree = '请先勾选使用说明'
  }

  return !Object.values(registerErrors).some(Boolean)
}

async function handleLogin() {
  if (!validateLogin()) return

  submitting.value = true
  const result = authStore.login(loginForm)
  submitting.value = false

  if (!result.success) {
    loginErrors.password = result.message
    setNotice('error', result.message)
    return
  }

  setNotice('success', `${result.message}，正在进入 PeakStars_blog...`)
  await router.replace(redirectPath.value)
}

function resetRegisterForm() {
  registerForm.username = ''
  registerForm.email = ''
  registerForm.password = ''
  registerForm.confirmPassword = ''
  registerForm.agree = false
}

async function handleRegister() {
  if (!validateRegister()) return

  submitting.value = true
  const result = authStore.register(registerForm)
  submitting.value = false

  if (!result.success) {
    setNotice('error', result.message)
    return
  }

  loginForm.account = registerForm.email
  loginForm.password = ''
  loginForm.remember = true
  resetRegisterForm()
  activeTab.value = 'login'
  setNotice('success', '注册成功，现在登录后就能进入 PeakStars_blog 了')
}
</script>

<style scoped src="../styles/views/AuthView.css"></style>
