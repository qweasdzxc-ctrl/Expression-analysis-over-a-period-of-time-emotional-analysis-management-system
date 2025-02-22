<template>
  <div class="community-container">
    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <el-tab-pane label="全部帖子" name="all">
        <el-row :gutter="20">
          <!-- 左侧帖子列表 -->
          <el-col :span="18">
            <el-card class="post-list">
              <template #header>
                <div class="post-header">
                  <span>心灵社区</span>
                  <el-button type="primary" @click="openPostDialog">发布帖子</el-button>
                </div>
              </template>
              
              <!-- 帖子列表 -->
              <div class="post-items">
                <el-card v-for="post in posts" :key="post.id" class="post-item" @click="viewPost(post)">
                  <div class="post-title">
                    <h3>{{ post.title }}</h3>
                  </div>
                  
                  <!-- 添加帖子内容和图片预览 -->
                  <div class="post-content">{{ post.content }}</div>
                  
                  <!-- 图片预览区域 -->
                  <div v-if="post.imageUrls && post.imageUrls.length > 0" class="post-images-preview">
                    <el-image
                      v-for="(url, index) in post.imageUrls.slice(0, 3)"
                      :key="index"
                      :src="url"
                      fit="cover"
                      class="preview-image"
                    >
                      <template #error>
                        <div class="image-slot">
                          <el-icon><Picture /></el-icon>
                        </div>
                      </template>
                    </el-image>
                    <div v-if="post.imageUrls.length > 3" class="more-images">
                      +{{ post.imageUrls.length - 3 }}
                    </div>
                  </div>

                  <div class="post-info">
                    <span>
                      <el-avatar :size="24" :src="post.user.avatar"></el-avatar>
                      {{ post.user.username }}
                    </span>
                    <span>{{ formatDate(post.createTime) }}</span>
                  </div>
                  <div class="post-stats">
                    <span>
                      <el-icon><View /></el-icon>
                      {{ post.viewCount }}
                    </span>
                    <span>
                      <el-icon><ChatDotSquare /></el-icon>
                      {{ post.commentCount }}
                    </span>
                    <span>
                      <el-icon><Star /></el-icon>
                      {{ post.collectCount }}
                    </span>
                  </div>
                </el-card>
              </div>
              
              <!-- 分页 -->
              <div class="pagination">
                <el-pagination
                  v-model="currentPage"
                  :page-size="pageSize"
                  :total="total"
                  @current-change="handlePageChange"
                />
              </div>
            </el-card>
          </el-col>
          
          <!-- 右侧信息栏 -->
        </el-row>
      </el-tab-pane>
      <el-tab-pane label="我的帖子" name="posts" v-if="isCurrentUserProfile">
        <el-card class="post-list">
          <div class="post-items">
            <el-card v-for="post in userPosts" :key="post.id" class="post-item">
              <div class="post-title">
                <h3>{{ post.title }}</h3>
                <el-button 
                  type="danger" 
                  size="small" 
                  @click.stop="handleDeletePost(post)"
                  class="delete-btn"
                >
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </div>
              <div class="post-content">{{ post.content }}</div>
              <div v-if="post.imageUrls && post.imageUrls.length > 0" class="post-images-preview">
                <el-image
                  v-for="(url, index) in post.imageUrls.slice(0, 3)"
                  :key="index"
                  :src="url"
                  fit="cover"
                  class="preview-image"
                  @click.stop
                >
                  <template #error>
                    <div class="image-slot">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
                <div v-if="post.imageUrls.length > 3" class="more-images">
                  +{{ post.imageUrls.length - 3 }}
                </div>
              </div>
              <div class="post-info">
                <span>{{ formatDate(post.createTime) }}</span>
              </div>
              <div class="post-stats">
                <span><el-icon><View /></el-icon>{{ post.viewCount }}</span>
                <span><el-icon><ChatDotSquare /></el-icon>{{ post.commentCount }}</span>
                <span><el-icon><Star /></el-icon>{{ post.collectCount }}</span>
              </div>
            </el-card>
          </div>
        </el-card>
      </el-tab-pane>
      <el-tab-pane label="我的获赞" name="received-likes" v-if="isCurrentUserProfile">
        <el-card class="post-list">
          <div class="post-items">
            <el-card v-for="post in receivedLikesPosts" :key="post.id" class="post-item" @click="viewPost(post)">
              <div class="post-title">
                <h3>{{ post.title }}</h3>
              </div>
              <div class="post-info">
                <span>{{ formatDate(post.createTime) }}</span>
              </div>
              <div class="post-content">{{ post.content }}</div>
              <div class="post-stats">
                <span><el-icon><View /></el-icon>{{ post.viewCount }}</span>
                <span><el-icon><ChatDotSquare /></el-icon>{{ post.commentCount }}</span>
                <span><el-icon><Star /></el-icon>{{ post.likeCount }}</span>
              </div>
            </el-card>
          </div>
        </el-card>
      </el-tab-pane>
      <el-tab-pane label="我的评论" name="comments" v-if="isCurrentUserProfile">
        <el-card class="comment-list">
          <div v-for="comment in userComments" :key="comment.id" class="comment-item">
            <div class="comment-content">{{ comment.content }}</div>
            <div class="comment-info">
              <span>发表于：{{ formatDate(comment.createTime) }}</span>
              <el-button link @click="viewPost(comment.post)">查看原帖</el-button>
            </div>
          </div>
        </el-card>
      </el-tab-pane>
      <el-tab-pane label="我的点赞" name="likes" v-if="isCurrentUserProfile">
        <el-card class="post-list">
          <div class="post-items">
            <el-card v-for="post in likedPosts" :key="post.id" class="post-item" @click="viewPost(post)">
              <div class="post-title">
                <h3>{{ post.title }}</h3>
              </div>
              <div class="post-info">
                <span>
                  <el-avatar :size="24" :src="post.user.avatar"></el-avatar>
                  {{ post.user.username }}
                </span>
                <span>{{ formatDate(post.createTime) }}</span>
              </div>
            </el-card>
          </div>
        </el-card>
      </el-tab-pane>
      <el-tab-pane label="我的收藏" name="collections" v-if="isCurrentUserProfile">
        <el-card class="post-list">
          <div class="post-items">
            <el-card v-for="post in collectedPosts" :key="post.id" class="post-item" @click="viewPost(post)">
              <div class="post-title">
                <h3>{{ post.title }}</h3>
              </div>
              <div class="post-info">
                <span>
                  <el-avatar :size="24" :src="post.user.avatar"></el-avatar>
                  {{ post.user.username }}
                </span>
                <span>{{ formatDate(post.createTime) }}</span>
              </div>
            </el-card>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 发帖对话框 -->
    <el-dialog
      v-model="showPostDialog"
      title="发布帖子"
      width="600px"
    >
      <el-form :model="postForm" ref="postFormRef" :rules="postRules">
        <el-form-item label="标题" prop="title">
          <el-input v-model="postForm.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="postForm.content"
            type="textarea"
            :rows="6"
            placeholder="请输入内容"
          />
        </el-form-item>
        <!-- 添加图片上传组件 -->
        <el-form-item label="图片">
          <el-upload
            class="post-image-upload"
            :action="null"
            :auto-upload="false"
            :show-file-list="true"
            :on-change="handleImageChange"
            :before-remove="handleImageRemove"
            accept="image/*"
            multiple
            list-type="picture-card"
          >
            <template #default>
              <el-icon><Plus /></el-icon>
            </template>
            <template #file="{ file }">
              <img class="upload-image" :src="file.url" alt=""/>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showPostDialog = false">取消</el-button>
          <el-button type="primary" @click="submitPost" :loading="submitting">
            发布
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  View,
  ChatDotSquare,
  Star,
  Plus,
  Picture,
  Delete
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// 分页相关
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 帖子列表
const posts = ref([])

// 用户统计
const userStats = reactive({
  postCount: 0,
  collectCount: 0,
  likeCount: 0
})

// 发帖相关
const showPostDialog = ref(false)
const submitting = ref(false)
const postFormRef = ref()
const postForm = ref({
  title: '',
  content: '',
  images: [] as File[]
})

const postRules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入内容', trigger: 'blur' }
  ]
}

const activeTab = ref(route.query.tab?.toString() || 'all')

const isCurrentUserProfile = computed(() => {
  return route.params.username === authStore.currentUser?.username
})

// 格式化日期
const formatDate = (date: string) => {
  if (!date) return ''
  return new Date(date).toLocaleString()
}

// 获取帖子列表
const fetchPosts = async () => {
  try {
    const response = await axios.get('/api/posts', {
      params: {
        page: currentPage.value - 1,  // 后端从0开始计数
        size: pageSize.value
      }
    })
    posts.value = response.data.content
    total.value = response.data.totalElements
  } catch (error) {
    console.error('获取帖子列表失败', error)
  }
}

// 获取用户统计
const fetchUserStats = async () => {
  try {
    const response = await axios.get(`http://localhost:8080/api/users/stats`)
    Object.assign(userStats, response.data)
  } catch (error) {
    ElMessage.error('获取用户统计失败')
  }
}

// 获取用户帖子
const userPosts = ref([])
const fetchUserPosts = async () => {
  try {
    const response = await axios.get(`/api/users/${authStore.currentUser?.id}/posts`)
    userPosts.value = response.data
  } catch (error) {
    console.error('获取用户帖子失败', error)
  }
}

// 获取用户评论
const userComments = ref([])
const fetchUserComments = async () => {
  try {
    const response = await axios.get(`/api/comments`, {
      params: {
        userId: authStore.currentUser?.id
      }
    })
    userComments.value = response.data
  } catch (error) {
    ElMessage.error('获取用户评论失败')
  }
}

// 获取用户点赞的帖子
const likedPosts = ref([])
const fetchLikedPosts = async () => {
  try {
    const response = await axios.get(`/api/users/stats/likes`, {
      params: {
        userId: authStore.currentUser?.id
      }
    })
    likedPosts.value = response.data
  } catch (error) {
    ElMessage.error('获取点赞帖子失败')
  }
}

// 获取用户收藏的帖子
const collectedPosts = ref([])
const fetchCollectedPosts = async () => {
  try {
    const userId = authStore.currentUser?.id
    if (!userId) {
      console.warn('用户未登录，无法获取收藏帖子')
      return
    }

    const response = await axios.get('/api/collections/user', {
      params: { userId }
    })
    collectedPosts.value = response.data
  } catch (error) {
    console.error('获取收藏帖子失败', error)
    ElMessage.error('获取收藏帖子失败')
    collectedPosts.value = [] // 设置为空数组
  }
}

// 获取用户获赞的帖子
const receivedLikesPosts = ref([])
const fetchReceivedLikesPosts = async () => {
  try {
    const userId = authStore.currentUser?.id
    if (!userId) {
      console.warn('用户未登录，无法获取获赞帖子')
      return
    }
    const response = await axios.get(`/api/likes/user/${userId}/received`)
    receivedLikesPosts.value = response.data
  } catch (error) {
    console.error('获取获赞帖子失败:', error)
    ElMessage.error('获取获赞帖子失败')
  }
}

// 修改为
const openPostDialog = () => {
  showPostDialog.value = true
}

// 提交帖子
const submitPost = async () => {
  if (!postFormRef.value) return
  
  try {
    await postFormRef.value.validate()
    submitting.value = true

    const formData = new FormData()
    formData.append('title', postForm.value.title)
    formData.append('content', postForm.value.content)
    
    // 添加图片文件到 FormData
    postForm.value.images.forEach((image, index) => {
      formData.append(`images`, image)
    })

    const response = await axios.post('/api/posts', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    if (response.data.success) {
      ElMessage.success('发布成功')
      showPostDialog.value = false
      resetForm()
      // 刷新帖子列表
      await fetchPosts()
    } else {
      throw new Error(response.data.message || '发布失败')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '发布失败')
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  if (postFormRef.value) {
    postFormRef.value.resetFields()
  }
  postForm.value.images = []
}

// 查看帖子详情
const viewPost = (post: any) => {
  router.push(`/post/${post.id}`)
}

// 处理分页变化
const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchPosts()
}

const handleTabClick = async (tab: any) => {
  const query = tab.props.name === 'all' ? {} : { tab: tab.props.name }
  router.push({ query })
  
  // 根据标签页加载对应数据
  switch (tab.props.name) {
    case 'posts':
      await fetchUserPosts()
      break
    case 'comments':
      await fetchUserComments()
      break
    case 'likes':
      await fetchLikedPosts()
      break
    case 'collections':
      await fetchCollectedPosts()
      break
    case 'received-likes':
      await fetchReceivedLikesPosts()
      break
  }
}

const handleImageChange = (file: any) => {
  if (!file.raw.type.startsWith('image/')) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB!')
    return false
  }
  postForm.value.images.push(file.raw)
  return true
}

const handleImageRemove = (file: any) => {
  const index = postForm.value.images.findIndex(f => f === file.raw)
  if (index !== -1) {
    postForm.value.images.splice(index, 1)
  }
}

// 添加删除帖子方法
const handleDeletePost = async (post: any) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这篇帖子吗？删除后无法恢复。',
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await axios.delete(`/api/posts/${post.id}`)
    ElMessage.success('删除成功')
    
    // 重新获取帖子列表
    if (activeTab.value === 'posts') {
      await fetchUserPosts()
    } else {
      await fetchPosts()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

onMounted(async () => {
  fetchPosts()
  fetchUserStats()
  
  // 根据 URL 参数设置初始 tab 并加载对应数据
  if (route.query.tab) {
    activeTab.value = route.query.tab.toString()
    switch (activeTab.value) {
      case 'posts':
        await fetchUserPosts()
        break
      case 'comments':
        await fetchUserComments()
        break
      case 'likes':
        await fetchLikedPosts()
        break
      case 'collections':
        await fetchCollectedPosts()
        break
      case 'received-likes':
        await fetchReceivedLikesPosts()
        break
    }
  }
})
</script>

<style scoped>
.community-container {
  padding: 20px;
}

.post-list {
  margin-bottom: 20px;
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.post-items {
  margin-bottom: 20px;
}

.post-item {
  margin-bottom: 16px;
  cursor: pointer;
  transition: transform 0.2s;
}

.post-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.post-title h3 {
  margin: 0;
  color: #303133;
}

.post-info {
  margin: 10px 0;
  color: #909399;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 20px;
}

.post-stats {
  display: flex;
  gap: 20px;
  color: #909399;
  font-size: 14px;
}

.post-stats span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.user-info {
  position: sticky;
  top: 20px;
}

.user-stats {
  padding: 10px 0;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #EBEEF5;
}

.stat-item:last-child {
  border-bottom: none;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.comment-list {
  margin-bottom: 20px;
}

.comment-item {
  padding: 15px;
  border-bottom: 1px solid #EBEEF5;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-content {
  margin-bottom: 10px;
  color: #303133;
}

.comment-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #909399;
  font-size: 14px;
}

.post-image-upload {
  :deep(.el-upload--picture-card) {
    width: 100px;
    height: 100px;
    line-height: 100px;
  }
  
  :deep(.el-upload-list--picture-card) {
    --el-upload-list-picture-card-size: 100px;
  }
}

.upload-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

:deep(.el-upload-list__item-thumbnail) {
  object-fit: cover;
}

.post-content {
  margin: 10px 0;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.post-images-preview {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin: 10px 0;
  position: relative;
}

.preview-image {
  width: 100%;
  height: 120px;
  border-radius: 4px;
  object-fit: cover;
}

.more-images {
  position: absolute;
  right: 8px;
  bottom: 8px;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
}

.post-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.delete-btn {
  opacity: 0;
  transition: opacity 0.2s;
}

.post-item:hover .delete-btn {
  opacity: 1;
}
</style> 