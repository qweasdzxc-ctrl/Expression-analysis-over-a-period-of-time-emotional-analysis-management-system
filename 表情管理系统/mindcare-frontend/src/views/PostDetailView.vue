<template>
  <div class="post-detail-container">
    <el-card class="post-card">
      <!-- 帖子标题 -->
      <h1 class="post-title">{{ post.title }}</h1>
      
      <!-- 作者信息 -->
      <div class="post-info">
        <div class="author-info">
          <el-avatar :size="32" :src="post.user?.avatar" />
          <span>{{ post.user?.username }}</span>
        </div>
        <span class="post-time">{{ formatDate(post.createTime) }}</span>
      </div>
      
      <!-- 帖子内容 -->
      <div class="post-content">
        {{ post.content }}
      </div>
      
      <!-- 添加图片显示区域 -->
      <div v-if="post.imageUrls && post.imageUrls.length > 0" class="post-images">
        <el-image
          v-for="(url, index) in post.imageUrls"
          :key="index"
          :src="url"
          :preview-src-list="post.imageUrls"
          fit="cover"
          class="post-image"
        />
      </div>

      <div class="post-actions">
        <el-button 
          :type="isLiked ? 'primary' : 'default'" 
          @click="handleLike"
          :loading="likeLoading"
        >
          <el-icon><Thumb /></el-icon>
          {{ post.likeCount }}
        </el-button>
        <el-button 
          :type="isCollected ? 'primary' : 'default'" 
          @click="handleCollect"
          :loading="collectLoading"
        >
          <el-icon><Star /></el-icon>
          {{ post.collectCount }}
        </el-button>
      </div>
    </el-card>

    <!-- 评论区域 -->
    <div class="comments-section">
      <h3>评论区</h3>
      <!-- 只有非管理员用户才显示评论输入框 -->
      <div v-if="authStore.isAuthenticated && !authStore.isAdmin" class="comment-input">
        <el-input
          v-model="newComment"
          type="textarea"
          :rows="3"
          placeholder="写下你的评论..."
        />
        <el-button 
          type="primary" 
          @click="submitComment"
          :loading="submitting"
        >
          发表评论
        </el-button>
      </div>

      <!-- 评论列表 -->
      <div class="comments-list">
        <div v-for="comment in comments" :key="comment.id" class="comment-item">
          <div class="comment-user">
            <el-avatar :size="32" :src="comment.user.avatar" />
            <span class="username">{{ comment.user.username }}</span>
            <span class="time">{{ formatDate(comment.createTime) }}</span>
          </div>
          <div class="comment-content">{{ comment.content }}</div>
        </div>
        <!-- 无评论时的提示 -->
        <el-empty 
          v-if="comments.length === 0" 
          description="暂无评论" 
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  View,
  Star,
  CaretTop as Thumb
} from '@element-plus/icons-vue'
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const authStore = useAuthStore()
const postId = computed(() => route.params.id)

// 帖子数据
const post = ref({
  id: '',
  title: '',
  content: '',
  user: null,
  createTime: '',
  likeCount: 0,
  commentCount: 0,
  collectCount: 0,
  imageUrls: []
})

// 评论相关
const comments = ref<Comment[]>([])
const newComment = ref('')
const submitting = ref(false)

// 交互状态
const isLiked = ref(false)
const isCollected = ref(false)
const likeLoading = ref(false)
const collectLoading = ref(false)

// 格式化日期
const formatDate = (date: string) => {
  if (!date) return ''
  return new Date(date).toLocaleString()
}

// 获取帖子详情和点赞状态
const fetchPost = async () => {
  try {
    const [postResponse, statusResponse] = await Promise.all([
      axios.get(`/api/posts/${route.params.id}`),
      axios.get(`/api/posts/${route.params.id}/status`)
    ])
    
    post.value = postResponse.data
    isLiked.value = statusResponse.data.isLiked
    isCollected.value = statusResponse.data.isCollected
    
    // 记录浏览
    await axios.post(`/api/posts/${route.params.id}/view`)
  } catch (error) {
    console.error('获取帖子详情失败', error)
    ElMessage.error('获取帖子详情失败')
  }
}

// 获取评论列表
const fetchComments = async () => {
  try {
    if (!postId.value) {
      ElMessage.error('帖子ID不存在')
      return
    }
    const response = await axios.get(`/api/posts/${postId.value}/comments`)
    comments.value = response.data
  } catch (error) {
    ElMessage.error('获取评论列表失败')
  }
}

// 提交评论
const submitComment = async () => {
  // 如果是管理员，不允许评论
  if (authStore.isAdmin) {
    ElMessage.warning('管理员不能发表评论')
    return
  }

  if (!newComment.value.trim()) {
    ElMessage.warning('评论内容不能为空')
    return
  }

  submitting.value = true
  try {
    await axios.post(`/api/posts/${route.params.id}/comments`, {
      content: newComment.value.trim()
    })
    ElMessage.success('评论发表成功')
    newComment.value = ''
    await fetchComments()
  } catch (error) {
    ElMessage.error('评论发表失败')
  } finally {
    submitting.value = false
  }
}

// 点赞/取消点赞
const handleLike = async () => {
  if (!authStore.isAuthenticated) {
    ElMessage.warning('请先登录')
    return
  }
  
  try {
    likeLoading.value = true
    const response = await axios.post(`/api/posts/${route.params.id}/like`)
    Object.assign(post.value, response.data.post)
    isLiked.value = response.data.isLiked
  } catch (error) {
    console.error('点赞操作失败:', error)
  } finally {
    likeLoading.value = false
  }
}

// 收藏/取消收藏
const handleCollect = async () => {
  if (!authStore.isAuthenticated) {
    ElMessage.warning('请先登录')
    return
  }
  
  try {
    collectLoading.value = true
    const response = await axios.post(`/api/posts/${route.params.id}/collect`)
    if (response.data.error) {
      console.error('收藏操作失败:', response.data.error)
      return
    }
    Object.assign(post.value, response.data.post)
    isCollected.value = response.data.isCollected
  } catch (error) {
    console.error('收藏操作失败:', error)
  } finally {
    collectLoading.value = false
  }
}

onMounted(() => {
  fetchPost()
  fetchComments()
})
</script>

<style scoped>
.post-detail-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.post-card {
  margin-bottom: 20px;
}

.post-header h2 {
  margin: 0;
  color: #303133;
}

.post-meta {
  margin-top: 10px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 20px;
}

.author {
  display: flex;
  align-items: center;
  gap: 8px;
}

.post-content {
  padding: 20px 0;
  line-height: 1.8;
  font-size: 16px;
  white-space: pre-wrap;
  word-break: break-word;
}

.post-actions {
  display: flex;
  gap: 15px;
  padding-top: 20px;
  border-top: 1px solid #EBEEF5;
}

.post-actions .el-button {
  min-width: 100px;
}

.post-actions .el-icon {
  margin-right: 5px;
}

.comments-section {
  margin-top: 20px;
}

.comment-input {
  margin-bottom: 20px;
}

.comment-input .el-button {
  margin-top: 10px;
  width: 100px;
}

.comments-list {
  margin-top: 20px;
}

.comment-item {
  padding: 15px 0;
  border-bottom: 1px solid #EBEEF5;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-user {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.username {
  font-weight: 500;
  color: #303133;
}

.time {
  color: #909399;
  font-size: 14px;
}

.comment-content {
  line-height: 1.6;
  margin: 10px 0;
  white-space: pre-wrap;
}

.post-images {
  margin-top: 20px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 10px;
}

.post-image {
  width: 100%;
  height: 200px;
  border-radius: 4px;
  cursor: pointer;
  transition: transform 0.3s;
}

.post-image:hover {
  transform: scale(1.02);
}

/* 当只有一张图片时的样式 */
.post-images:has(.post-image:only-child) {
  grid-template-columns: minmax(200px, 400px);
}

/* 当有两张图片时的样式 */
.post-images:has(.post-image:nth-child(2):last-child) {
  grid-template-columns: repeat(2, 1fr);
}

/* 当有三张及以上图片时的样式 */
.post-images:has(.post-image:nth-child(3)) {
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
}
</style> 