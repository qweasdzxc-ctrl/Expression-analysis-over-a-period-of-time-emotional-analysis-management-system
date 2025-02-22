async function uploadImage(imageData) {
    try {
        const response = await fetch('/api/face-captures/upload', {
            method: 'POST',
            body: imageData
        });
        
        const result = await response.json();
        
        // 在浏览器控制台打印工作流数据
        console.log('发送到工作流的数据：');
        console.log('类型:', result.type);
        console.log('文件名:', result.name);
        console.log('数据长度:', result.data.length, '字符');
        console.log('数据预览:', result.data.substring(0, 100) + '...');
        
        return result;
    } catch (error) {
        console.error('上传失败:', error);
        throw error;
    }
} 