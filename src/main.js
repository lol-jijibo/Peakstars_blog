import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './assets/styles/global.css'
import { initTheme } from '@/stores/theme'

const app = createApp(App)
initTheme()
app.use(router)
app.mount('#app')
