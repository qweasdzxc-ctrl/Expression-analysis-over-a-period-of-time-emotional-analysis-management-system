<template>
  <el-container class="layout">
    <el-header v-if="!isAdminRoute">
      <el-menu mode="horizontal" router :default-active="activeIndex" class="main-menu">
        <template v-if="isAuthenticated">
          <!-- 左侧菜单 -->
          <div class="left-menu">
            <el-menu-item :index="homeRoute">
              <el-icon><House /></el-icon>
              首页
            </el-menu-item>
            <el-menu-item :index="`${baseRoute}/face-capture`">
              <el-icon><Camera /></el-icon>
              表情捕捉
            </el-menu-item>
            <el-menu-item :index="`${baseRoute}/community`">
              <el-icon><ChatDotRound /></el-icon>
              心灵社区
            </el-menu-item>
            <el-menu-item :index="`${baseRoute}/chat`">
              <el-icon><ChatLineRound /></el-icon>
              聊天室
            </el-menu-item>
          </div>

          <!-- 右侧用户菜单 -->
          <div class="user-menu">
            <el-dropdown trigger="click">
              <div class="user-dropdown-link">
                <el-avatar :size="32" :src="currentUserAvatar" />
                <span class="username">{{ currentUser?.username }}</span>
                <el-icon class="el-icon--right"><arrow-down /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="goToProfile">
                    <el-icon><User /></el-icon>
                    个人主页
                  </el-dropdown-item>
                  <el-dropdown-item @click="handleLogout">
                    <el-icon><SwitchButton /></el-icon>
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>
        <template v-else>
          <div class="auth-menu">
            <el-menu-item index="/login">
              <el-icon><Key /></el-icon>
              登录
            </el-menu-item>
            <el-menu-item index="/register">
              <el-icon><Edit /></el-icon>
              注册
            </el-menu-item>
          </div>
        </template>
      </el-menu>
    </el-header>
    <el-main :class="{ 'admin-main': isAdminRoute }">
      <router-view></router-view>
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  House,
  Camera,
  Service,
  ChatDotRound,
  ChatLineRound,
  Setting,
  Key,
  Edit,
  UserFilled,
  User,
  SwitchButton,
  ArrowDown
} from '@element-plus/icons-vue'
import {
  ElContainer,
  ElHeader,
  ElMain,
  ElMenu,
  ElMenuItem,
  ElDropdown,
  ElDropdownMenu,
  ElDropdownItem,
  ElIcon,
  ElAvatar
} from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// 添加一个强制更新的标志
const updateKey = ref(0)

const activeIndex = computed(() => {
  if (!authStore.isAuthenticated) return route.path
  
  const username = authStore.currentUser?.username
  const basePath = authStore.isAdmin ? '/admin' : '/user'
  
  if (route.path.includes(basePath)) {
    return route.path
  }
  return `${basePath}/${username}`
})

// 监听路由变化，强制更新组件
watch(() => route.path, () => {
  updateKey.value++
})

const isAuthenticated = computed(() => authStore.isAuthenticated)
const isAdmin = computed(() => authStore.isAdmin)
const currentUser = computed(() => authStore.currentUser)

const currentUsername = computed(() => authStore.currentUser?.username || '')
const currentUserAvatar = computed(() => {
  const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
  if (!authStore.currentUser?.avatar) return defaultAvatar
  // 添加时间戳防止缓存
  return `${authStore.currentUser.avatar}?t=${new Date().getTime()}`
})

// 计算基础路由
const baseRoute = computed(() => {
  const username = authStore.currentUser?.username
  return authStore.isAdmin ? `/admin/${username}` : `/user/${username}`
})

// 计算首页路由
const homeRoute = computed(() => baseRoute.value)

// 添加判断是否为管理员路由的计算属性
const isAdminRoute = computed(() => {
  return route.path.startsWith('/admin/')
})

const goToProfile = () => {
  if (authStore.currentUser?.username) {
    // 根据用户类型决定路由
    const profilePath = isAdmin.value 
      ? `/admin/${authStore.currentUser.username}/profile`
      : `/user/${authStore.currentUser.username}/profile`
    router.push(profilePath)
  }
}

const handleLogout = async () => {
  authStore.logout()
  ElMessage.success('已退出登录')
  await router.push('/login')
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
}

.el-header {
  padding: 0;
  height: 60px;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);
}

.el-main {
  padding-top: 80px;
  background-color: #f5f7fa;
}

/* 管理员界面的主内容区不需要顶部padding */
.admin-main {
  padding-top: 0;
}

.main-menu {
  display: flex;
  justify-content: space-between;
  height: 60px;
  line-height: 60px;
  border: none !important;
}

.left-menu {
  display: flex;
  align-items: center;
}

.user-menu {
  margin-right: 20px;
}

.user-dropdown-link {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 0 8px;
  height: 40px;
  border-radius: 20px;
  transition: all 0.3s;
}

.user-dropdown-link:hover {
  background: #f5f7fa;
}

.username {
  margin: 0 8px;
  color: #606266;
}

.el-dropdown-menu__item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.el-menu-item {
  font-size: 15px;
}

.el-menu-item .el-icon {
  margin-right: 4px;
  font-size: 18px;
}

.auth-menu {
  display: flex;
  margin-right: 20px;
}
</style> 