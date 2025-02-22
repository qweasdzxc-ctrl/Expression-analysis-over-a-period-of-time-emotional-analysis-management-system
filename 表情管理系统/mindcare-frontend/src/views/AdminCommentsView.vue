<template>
  <div class="comments-container">
    <el-card class="comments-card">
      <template #header>
        <div class="card-header">
          <span>评论管理</span>
          <el-input
            v-model="searchQuery"
            placeholder="搜索评论内容..."
            class="search-input"
            clearable
            @clear="handleSearch"
            @input="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
      </template>

      <el-table
        :data="comments"
        style="width: 100%"
        v-loading="loading"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="content" label="评论内容" show-overflow-tooltip />
        <el-table-column label="评论者" width="120">
          <template #default="{ row }">
            <div class="user-info">
              <el-avatar :size="24" :src="row.user.avatar" />
              <span>{{ row.user.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="所属帖子" width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" @click="viewPost(row.post)">
              {{ row.post.title }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="评论时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="danger"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
            <el-button
              size="small"
              type="primary"
              @click="viewPost(row.post)"
            >
              查看帖子
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import axios from 'axios'
import type { Comment, Post } from '@/types'

const router = useRouter()
const loading = ref(false)
const comments = ref<Comment[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchQuery = ref('')

const fetchComments = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/admin/comments', {
      params: {
        page: currentPage.value - 1,
        size: pageSize.value,
        search: searchQuery.value
      }
    })
    comments.value = response.data.content
    total.value = response.data.totalElements
  } catch (error) {
    ElMessage.error('获取评论列表失败')
  } finally {
    loading.value = false
  }
}

const handleDelete = async (comment: Comment) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这条评论吗？此操作不可恢复。',
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await axios.delete(`/api/admin/comments/${comment.id}`)
    ElMessage.success('删除成功')
    await fetchComments()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const viewPost = (post: Post) => {
  router.push(`/post/${post.id}`)
}

const handleSearch = () => {
  currentPage.value = 1
  fetchComments()
}

const handleSizeChange = (val: number) => {
  pageSize.value = val
  fetchComments()
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
  fetchComments()
}

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  fetchComments()
})
</script>

<style scoped>
.comments-container {
  min-height: 100%;
}

.comments-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-input {
  width: 300px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

:deep(.el-table) {
  margin-top: 20px;
}

:deep(.el-button) {
  margin-left: 8px;
}

:deep(.el-table .cell) {
  white-space: nowrap;
}
</style> 