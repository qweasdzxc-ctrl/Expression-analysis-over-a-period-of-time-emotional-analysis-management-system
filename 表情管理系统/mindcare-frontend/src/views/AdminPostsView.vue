<template>
  <div class="posts-container">
    <el-card class="posts-card">
      <template #header>
        <div class="card-header">
          <span>帖子管理</span>
          <el-input
            v-model="searchQuery"
            placeholder="搜索帖子..."
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
        :data="posts"
        style="width: 100%"
        v-loading="loading"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="user.username" label="发布者" width="120" />
        <el-table-column prop="createTime" label="发布时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusTag(scope.row.status)">
              {{ getStatusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button
              size="small"
              @click="viewPost(scope.row)"
            >
              查看
            </el-button>
            <el-button
              size="small"
              :type="scope.row.status === 'normal' ? 'danger' : 'success'"
              @click="togglePostStatus(scope.row)"
            >
              {{ scope.row.status === 'normal' ? '禁用' : '启用' }}
            </el-button>
            <el-button
              size="small"
              type="danger"
              @click="deletePost(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
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
import type { Post } from '@/types'

const router = useRouter()
const loading = ref(false)
const posts = ref<Post[]>([])
const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const formatDate = (date: string) => {
  return new Date(date).toLocaleString()
}

const fetchPosts = async () => {
  try {
    loading.value = true
    const response = await axios.get('/api/admin/posts', {
      params: {
        page: currentPage.value - 1,
        size: pageSize.value,
        search: searchQuery.value
      }
    })
    posts.value = response.data.content
    total.value = response.data.totalElements
  } catch (error) {
    ElMessage.error('获取帖子列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchPosts()
}

const handleSizeChange = (val: number) => {
  pageSize.value = val
  fetchPosts()
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
  fetchPosts()
}

const viewPost = (post: Post) => {
  router.push(`/post/${post.id}`)
}

const togglePostStatus = async (post: Post) => {
  try {
    const action = post.status === 'normal' ? 'disable' : 'enable'
    await axios.post(`/api/admin/posts/${post.id}/${action}`)
    ElMessage.success(`${action === 'disable' ? '禁用' : '启用'}成功`)
    await fetchPosts()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const deletePost = async (post: Post) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除该帖子吗？此操作不可恢复',
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await axios.delete(`/api/admin/posts/${post.id}`)
    ElMessage.success('删除成功')
    await fetchPosts()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const getStatusTag = (status: string) => {
  switch (status) {
    case 'normal':
      return 'success'
    case 'disabled':
      return 'danger'
    default:
      return 'warning'
  }
}

const getStatusLabel = (status: string) => {
  switch (status) {
    case 'normal':
      return '正常'
    case 'disabled':
      return '已禁用'
    default:
      return '待审核'
  }
}

onMounted(() => {
  fetchPosts()
})
</script>

<style scoped>
.posts-container {
  min-height: 100%;
}

.posts-card {
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

:deep(.el-table) {
  margin-top: 20px;
}
</style> 