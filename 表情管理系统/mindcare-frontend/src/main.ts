import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import axios from 'axios'
import { useAuthStore } from './stores/auth'
import { ElMessage } from 'element-plus'

import App from './App.vue'
import router from './router'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)

// 配置 axios
axios.defaults.baseURL = 'http://localhost:8080'
axios.defaults.withCredentials = true

// 初始化 auth store
const authStore = useAuthStore()

// 请求拦截器
axios.interceptors.request.use(config => {
  const headers = config.headers || {}
  
  // 添加认证信息
  if (authStore.sessionId) {
    headers['X-Session-ID'] = authStore.sessionId
  }
  if (authStore.currentUser?.id) {
    headers['X-User-Id'] = authStore.currentUser.id.toString()
  }
  
  config.headers = headers
  return config
}, error => {
  return Promise.reject(error)
})

// 响应拦截器
axios.interceptors.response.use(response => {
  return response
}, error => {
  if (error.response?.status === 401) {
    authStore.$reset()
    router.push('/login')
    ElMessage.error('请先登录')
  }
  return Promise.reject(error)
})

// 注册图标组件
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(router)
app.use(ElementPlus)

app.mount('#app') 