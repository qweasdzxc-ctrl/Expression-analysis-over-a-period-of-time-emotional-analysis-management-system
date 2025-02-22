<template>
  <div class="chat-container">
    <el-card class="chat-card">
      <template #header>
        <div class="chat-header">
          <h2>聊天室</h2>
          <div class="header-actions">
            <span class="online-count">在线人数: {{ onlineCount }}</span>
            <!-- 添加管理员功能 -->
            <template v-if="isAdmin">
              <el-button type="danger" size="small" @click="handleClearChat">
                清空聊天记录
              </el-button>
              <el-button type="primary" size="small" @click="openAnnouncementDialog">
                发布公告
              </el-button>
            </template>
          </div>
        </div>
      </template>
      
      <!-- 消息列表 -->
      <div class="message-list" ref="messageList">
        <div v-for="message in messages" :key="message.id" 
          :class="['message-item', {'my-message': message.userId === currentUserId}]">
          <div class="message-user">
            <el-avatar :size="32" :src="message.user?.avatar"></el-avatar>
            <span class="username">{{ message.user?.username }}</span>
            <span class="time">{{ formatDate(message.createTime) }}</span>
          </div>
          <div class="message-content">{{ message.content }}</div>
        </div>
      </div>
      
      <!-- 发送消息区域 -->
      <div class="message-input-area">
        <el-input
          v-model="newMessage"
          type="textarea"
          :rows="3"
          placeholder="输入消息..."
          resize="none"
          @keyup.enter.native.exact="sendMessage"
          @keyup.enter.native.shift.exact="newMessage += '\n'"
        ></el-input>
        <div class="button-area">
          <span class="tip">Shift + Enter 换行，Enter 发送</span>
          <el-button type="primary" @click="sendMessage" :loading="loading">
            发送消息
          </el-button>
        </div>
      </div>
    </el-card>
    
    <!-- 添加发布公告对话框 -->
    <el-dialog
      v-model="announcementDialogVisible"
      title="发布公告"
      width="500px"
    >
      <el-form :model="announcementForm">
        <el-form-item label="公告内容">
          <el-input
            v-model="announcementForm.content"
            type="textarea"
            :rows="4"
            placeholder="请输入公告内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="announcementDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handlePublishAnnouncement">
            发布
          </el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 添加重连按钮 -->

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick, computed, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const messages = ref([])
const newMessage = ref('')
const loading = ref(false)
const messageList = ref(null)
const onlineCount = ref(0)
const currentUserId = computed(() => authStore.currentUser?.id)
const isAdmin = computed(() => authStore.isAdmin)
const announcementDialogVisible = ref(false)
const announcementForm = reactive({
  content: ''
})

// 格式化日期
const formatDate = (date: string) => {
  if (!date) return ''
  const d = new Date(date)
  return d.toLocaleTimeString('zh-CN', { hour12: false })
}

// 发送消息
const sendMessage = async () => {
  if (!newMessage.value.trim()) {
    return
  }
  
  try {
    loading.value = true
    const response = await axios.post('/api/chat/send', {
      content: newMessage.value
    })
    messages.value.push(response.data)
    newMessage.value = ''
  } catch (error) {
    ElMessage.error('发送失败')
  } finally {
    loading.value = false
  }
}

// 获取消息
const fetchMessages = async () => {
  try {
    const response = await axios.get('/api/chat/messages', {
      timeout: 10000, // 设置超时时间
      headers: {
        'Accept': 'application/json'
      }
    });
    
    if (response.data) {
      // 保持消息顺序，最早的消息在前
      messages.value = response.data.sort((a: any, b: any) => 
        new Date(a.createTime).getTime() - new Date(b.createTime).getTime()
      );
    }
  } catch (error: any) {
    if (error.code === 'ERR_INCOMPLETE_CHUNKED_ENCODING') {
      console.error('消息加载不完整，正在重试...');
      // 延迟1秒后重试
      setTimeout(fetchMessages, 1000);
    } else {
      console.error('获取消息失败:', error);
      ElMessage.error('获取消息失败，请刷新页面重试');
    }
  }
};

// 获取在线人数
const fetchOnlineCount = async () => {
  try {
    const response = await axios.get('/api/chat/online-count')
    onlineCount.value = response.data
  } catch (error) {
    console.error('获取在线人数失败:', error)
  }
}

// 用户上线
const userOnline = async () => {
  try {
    await axios.post('/api/chat/online')
    await fetchOnlineCount()
  } catch (error) {
    console.error('上线通知失败:', error)
  }
}

// 用户下线
const userOffline = async () => {
  try {
    await axios.post('/api/chat/offline')
    await fetchOnlineCount()
  } catch (error) {
    console.error('下线通知失败:', error)
  }
}

// 修改定时任务
let timer: number;
const startPolling = () => {
  timer = window.setInterval(async () => {
    try {
      await fetchMessages();
      await fetchOnlineCount();
    } catch (error) {
      console.error('轮询失败:', error);
      // 发生错误时停止轮询
      if (timer) {
        clearInterval(timer);
        timer = 0;
      }
    }
  }, 3000);
};

// 添加重新连接方法
const reconnect = () => {
  if (!timer) {
    startPolling();
    ElMessage.success('已重新连接');
  }
};

onMounted(async () => {
  await userOnline() // 用户进入聊天室时通知上线
  await fetchMessages()
  startPolling()
})

onBeforeUnmount(async () => {
  if (timer) {
    clearInterval(timer)
  }
  await userOffline() // 用户离开聊天室时通知下线
})

const handleClearChat = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要清空所有聊天记录吗？此操作不可恢复。',
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await axios.delete('/api/admin/chat/clear')
    ElMessage.success('聊天记录已清空')
    messages.value = []
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('清空聊天记录失败')
    }
  }
}

const openAnnouncementDialog = () => {
  announcementDialogVisible.value = true
  announcementForm.content = ''
}

const handlePublishAnnouncement = async () => {
  try {
    if (!announcementForm.content.trim()) {
      ElMessage.warning('请输入公告内容')
      return
    }
    
    await axios.post('/api/admin/announcements', {
      content: announcementForm.content
    })
    
    ElMessage.success('公告发布成功')
    announcementDialogVisible.value = false
  } catch (error) {
    ElMessage.error('公告发布失败')
  }
}
</script>

<style scoped>
.chat-container {
  padding: 20px;
  height: calc(100vh - 100px);
}

.chat-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chat-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.online-count {
  color: #67c23a;
  font-size: 14px;
}

/* 新增消息容器样式 */
.messages-container {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

/* 新增占位容器样式 */
.messages-spacer {
  flex: 1 0 auto;
}

/* 修改消息列表样式 */
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background-color: #f5f7fa;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  height: 0; /* 关键：设置高度为0，让flex: 1生效 */
}

.message-item {
  margin-bottom: 20px;
  max-width: 80%;
  width: fit-content;
}

.my-message {
  margin-left: auto;
}

.my-message .message-user {
  flex-direction: row-reverse;
}

.my-message .message-content {
  background-color: #ecf5ff;
}

.message-user {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 5px;
}

.username {
  font-weight: 500;
  color: #303133;
}

.time {
  color: #909399;
  font-size: 14px;
}

.message-content {
  padding: 12px 16px;
  background: #fff;
  border-radius: 4px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.message-input-area {
  padding: 20px;
  background: #fff;
  border-top: 1px solid #EBEEF5;
}

.button-area {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.tip {
  color: #909399;
  font-size: 12px;
}

:deep(.el-textarea__inner) {
  resize: none;
  border-radius: 4px;
}

.el-button {
  padding: 12px 20px;
  font-size: 14px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.el-button {
  margin-left: 8px;
}

.reconnect-btn {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 1000;
}
</style> 