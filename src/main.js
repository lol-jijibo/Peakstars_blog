import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './assets/styles/global.css'
import { initTheme } from '@/stores/theme'

const app = createApp(App)
const pinia = createPinia()
initTheme()
app.use(pinia)
app.use(router)
app.mount('#app')
