<template>
  <div class="login-container">
    <el-tabs v-model="activeTab" class="login-tabs">
      <el-tab-pane label="用户登录" name="user">
        <el-form :model="userForm" :rules="rules" ref="userFormRef">
          <el-form-item prop="username" label="用户名">
            <el-input v-model="userForm.username" placeholder="请输入用户名"></el-input>
          </el-form-item>
          <el-form-item prop="password" label="密码">
            <el-input v-model="userForm.password" type="password" placeholder="请输入密码"></el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleUserLogin" :loading="loading">登录</el-button>
            <el-button @click="$router.push('/register')">注册</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      
      <el-tab-pane label="管理员登录" name="admin">
        <el-form :model="adminForm" :rules="rules" ref="adminFormRef">
          <el-form-item prop="username" label="用户名">
            <el-input v-model="adminForm.username" placeholder="请输入管理员用户名"></el-input>
          </el-form-item>
          <el-form-item prop="password" label="密码">
            <el-input v-model="adminForm.password" type="password" placeholder="请输入密码"></el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleAdminLogin" :loading="loading">登录</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const activeTab = ref('user')
const loading = ref(false)

const userForm = reactive({
  username: '',
  password: ''
})

const adminForm = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}

const userFormRef = ref()
const adminFormRef = ref()

const authStore = useAuthStore()

const handleLoginSuccess = (response: any) => {
  // ... 其他登录成功处理代码

  // 显示最新公告
  if (response.announcement) {
    ElMessageBox.alert(
      response.announcement.content,
      '系统公告',
      {
        confirmButtonText: '我知道了',
        type: 'info'
      }
    )
  }
}

const handleUserLogin = async () => {
  if (!userFormRef.value) return
  
  await userFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        loading.value = true
        const response = await axios.post('/api/auth/user/login', userForm)
        
        if (!response.data || !response.data.user || !response.data.sessionId) {
          throw new Error('Invalid response data')
        }
        
        authStore.setUser(response.data.user, response.data.sessionId)
        ElMessage.success('登录成功')
        
        // 使用用户名构建路径
        await router.push(`/user/${response.data.user.username}`)

        handleLoginSuccess(response.data)
      } catch (error: any) {
        console.error('Login error:', error)
        ElMessage.error(error.response?.data?.error || '登录失败')
      } finally {
        loading.value = false
      }
    }
  })
}

const handleAdminLogin = () => {
  adminFormRef.value?.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const response = await axios.post('/api/auth/admin/login', {
          username: adminForm.username,
          password: adminForm.password
        })
        
        // 存储认证信息
        authStore.setAdmin(response.data.admin, response.data.sessionId)
        ElMessage.success('登录成功')
        
        // 修改这里，使用管理员路径
        await router.push(`/admin/${response.data.admin.username}`)
      } catch (error: any) {
        console.error('Login error:', error)
        ElMessage.error(error.response?.data?.error || '登录失败')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 80vh;
}

.login-tabs {
  width: 400px;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
}

.el-form-item {
  margin-bottom: 20px;
}

.el-button {
  width: 100%;
  margin-top: 10px;
}
</style> 