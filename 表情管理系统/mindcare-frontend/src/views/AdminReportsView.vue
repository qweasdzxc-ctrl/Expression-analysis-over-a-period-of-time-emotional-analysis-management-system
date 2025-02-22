<template>
  <div class="reports-container">
    <el-card class="reports-card">
      <template #header>
        <div class="card-header">
          <span>举报处理</span>
          <el-radio-group v-model="status" @change="handleStatusChange">
            <el-radio-button label="pending">待处理</el-radio-button>
            <el-radio-button label="processed">已处理</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table
        :data="reports"
        style="width: 100%"
        v-loading="loading"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="scope">
            <el-tag :type="getReportTypeTag(scope.row.type)">
              {{ getReportTypeLabel(scope.row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="举报原因" />
        <el-table-column prop="reporter.username" label="举报人" width="120" />
        <el-table-column prop="createTime" label="举报时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button
              size="small"
              @click="viewDetail(scope.row)"
            >
              查看详情
            </el-button>
            <el-button
              v-if="status === 'pending'"
              size="small"
              type="primary"
              @click="handleReport(scope.row)"
            >
              处理
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

    <!-- 处理举报的对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="处理举报"
      width="500px"
    >
      <el-form :model="form" label-width="80px">
        <el-form-item label="处理结果">
          <el-radio-group v-model="form.result">
            <el-radio label="valid">举报有效</el-radio>
            <el-radio label="invalid">举报无效</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处理说明">
          <el-input
            v-model="form.comment"
            type="textarea"
            :rows="3"
            placeholder="请输入处理说明"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitHandle">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const loading = ref(false)
const reports = ref([])
const status = ref('pending')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const currentReport = ref(null)
const form = ref({
  result: 'valid',
  comment: ''
})

const formatDate = (date: string) => {
  return new Date(date).toLocaleString()
}

const getReportTypeLabel = (type: string) => {
  const types: Record<string, string> = {
    'post': '帖子',
    'comment': '评论',
    'user': '用户'
  }
  return types[type] || type
}

const getReportTypeTag = (type: string) => {
  const types: Record<string, string> = {
    'post': 'warning',
    'comment': 'info',
    'user': 'danger'
  }
  return types[type] || ''
}

const fetchReports = async () => {
  try {
    loading.value = true
    const response = await axios.get('/api/admin/reports', {
      params: {
        status: status.value,
        page: currentPage.value - 1,
        size: pageSize.value
      }
    })
    reports.value = response.data.content
    total.value = response.data.totalElements
  } catch (error) {
    ElMessage.error('获取举报列表失败')
  } finally {
    loading.value = false
  }
}

const handleStatusChange = () => {
  currentPage.value = 1
  fetchReports()
}

const handleSizeChange = (val: number) => {
  pageSize.value = val
  fetchReports()
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
  fetchReports()
}

const viewDetail = (report: any) => {
  const { type, targetId } = report
  if (type === 'post') {
    window.open(`/post/${targetId}`, '_blank')
  }
  // 其他类型的处理...
}

const handleReport = (report: any) => {
  currentReport.value = report
  dialogVisible.value = true
  form.value = {
    result: 'valid',
    comment: ''
  }
}

const submitHandle = async () => {
  if (!currentReport.value) return
  
  try {
    await axios.post(`/api/admin/reports/${currentReport.value.id}/process`, form.value)
    ElMessage.success('处理成功')
    dialogVisible.value = false
    await fetchReports()
  } catch (error) {
    ElMessage.error('处理失败')
  }
}

onMounted(() => {
  fetchReports()
})
</script>

<style scoped>
.reports-container {
  min-height: 100%;
}

.reports-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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