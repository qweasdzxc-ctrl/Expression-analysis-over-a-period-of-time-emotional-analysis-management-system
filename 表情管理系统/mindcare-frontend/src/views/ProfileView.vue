<template>
  <div class="profile-container">
    <el-card class="profile-card">
      <template #header>
        <div class="profile-header">
          <h2>个人资料</h2>
          <el-button 
            v-if="showUploadButton" 
            type="primary" 
            @click="triggerUpload"
          >更换头像</el-button>
        </div>
      </template>
      
      <!-- 用户基本信息 -->
      <div class="user-info">
        <div class="avatar-container">
          <el-avatar 
            :size="100" 
            :src="avatarUrl" 
            @error="handleAvatarError"
            @click="handleAvatarClick"
          />
        </div>
        <div class="info-details">
          <h3>{{ userInfo.username }}</h3>
          <p>{{ userInfo.email }}</p>
          <p>注册时间：{{ formatDate(userInfo.createTime) }}</p>
        </div>
      </div>
      
      <!-- 用户统计信息 -->
      <div class="user-stats">
        <el-row :gutter="20">
          <el-col :span="6">
            <div class="stat-item" @click="goToUserPosts">
              <div class="stat-icon">
                <el-icon><Document /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.postCount }}</div>
                <div class="stat-label">发帖</div>
              </div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item" @click="goToUserComments">
              <div class="stat-icon">
                <el-icon><ChatDotRound /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.commentCount }}</div>
                <div class="stat-label">评论</div>
              </div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item" @click="goToReceivedLikes">
              <div class="stat-icon">
                <el-icon><Star /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.likeCount }}</div>
                <div class="stat-label">获赞</div>
              </div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item" @click="goToUserCollections">
              <div class="stat-icon">
                <el-icon><Collection /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.collectCount }}</div>
                <div class="stat-label">收藏</div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
      
      <!-- 最近的帖子 -->
      <div class="recent-posts">
        <h3>最近发布的帖子</h3>
        <div v-for="post in recentPosts" :key="post.id" class="post-item">
          <router-link :to="`/post/${post.id}`">{{ post.title }}</router-link>
          <span class="post-time">{{ formatDate(post.createTime) }}</span>
        </div>
      </div>
    </el-card>

    <!-- 头像预览对话框 -->
    <el-dialog
      v-model="showPreview"
      width="400px"
      :show-close="true"
      :modal="true"
      custom-class="avatar-preview-dialog"
    >
      <el-image
        :src="userInfo.avatar || defaultAvatar"
        fit="contain"
        style="width: 100%; max-height: 400px;"
      />
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showPreview = false">关闭</el-button>
          <el-button v-if="showUploadButton" type="primary" @click="triggerUpload">更换头像</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 头像裁剪对话框 -->
    <el-dialog
      v-model="showCropper"
      title="裁剪头像"
      width="520px"
      :close-on-click-modal="false"
      custom-class="avatar-crop-dialog"
      :before-close="handleCropperClose"
    >
      <div class="cropper-box">
        <VueCropper
          ref="cropperRef"
          :img="cropperImage"
          :autoCrop="true"
          :fixedBox="true"
          :fixed="true"
          :centerBox="true"
          :infoTrue="true"
          :full="true"
          :autoCropWidth="200"
          :autoCropHeight="200"
          :outputType="'png'"
          mode="contain"
        />
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleCropperClose">取消</el-button>
          <el-button type="primary" @click="handleCropConfirm">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 隐藏的文件上传输入框 -->
    <input 
      type="file" 
      ref="fileInput" 
      style="display: none" 
      accept="image/*"
      @change="handleAvatarUpload"
    >
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import 'vue-cropper/dist/index.css'
import { VueCropper } from 'vue-cropper'
import type { UploadRequestOptions } from 'element-plus'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const fileInput = ref<HTMLInputElement | null>(null)
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const userInfo = ref({
  username: '',
  email: '',
  avatar: '',
  createTime: null
})

const stats = ref({
  postCount: 0,
  commentCount: 0,
  likeCount: 0,
  collectCount: 0
})

const recentPosts = ref([])

const userId = computed(() => {
  const routeUserId = route.params.userId
  if (routeUserId) return routeUserId
  return authStore.currentUser?.id
})

const isCurrentUser = computed(() => {
  return userId.value === authStore.currentUser?.id
})

const currentUsername = computed(() => authStore.currentUser?.username)

const formatDate = (date: string) => {
  if (!date) return ''
  return new Date(date).toLocaleString()
}

const avatarUrl = computed(() => {
  if (!userInfo.value.avatar) return defaultAvatar
  // 添加时间戳防止缓存
  return `${userInfo.value.avatar}?t=${new Date().getTime()}`
})

const handleAvatarError = () => {
  userInfo.value.avatar = defaultAvatar
  return true
}

const fetchUserInfo = async () => {
  if (!userId.value) return
  try {
    const response = await axios.get(`/api/users/${userId.value}`)
    userInfo.value = {
      ...response.data,
      avatar: response.data.avatar ? `${response.data.avatar}?t=${new Date().getTime()}` : defaultAvatar
    }
  } catch (error) {
    ElMessage.error('获取用户信息失败')
  }
}

const fetchUserStats = async () => {
  try {
    const response = await axios.get(`/api/users/stats/${userId.value}`)
    stats.value = response.data
  } catch (error) {
    console.error('获取用户统计信息失败:', error)
  }
}

const fetchRecentPosts = async () => {
  try {
    const response = await axios.get(`/api/users/posts/${userId.value}`)
    recentPosts.value = response.data
  } catch (error) {
    console.error('获取用户帖子失败:', error)
  }
}

const triggerUpload = () => {
  fileInput.value?.click()
}

const handleAvatarClick = () => {
  if (userInfo.value.avatar) {
    showPreview.value = true
  } else if (showUploadButton.value) {
    triggerUpload()
  }
}

const showCropper = ref(false)
const cropperImage = ref('')
const cropperRef = ref()

const handleCropperClose = () => {
  showCropper.value = false
  cropperImage.value = ''
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

const handleAvatarUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement
  if (!target.files?.length) return

  const file = target.files[0]
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
    return
  }

  // 读取文件并显示裁剪对话框
  const reader = new FileReader()
  reader.onload = (e) => {
    cropperImage.value = e.target?.result as string
    showCropper.value = true
  }
  reader.readAsDataURL(file)
}

const handleCropConfirm = async () => {
  try {
    // 获取裁剪后的图片数据
    const base64Data = await new Promise((resolve) => {
      cropperRef.value.getCropBlob((blob: Blob) => {
        resolve(blob)
      })
    })

    if (!base64Data) {
      ElMessage.error('获取裁剪图片失败')
      return
    }

    const formData = new FormData()
    formData.append('avatar', base64Data as Blob, 'avatar.png')

    const response = await axios.post('/api/users/avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        'X-User-Id': userId.value?.toString()
      }
    })

    if (response.data.success) {
      // 添加时间戳防止缓存
      const newAvatarUrl = `${response.data.url}?t=${new Date().getTime()}`
      userInfo.value.avatar = newAvatarUrl
      // 更新 store 中的头像
      authStore.updateUserAvatar(newAvatarUrl)
      ElMessage.success('头像上传成功')
      handleCropperClose()
      // 刷新用户信息
      await fetchUserInfo()
    } else {
      ElMessage.error(response.data.message || '头像上传失败')
    }
  } catch (error: any) {
    console.error('头像上传失败:', error)
    ElMessage.error(error.response?.data?.message || '头像上传失败')
  }
}

const showUploadButton = computed(() => isCurrentUser.value)

const goToUserPosts = () => {
  const basePath = isAdmin.value ? '/admin' : '/user'
  router.push(`${basePath}/${currentUsername.value}/community?tab=posts`)
}

const goToUserComments = () => {
  const basePath = isAdmin.value ? '/admin' : '/user'
  router.push(`${basePath}/${currentUsername.value}/community?tab=comments`)
}

const goToReceivedLikes = () => {
  const basePath = isAdmin.value ? '/admin' : '/user'
  router.push(`${basePath}/${currentUsername.value}/community?tab=received-likes`)
}

const goToUserCollections = () => {
  const basePath = isAdmin.value ? '/admin' : '/user'
  router.push(`${basePath}/${currentUsername.value}/community?tab=collections`)
}

const showPreview = ref(false)

onMounted(() => {
  fetchUserInfo()
  fetchUserStats()
  fetchRecentPosts()
})
</script>

<style scoped>
.profile-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.profile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info .el-avatar {
  cursor: pointer;
  transition: transform 0.2s;
}

.user-info .el-avatar:hover {
  transform: scale(1.05);
}

.info-details h3 {
  margin: 0 0 10px 0;
  color: #303133;
}

.info-details p {
  margin: 5px 0;
  color: #606266;
}

.user-stats {
  margin: 30px 0;
}

.stat-item {
  display: flex;
  align-items: center;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.stat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
  background: #ecf5ff;
}

.stat-icon {
  font-size: 24px;
  color: #409EFF;
  margin-right: 15px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.recent-posts {
  margin-top: 30px;
}

.recent-posts h3 {
  margin-bottom: 15px;
  color: #303133;
}

.post-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #EBEEF5;
}

.post-item:last-child {
  border-bottom: none;
}

.post-item a {
  color: #409EFF;
  text-decoration: none;
}

.post-time {
  color: #909399;
  font-size: 14px;
}

.avatar-container {
  position: relative;
  display: inline-block;
  cursor: pointer;
}

.avatar-container .el-avatar {
  transition: transform 0.2s;
}

.avatar-container:hover .el-avatar {
  transform: scale(1.05);
}

.el-avatar {
  cursor: pointer;
  transition: opacity 0.3s;
}

.el-avatar:hover {
  opacity: 0.9;
}

:deep(.avatar-preview-dialog .el-dialog__body) {
  padding: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.cropper-box {
  width: 100%;
  height: 360px;
  background-color: #f5f7fa;
}

:deep(.avatar-crop-dialog) {
  .el-dialog__body {
    padding: 0 !important;
  }
}

:deep(.avatar-crop-dialog .el-dialog__header) {
  padding: 15px 20px;
  margin-right: 0;
  border-bottom: 1px solid #e4e7ed;
}

:deep(.avatar-crop-dialog .el-dialog__footer) {
  padding: 15px 20px;
  border-top: 1px solid #e4e7ed;
}

:deep(.vue-cropper) {
  background-color: #f5f7fa !important;
}

:deep(.cropper-view-box) {
  border-radius: 50%;
  outline: 2px solid #fff;
  outline-offset: -1px;
}

:deep(.cropper-dashed) {
  border: none;
}

:deep(.cropper-modal) {
  background: rgba(0, 0, 0, 0.5);
}

:deep(.cropper-face) {
  background-color: transparent !important;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style> 