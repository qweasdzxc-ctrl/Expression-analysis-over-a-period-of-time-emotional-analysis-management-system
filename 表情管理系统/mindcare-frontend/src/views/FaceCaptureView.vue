<template>
  <div class="face-capture">
    <el-row :gutter="20">
      <el-col :span="14">
        <el-card class="camera-container">
          <video ref="videoRef" width="100%" autoplay muted></video>
          <canvas ref="canvasRef" style="display: none;"></canvas>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card>
          <template #header>
            <div class="card-header">
              <span class="control-title">表情捕捉控制面板</span>
            </div>
          </template>
          <el-form>
            <el-form-item label="捕捉间隔(秒)">
              <el-input-number v-model="captureInterval" :min="1" :max="60" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="startCapture" :disabled="isCapturing">
                开始捕捉
              </el-button>
              <el-button type="danger" @click="stopCapture" :disabled="!isCapturing">
                停止捕捉
              </el-button>
              <el-button 
                type="success" 
                @click="analyzeTimePeriod" 
                :disabled="!hasCaptures || isAnalyzing"
                :loading="isAnalyzing"
              >
                {{ isAnalyzing ? '分析中...' : '分析表情' }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
        
        <el-card v-if="workflowImageUrl" class="mt-4">
          <template #header>
            <div class="card-header">
              <span class="result-title">分析结果图片</span>
              <span class="image-tip">(点击图片可放大查看)</span>
            </div>
          </template>
          <div class="generated-image">
            <img 
              :src="workflowImageUrl" 
              alt="分析结果" 
              @error="handleWorkflowImageError"
              class="workflow-result-image"
              @click="showLargeImage"
              style="cursor: pointer;"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 添加图片放大对话框 -->
    <el-dialog
      v-model="showEnlargedImage"
      :title="null"
      width="90%"
      :destroy-on-close="true"
      :show-close="true"
      center
      class="image-dialog"
    >
      <div class="enlarged-image-container">
        <img 
          :src="workflowImageUrl" 
          alt="放大的分析结果"
          class="enlarged-image"
          @click="showEnlargedImage = false"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const videoRef = ref<HTMLVideoElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)
const captureInterval = ref(5)
const isCapturing = ref(false)
const analysisResult = ref<string | null>(null)
const showAnalysis = ref(false)
const trendData = ref<any[]>([])
let captureTimer: number | null = null

// 添加分析状态
const isAnalyzing = ref(false);

// 添加时间范围变量
const selectedStartTime = ref<string>('');
const selectedEndTime = ref<string>('');

// 添加捕获会话的开始时间
const sessionStartTime = ref<string>('');

// 添加工作流图片相关的响应式变量
const workflowImageUrl = ref('');

// 添加图片放大相关的变量
const showEnlargedImage = ref(false);

// 添加显示大图的方法
const showLargeImage = () => {
  showEnlargedImage.value = true;
};

// 添加图片压缩和格式化函数
const processImage = async (canvas: HTMLCanvasElement): Promise<string> => {
  return new Promise((resolve) => {
    // 压缩图片，控制质量和大小
    const maxWidth = 800;
    const maxHeight = 600;
    
    let width = canvas.width;
    let height = canvas.height;
    
    if (width > maxWidth) {
      height = Math.round((height * maxWidth) / width);
      width = maxWidth;
    }
    if (height > maxHeight) {
      width = Math.round((width * maxHeight) / height);
      height = maxHeight;
    }
    
    const tempCanvas = document.createElement('canvas');
    tempCanvas.width = width;
    tempCanvas.height = height;
    
    const ctx = tempCanvas.getContext('2d');
    ctx?.drawImage(canvas, 0, 0, width, height);
    
    // 转换为base64，控制图片质量
    resolve(tempCanvas.toDataURL('image/jpeg', 0.8));
  });
};

// 修改捕捉图像函数
const captureImage = async () => {
  if (!videoRef.value || !canvasRef.value) return;

  const context = canvasRef.value.getContext('2d');
  if (!context) return;

  try {
    // 设置canvas尺寸与视频一致
    canvasRef.value.width = videoRef.value.videoWidth;
    canvasRef.value.height = videoRef.value.videoHeight;

    // 在canvas上绘制当前视频帧
    context.drawImage(videoRef.value, 0, 0);

    // 获取图片数据
    const imageData = canvasRef.value.toDataURL('image/jpeg', 0.8);
    
    // 格式化时间戳
    const now = new Date();
    const timestamp = now.toISOString();
    
    console.log('Sending capture request with timestamp:', timestamp);
    
    // 发送到后端保存
    const response = await axios.post('http://localhost:8080/api/face-captures', {
      image: imageData,
      timestamp: timestamp
    }, {
      headers: {
        'Content-Type': 'application/json'
      }
    });
    
    console.log('Capture response:', response.data);
    // 保存捕获记录到历史
    if (response.data) {
      console.log('Capture saved successfully:', timestamp);
      ElMessage.success('捕捉成功');
      captureHistory.value.push({
        timestamp: timestamp,
        result: null
      });
    }
    
    return response.data;
  } catch (error) {
    console.error('Capture error:', error);
    ElMessage.error(`捕捉失败: ${error.response?.data || error.message}`);
    throw error;
  }
};

// 添加历史记录
const captureHistory = ref<Array<{
  timestamp: string;
  result: any;
}>>([]);

// 检查是否有捕捉历史
const hasCaptures = computed(() => captureHistory.value.length > 0);

// 格式化分析结果
const formattedAnalysisResult = computed(() => {
  if (!analysisResult.value) return '';
  // 确保 analysisResult.value 是字符串
  const result = String(analysisResult.value);
  return result.replace(/\n/g, '<br>');
});

// 初始化摄像头
const initCamera = async () => {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ video: true })
    if (videoRef.value) {
      videoRef.value.srcObject = stream
    }
  } catch (error) {
    ElMessage.error('无法访问摄像头')
    console.error('Camera error:', error)
  }
}

// 修改开始捕捉函数
const startCapture = () => {
  isCapturing.value = true;
  sessionStartTime.value = new Date().toISOString(); // 记录开始时间
  captureTimer = window.setInterval(() => {
    captureImage();
  }, captureInterval.value * 1000);
};

// 修改分析函数
const analyzeTimePeriod = async () => {
  if (isAnalyzing.value) return;
  
  try {
    isAnalyzing.value = true;
    ElMessage.info('正在分析表情数据...');
    
    if (!sessionStartTime.value) {
      ElMessage.warning('没有找到捕获会话的开始时间');
      return;
    }
    
    const response = await axios.post('http://localhost:8080/api/face-captures/analyze-period', {
      startTime: sessionStartTime.value,
      endTime: new Date().toISOString()
    });
    
    // 打印工作流数据
    console.log('============= 工作流数据 =============');
    if (response.data.workflowData) {
      console.log('类型:', response.data.workflowData.type);
      console.log('文件名:', response.data.workflowData.name);
      console.log('数据长度:', response.data.workflowData.data?.length || 0, '字符');
      if (response.data.workflowData.data) {
        const analysisData = JSON.parse(response.data.workflowData.data);
        console.log('数据预览:', analysisData);
      }
    }
    console.log('====================================');

    // 检查响应是否是字符串
    if (typeof response.data === 'string') {
      ElMessage.warning(response.data);
      return;
    }
    
    // 解析后端返回的分析数据
    const analysisData = JSON.parse(response.data.workflowData.data);
    
    // 打印图片链接到浏览器控制台
    console.log('============= 合并图片信息 =============');
    console.log(`图片数量: ${analysisData.imageCount}`);
    console.log(`时间范围: ${analysisData.startTime} 到 ${analysisData.endTime}`);
    console.log('阿里云 OSS 图片链接:');
    console.log(analysisData.imageUrl);
    console.log('分析结果:', analysisData.analysis);
    console.log('=======================================');

    // 设置工作流生成的图片URL并显示弹窗
    if (analysisData.analysis) {
      workflowImageUrl.value = analysisData.analysis;
      ElMessage.success('分析完成，已生成结果图片');
      // 自动滚动到图片位置
      setTimeout(() => {
        document.querySelector('.workflow-result-image')?.scrollIntoView({ 
          behavior: 'smooth',
          block: 'center'
        });
      }, 100);
    } else {
      ElMessage.warning('分析完成，但未生成图片');
    }

  } catch (error: any) {
    console.error('Analysis error:', error);
    const errorMessage = error.response?.data || error.message;
    ElMessage.error(`分析失败: ${errorMessage}`);
  } finally {
    isAnalyzing.value = false;
  }
};

// 停止捕捉时清除会话时间
const stopCapture = () => {
  if (captureTimer) {
    clearInterval(captureTimer);
    captureTimer = null;
  }
  isCapturing.value = false;
  // 不要清除 sessionStartTime，因为还需要用它来分析
};

const handleImageError = (error) => {
  console.error('图片加载失败:', error);
  // 显示更详细的错误信息
  ElMessage.error({
    message: '图片加载失败，请检查网络连接或重试',
    duration: 5000
  });
  console.log('尝试加载的图片URL:', workflowImageUrl.value);
};

// 添加工作流图片错误处理函数
const handleWorkflowImageError = (error) => {
  console.error('工作流图片加载失败:', error);
  ElMessage.error({
    message: '分析结果图片加载失败，请重试',
    duration: 5000
  });
};

onMounted(() => {
  initCamera()
})

onUnmounted(() => {
  stopCapture()
  // 关闭摄像头
  const stream = videoRef.value?.srcObject as MediaStream
  stream?.getTracks().forEach(track => track.stop())
})
</script>

<style scoped>
.face-capture {
  padding: 20px;
}

.camera-container {
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.control-title {
  font-size: 18px;
  font-weight: bold;
  color: #409EFF;
}

.result-title {
  font-size: 18px;
  font-weight: bold;
  color: #67C23A;
}

.mt-4 {
  margin-top: 1rem;
}

.generated-image {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 10px;
}

.generated-image img {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
}

.el-form-item {
  margin-bottom: 20px;
}

.el-button {
  margin-right: 10px;
}

.workflow-result-image {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
}

.image-tip {
  font-size: 14px;
  color: #909399;
  margin-left: 10px;
}

.enlarged-image-container {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 80vh;
  background-color: rgba(0, 0, 0, 0.7);
  overflow: auto;
}

.enlarged-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  cursor: zoom-out;
}

:deep(.image-dialog .el-dialog) {
  background: transparent;
  box-shadow: none;
}

:deep(.image-dialog .el-dialog__header) {
  padding: 0;
  margin: 0;
}

:deep(.image-dialog .el-dialog__body) {
  padding: 0;
  margin: 0;
}

:deep(.image-dialog .el-dialog__headerbtn) {
  font-size: 20px;
  top: 10px;
  right: 10px;
  z-index: 2;
}

:deep(.image-dialog .el-dialog__headerbtn .el-dialog__close) {
  color: #fff;
}
</style> 