async function stopCapture() {
  try {
    isCapturing.value = false;
    if (captures.value.length > 0) {
      const response = await analyzeEmotions(captures.value);
      // 解析后端返回的 JSON 字符串
      const result = JSON.parse(response);
      
      // 打印图片链接到浏览器控制台
      console.log('============= 合并图片链接 =============');
      console.log(`图片数量: ${result.imageCount}`);
      console.log(`时间范围: ${result.startTime} 到 ${result.endTime}`);
      console.log('图片链接:');
      console.log(result.imageUrl);  // 直接打印完整的 URL
      console.log('分析结果:', result.analysis);
      console.log('=======================================');
      
      // 处理分析结果
      analysisResult.value = result.analysis;
      
      // 可选：在页面上显示合并后的图片
      // combinedImage.value = result.imageUrl;
    }
  } catch (error) {
    console.error('停止捕获失败:', error);
  }
} 